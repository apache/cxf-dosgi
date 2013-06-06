/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.dosgi.topologymanager.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.dosgi.topologymanager.util.SimpleServiceTracker;
import org.apache.cxf.dosgi.topologymanager.util.SimpleServiceTrackerListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for remote endpoints using the EndpointListener interface and the EndpointListenerManager.
 * Listens for local service interests using the ListenerHookImpl that calls back through the
 * ServiceInterestListener interface.
 * Manages local creation and destruction of service imports using the available RemoteServiceAdmin services.
 */
public class TopologyManagerImport implements EndpointListener, RemoteServiceAdminListener, ServiceInterestListener {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyManagerImport.class);
    private ExecutorService execService;

    private final EndpointListenerManager endpointListenerManager;
    private final BundleContext bctx;
    private final SimpleServiceTracker<RemoteServiceAdmin> remoteServiceAdminTracker;
    private final ListenerHookImpl listenerHook;

    /**
     * If set to false only one service is imported for each import interest even it multiple services are
     * available. If set to true, all available services are imported.
     *
     * TODO: Make this available as a configuration option
     */
    private boolean importAllAvailable = true;

    /**
     * Contains an instance of the Class Import Interest for each distinct import request. If the same filter
     * is requested multiple times the existing instance of the Object increments an internal reference
     * counter. If an interest is removed, the related ServiceInterest object is used to reduce the reference
     * counter until it reaches zero. in this case the interest is removed.
     */
    private final ReferenceCounter<String> importInterestsCounter = new ReferenceCounter<String>();

    /**
     * List of Endpoints by matched filter that were reported by the EndpointListener and can be imported
     */
    private final Map<String /* filter */, List<EndpointDescription>> importPossibilities
        = new HashMap<String, List<EndpointDescription>>();

    /**
     * List of already imported Endpoints by their matched filter
     */
    private final Map<String /* filter */, List<ImportRegistration>> importedServices
        = new HashMap<String, List<ImportRegistration>>();

    public TopologyManagerImport(BundleContext bc, SimpleServiceTracker<RemoteServiceAdmin> rsaTracker) {
        bctx = bc;
        remoteServiceAdminTracker = rsaTracker;
        remoteServiceAdminTracker.addListener(new SimpleServiceTrackerListener<RemoteServiceAdmin>() {

            public void added(RemoteServiceAdmin rsa) {
                triggerImportsForRemoteServiceAdmin(rsa);
            }

            public void removed(RemoteServiceAdmin rsa) {
            }
        });
        endpointListenerManager = new EndpointListenerManager(bctx, this);
        execService = new ThreadPoolExecutor(5, 10, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        listenerHook = new ListenerHookImpl(bc, this);
    }

    public void start() {
        bctx.registerService(RemoteServiceAdminListener.class.getName(), this, null);
        bctx.registerService(ListenerHook.class.getName(), listenerHook, null);
        endpointListenerManager.start();
    }

    public void stop() {
        endpointListenerManager.stop();
        execService.shutdown();
        // this is called from Activator.stop(), which implicitly unregisters our registered services
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.dosgi.topologymanager.ServiceInterestListener#addServiceInterest(java.lang.String)
     */
    public void addServiceInterest(String filter) {
        if (importInterestsCounter.add(filter) == 1) {
            endpointListenerManager.extendScope(filter);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.dosgi.topologymanager.ServiceInterestListener#removeServiceInterest(java.lang.String)
     */
    public void removeServiceInterest(String filter) {
        if (importInterestsCounter.remove(filter) == 0) {
            LOG.debug("last reference to import interest is gone -> removing interest filter: {}", filter);
            endpointListenerManager.reduceScope(filter);
            synchronized (importedServices) {
                List<ImportRegistration> irs = importedServices.remove(filter);
                if (irs != null) {
                    for (ImportRegistration ir : irs) {
                        ir.close();
                    }
                }
            }
        }
    }

    public void endpointAdded(EndpointDescription epd, String filter) {
        if (filter == null) {
            LOG.error("Endpoint is not handled because no matching filter was provided!");
            return;
        }
        LOG.debug("importable service added for filter {}, endpoint {}", filter, epd);
        addImportPossibility(epd, filter);
        triggerImport(filter);
    }

    public void endpointRemoved(EndpointDescription epd, String filter) {
        LOG.debug("EndpointRemoved {}", epd);
        removeImportPossibility(epd, filter);
        triggerImport(filter);
    }

    private void addImportPossibility(EndpointDescription epd, String filter) {
        synchronized (importPossibilities) {
            List<EndpointDescription> ips = importPossibilities.get(filter);
            if (ips == null) {
                ips = new ArrayList<EndpointDescription>();
                importPossibilities.put(filter, ips);
            }
            // prevent adding the same endpoint multiple times, which can happen sometimes,
            // and which causes imports to remain available even when services are actually down
            if (!ips.contains(epd)) {
                ips.add(epd);
            }
        }
    }

    private void removeImportPossibility(EndpointDescription epd, String filter) {
        synchronized (importPossibilities) {
            List<EndpointDescription> ips = importPossibilities.get(filter);
            if (ips != null) {
                ips.remove(epd);
                if (ips.isEmpty()) {
                    importPossibilities.remove(filter);
                }
            }
        }
    }

    public void triggerImportsForRemoteServiceAdmin(RemoteServiceAdmin rsa) {
        LOG.debug("New RemoteServiceAdmin {} detected, trying to import services with it", rsa);
        synchronized (importPossibilities) {
            for (String filter : importPossibilities.keySet()) {
                triggerImport(filter);
            }
        }
    }

    private void triggerImport(final String filter) {
        LOG.debug("Import of a service for filter {} was queued", filter);

        execService.execute(new Runnable() {
            public void run() {
                try {
                    unexportNotAvailableServices(filter);
                    importServices(filter);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
                // Notify EndpointListeners? NO!
            }
        });
    }

    private void unexportNotAvailableServices(String filter) {
        synchronized (importedServices) {
            List<ImportRegistration> importRegistrations = importedServices.get(filter);
            if (importRegistrations == null) {
                return;
            }

            Iterator<ImportRegistration> it = importRegistrations.iterator();
            while (it.hasNext()) {
                ImportRegistration ir = it.next();
                EndpointDescription ep = ir.getImportReference().getImportedEndpoint();
                if (!isImportPossibilityAvailable(ep, filter)) {
                    // unexport service
                    ir.close();
                    it.remove();
                }
            }

            if (importRegistrations.isEmpty()) {
                importedServices.remove(filter);
            }
        }
    }

    private boolean isImportPossibilityAvailable(EndpointDescription ep, String filter) {
        synchronized (importPossibilities) {
            List<EndpointDescription> ips = importPossibilities.get(filter);
            return ips != null && ips.contains(ep);
        }
    }

    // return a copy to prevent sync issues
    private List<EndpointDescription> getImportPossibilitiesCopy(String filter) {
        synchronized (importPossibilities) {
            List<EndpointDescription> possibilities = importPossibilities.get(filter);
            return possibilities == null
                ? Collections.<EndpointDescription>emptyList()
                : new ArrayList<EndpointDescription>(possibilities);
        }
    }

    private void importServices(String filter) {
        synchronized (importedServices) {
            List<ImportRegistration> importRegistrations = importedServices.get(filter);
            for (EndpointDescription epd : getImportPossibilitiesCopy(filter)) {
                // TODO but optional: if the service is already imported and the endpoint is still
                // in the list of possible imports check if a "better" endpoint is now in the list
                if (!alreadyImported(epd, importRegistrations)) {
                    // service not imported yet -> import it now
                    ImportRegistration ir = importService(epd);
                    if (ir != null) {
                        // import was successful
                        if (importRegistrations == null) {
                            importRegistrations = new ArrayList<ImportRegistration>();
                            importedServices.put(filter, importRegistrations);
                        }
                        importRegistrations.add(ir);
                        if (!importAllAvailable) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean alreadyImported(EndpointDescription epd, List<ImportRegistration> importRegistrations) {
        if (importRegistrations != null) {
            for (ImportRegistration ir : importRegistrations) {
                if (epd.equals(ir.getImportReference().getImportedEndpoint())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tries to import the service with each rsa until one import is successful
     *
     * @param ep endpoint to import
     * @return import registration of the first successful import
     */
    private ImportRegistration importService(EndpointDescription ep) {
        for (RemoteServiceAdmin rsa : remoteServiceAdminTracker.getAllServices()) {
            ImportRegistration ir = rsa.importService(ep);
            if (ir != null && ir.getException() == null) {
                LOG.debug("Service import was successful {}", ir);
                return ir;
            }
        }
        return null;
    }

    /**
     * This method is called once a RemoteServiceAdminEvent for an removed import reference is received.
     * However the current implementation has no special support for multiple topology managers, therefore this method
     * does nothing for the moment.
     */
    public void removeImportReference(ImportReference anyObject) {
    }

    public void remoteAdminEvent(RemoteServiceAdminEvent event) {
        if (event.getType() == RemoteServiceAdminEvent.IMPORT_UNREGISTRATION) {
            removeImportReference(event.getImportReference());
        }
    }
}
