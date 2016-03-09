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
import java.util.Collection;
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
import org.osgi.framework.ServiceReference;
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

            public void added(ServiceReference<RemoteServiceAdmin> reference, RemoteServiceAdmin rsa) {
                triggerImportsForRemoteServiceAdmin(rsa);
            }

            public void modified(ServiceReference<RemoteServiceAdmin> reference, RemoteServiceAdmin rsa) {
            }

            public void removed(ServiceReference<RemoteServiceAdmin> reference, RemoteServiceAdmin rsa) {
                // the RSA's imports will be closed by its shutdown, so nothing to do here
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

    public void endpointAdded(EndpointDescription endpoint, String filter) {
        if (filter == null) {
            LOG.error("Endpoint is not handled because no matching filter was provided!");
            return;
        }
        LOG.debug("importable service added for filter {}, endpoint {}", filter, endpoint);
        addImportPossibility(endpoint, filter);
        triggerImport(filter);
    }

    public void endpointRemoved(EndpointDescription endpoint, String filter) {
        LOG.debug("EndpointRemoved {}", endpoint);
        removeImportPossibility(endpoint, filter);
        triggerImport(filter);
    }

    private void addImportPossibility(EndpointDescription endpoint, String filter) {
        synchronized (importPossibilities) {
            List<EndpointDescription> endpoints = importPossibilities.get(filter);
            if (endpoints == null) {
                endpoints = new ArrayList<EndpointDescription>();
                importPossibilities.put(filter, endpoints);
            }
            // prevent adding the same endpoint multiple times, which can happen sometimes,
            // and which causes imports to remain available even when services are actually down
            if (!endpoints.contains(endpoint)) {
                endpoints.add(endpoint);
            }
        }
    }

    private void removeImportPossibility(EndpointDescription endpoint, String filter) {
        synchronized (importPossibilities) {
            List<EndpointDescription> endpoints = importPossibilities.get(filter);
            if (endpoints != null) {
                endpoints.remove(endpoint);
                if (endpoints.isEmpty()) {
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
            if (importRegistrations != null) {
                // iterate over a copy
                for (ImportRegistration ir : new ArrayList<ImportRegistration>(importRegistrations)) {
                    EndpointDescription endpoint = ir.getImportReference().getImportedEndpoint();
                    if (!isImportPossibilityAvailable(endpoint, filter)) {
                        removeImport(ir, null); // also unexports the service
                    }
                }
            }
        }
    }

    private boolean isImportPossibilityAvailable(EndpointDescription endpoint, String filter) {
        synchronized (importPossibilities) {
            List<EndpointDescription> endpoints = importPossibilities.get(filter);
            return endpoints != null && endpoints.contains(endpoint);
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
            for (EndpointDescription endpoint : getImportPossibilitiesCopy(filter)) {
                // TODO but optional: if the service is already imported and the endpoint is still
                // in the list of possible imports check if a "better" endpoint is now in the list
                if (!alreadyImported(endpoint, importRegistrations)) {
                    // service not imported yet -> import it now
                    ImportRegistration ir = importService(endpoint);
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

    private boolean alreadyImported(EndpointDescription endpoint, List<ImportRegistration> importRegistrations) {
        if (importRegistrations != null) {
            for (ImportRegistration ir : importRegistrations) {
                if (endpoint.equals(ir.getImportReference().getImportedEndpoint())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tries to import the service with each rsa until one import is successful
     *
     * @param endpoint endpoint to import
     * @return import registration of the first successful import
     */
    private ImportRegistration importService(EndpointDescription endpoint) {
        for (RemoteServiceAdmin rsa : remoteServiceAdminTracker.getAllServices()) {
            ImportRegistration ir = rsa.importService(endpoint);
            if (ir != null) {
                if (ir.getException() == null) {
                    LOG.debug("Service import was successful {}", ir);
                    return ir;
                } else {
                    LOG.info("Error importing service " + endpoint, ir.getException());
                }
            }
        }
        return null;
    }

    /**
     * Remove and close (unexport) the given import. The import is specified either
     * by its ImportRegistration or by its ImportReference (only one of them must
     * be specified).
     * <p>
     * If this method is called from within iterations on the underlying data structure,
     * the iterations must be made on copies of the structures rather than the original
     * references in order to prevent ConcurrentModificationExceptions.
     *
     * @param reg the import registration to remove
     * @param ref the import reference to remove
     */
    private void removeImport(ImportRegistration reg, ImportReference ref) {
        // this method may be called recursively by calling ImportRegistration.close()
        // and receiving a RemoteServiceAdminEvent for its unregistration, which results
        // in a ConcurrentModificationException. We avoid this by closing the registrations
        // only after data structure manipulation is done, and being re-entrant.
        synchronized (importedServices) {
            List<ImportRegistration> removed = new ArrayList<ImportRegistration>();
            for (Iterator<List<ImportRegistration>> it1 = importedServices.values().iterator(); it1.hasNext();) {
                Collection<ImportRegistration> irs = it1.next();
                for (Iterator<ImportRegistration> it2 = irs.iterator(); it2.hasNext();) {
                    ImportRegistration ir = it2.next();
                    if (ir.equals(reg) || ir.getImportReference().equals(ref)) {
                        removed.add(ir);
                        it2.remove();
                    }
                }
                if (irs.isEmpty()) {
                    it1.remove();
                }
            }
            for (ImportRegistration ir : removed) {
                ir.close();
            }
        }
    }

    public void remoteAdminEvent(RemoteServiceAdminEvent event) {
        if (event.getType() == RemoteServiceAdminEvent.IMPORT_UNREGISTRATION) {
            removeImport(null, event.getImportReference());
        }
    }
}
