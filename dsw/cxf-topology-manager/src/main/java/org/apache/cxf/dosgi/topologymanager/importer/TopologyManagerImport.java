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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.dosgi.topologymanager.rsatracker.RemoteServiceAdminLifeCycleListener;
import org.apache.cxf.dosgi.topologymanager.rsatracker.RemoteServiceAdminTracker;
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
    private final RemoteServiceAdminTracker remoteServiceAdminTracker;
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
    private final RefManager importInterests = new RefManager();

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

    public TopologyManagerImport(BundleContext bc, RemoteServiceAdminTracker rsaTracker) {
        bctx = bc;
        this.remoteServiceAdminTracker = rsaTracker;
        this.remoteServiceAdminTracker.addListener(new RemoteServiceAdminLifeCycleListener() {
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
        execService.shutdown();
    }
    
    /* (non-Javadoc)
     * @see org.apache.cxf.dosgi.topologymanager.ServiceInterestListener#addServiceInterest(java.lang.String)
     */
    public void addServiceInterest(String filter) {
        if (importInterests.addReference(filter) == 1) {
            endpointListenerManager.extendScope(filter);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.dosgi.topologymanager.ServiceInterestListener#removeServiceInterest(java.lang.String)
     */
    public void removeServiceInterest(String filter) {
        if (importInterests.removeReference(filter) <= 0) {
            LOG.debug("last reference to import interest is gone -> removing interest  filter: {}", filter);
            endpointListenerManager.reduceScope(filter);
            List<ImportRegistration> irs = importedServices.remove(filter);
            if (irs != null) {
                for (ImportRegistration ir : irs) {
                    if (ir != null) {
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

            ips.add(epd);
        }
    }

    private void removeImportPossibility(EndpointDescription epd, String filter) {
        synchronized (importPossibilities) {
            List<EndpointDescription> ips = importPossibilities.get(filter);
            if (ips != null) {
                ips.remove(epd);
            } else {
                // should not happen
            }
        }
    }

    public void triggerImportsForRemoteServiceAdmin(RemoteServiceAdmin rsa) {
        LOG.debug("New RSA detected trying to import services with it");
        synchronized (importPossibilities) {
            Set<Map.Entry<String, List<EndpointDescription>>> entries = importPossibilities.entrySet();
            for (Entry<String, List<EndpointDescription>> entry : entries) {
                triggerImport(entry.getKey());
            }
        }
    }

    private void triggerImport(final String filter) {
        LOG.debug("Import of a service for filter {} was queued", filter);

        execService.execute(new Runnable() {
            public void run() {
                try {
                    synchronized (importedServices) { // deadlock possibility ?
                        synchronized (importPossibilities) {
                            unexportNotAvailableServices(filter);
                            importServices(filter);
                        }
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
                // Notify EndpointListeners ? NO!
            }

        });

    }
    
    private void unexportNotAvailableServices(String filter) {
        List<ImportRegistration> importRegistrations = getImportedServices(filter);
        if (importRegistrations.size() == 0) {
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
    }
    
    private boolean isImportPossibilityAvailable(EndpointDescription ep, String filter) {
        List<EndpointDescription> ips = importPossibilities.get(filter);
        return ips != null && ips.contains(ep);
    }

    /**
     * TODO but optional: if the service is already imported and the endpoint is still
     * in the list of possible imports check if a "better" endpoint is now in the list ?
     * 
     * @param filter
     */
    private void importServices(String filter) {        
        List<ImportRegistration> importRegistrations = getImportedServices(filter);
        List<EndpointDescription> possibilities = importPossibilities.get(filter);
        if (possibilities == null) {
            return;
        }
        for (EndpointDescription epd : possibilities) {
            if (!alreadyImported(epd, importRegistrations)) {
                // service not imported yet -> import it now
                ImportRegistration ir = importService(epd);
                if (ir != null) {
                    // import was successful
                    importRegistrations.add(ir);
                    if (!importAllAvailable) {
                        return;
                    }
                }
            }
        }
    }

    private boolean alreadyImported(EndpointDescription epd, List<ImportRegistration> importRegistrations) {
        for (ImportRegistration ir : importRegistrations) {
            if (epd.equals(ir.getImportReference().getImportedEndpoint())) {
                return true;
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
        for (RemoteServiceAdmin rsa : remoteServiceAdminTracker.getList()) {
            ImportRegistration ir = rsa.importService(ep);
            if (ir != null && ir.getException() == null) {
                LOG.debug("Service import was successful {}", ir);
                return ir;
            }
        }
        return null;
    }

    /**
     * Returns the list of already imported services for the given filter
     *  
     * @param filter
     * @return import registrations for filter (will never return null)
     */
    private List<ImportRegistration> getImportedServices(String filter) {
        List<ImportRegistration> irs = importedServices.get(filter);
        if (irs == null) {
            irs = new ArrayList<ImportRegistration>();
            importedServices.put(filter, irs);
        }
        return irs;
    }

    /**
     * This method is called once a RemoteServiceAdminEvent for an removed import reference is received.
     * However the current implementation has no special support for multiple topology managers, therefore this method
     * does nothing for the moment.
     */
    public void removeImportReference(ImportReference anyObject) {
        //LOG.severe("NOT IMPLEMENTED !!!");
    }

    public void remoteAdminEvent(RemoteServiceAdminEvent event) {
        if (event.getType() == RemoteServiceAdminEvent.IMPORT_UNREGISTRATION) {
            removeImportReference(event.getImportReference());
        }
    }

}
