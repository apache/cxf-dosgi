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
package org.apache.cxf.dosgi.topologymanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;

/**
 * <li>This class keeps a list of currently imported and exported endpoints <li>It requests the import/export
 * from RemoteAdminServices
 */
public class TopologyManager implements ExportRepository {

    private final static Logger LOG = Logger.getLogger(TopologyManager.class.getName());

    private final BundleContext bctx;
    private final EndpointListenerNotifier epListenerNotifier;
    private final ExecutorService execService;
    private final RemoteServiceAdminTracker remoteServiceAdminTracker;
    private final ServiceListener serviceListerner;

    /**
     * Holds all services that are exported by this TopologyManager for each ServiceReference that should be
     * exported a map is maintained which contains information on the endpoints for each RemoteAdminService
     * 
     * <pre>
     * Bsp.:
     * ServiceToExort_1
     * ---&gt; RemoteAdminService_1 (CXF HTTP)
     * --------&gt; List&lt;EndpointDescription&gt; -&gt; {http://localhost:1234/greeter, http://localhost:8080/hello}
     * ---&gt; RemoteAdminService_2 (ActiveMQ JMS/OpenWire)
     * --------&gt; List&lt;EndpointDescription&gt; -&gt; {OpenWire://127.0.0.1:123/testQueue}
     * ServiceToExort_2
     * ---&gt; RemoteAdminService_1 (CXF HTTP)
     * --------&gt; List&lt;EndpointDescription&gt; -&gt; {empty} // not exported yet or not suitable
     * 
     * </pre>
     */
    private final Map<ServiceReference, 
                      Map<RemoteServiceAdmin, Collection<ExportRegistration>>> exportedServices = 
        new LinkedHashMap<ServiceReference, Map<RemoteServiceAdmin, Collection<ExportRegistration>>>();

    public TopologyManager(BundleContext ctx, RemoteServiceAdminTracker rsaTracker) {
        execService = new ThreadPoolExecutor(5, 10, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        bctx = ctx;
        this.remoteServiceAdminTracker = rsaTracker;
        this.remoteServiceAdminTracker.addListener(new RemoteServiceAdminLifeCycleListener() {

            public void added(RemoteServiceAdmin rsa) {
                triggerExportForRemoteServiceAdmin(rsa);
            }

            public void removed(RemoteServiceAdmin rsa) {
                // TODO: remove service exports from management structure and notify
                // discovery stuff...
                removeRemoteServiceAdmin(rsa);
            }
        });
        serviceListerner = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                LOG.fine("Received ServiceEvent: " + event);
                ServiceReference sref = event.getServiceReference();
                if (event.getType() == ServiceEvent.REGISTERED) {
                    LOG.fine("Registered");
                    if (shouldExportService(sref)) {
                        exportService(sref);
                    }
                } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                    removeService(sref);
                }
            }
        };
        
        epListenerNotifier = new EndpointListenerNotifier(ctx, this);
    }
    
    /**
     * checks if a Service is intended to be exported
     */
    private boolean shouldExportService(ServiceReference sref) {
        if (sref.getProperty(RemoteConstants.SERVICE_EXPORTED_INTERFACES) != null) {
            return true;
        }
        return false;
    }

    protected void removeRemoteServiceAdmin(RemoteServiceAdmin rsa) {
        synchronized (exportedServices) {
            for (Map.Entry<ServiceReference, Map<RemoteServiceAdmin, Collection<ExportRegistration>>> exports : exportedServices
                .entrySet()) {
                if (exports.getValue().containsKey(rsa)) {
                    // service was handled by this RemoteServiceAdmin
                    Collection<ExportRegistration> endpoints = exports.getValue().get(rsa);
                    // TODO for each notify discovery......

                    this.epListenerNotifier.notifyAllListenersOfRemoval(endpoints);

                    // remove all management information for the RemoteServiceAdmin
                    exports.getValue().remove(rsa);
                }
            }
        }
    }

    protected void triggerExportForRemoteServiceAdmin(RemoteServiceAdmin rsa) {
        LOG.finer("TopologyManager: triggerExportImportForRemoteSericeAdmin()");

        synchronized (exportedServices) {
            for (ServiceReference serviceRef : exportedServices.keySet()) {
                Map<RemoteServiceAdmin, Collection<ExportRegistration>> rsaExports = exportedServices.get(serviceRef);
                String bundleName = serviceRef.getBundle().getSymbolicName();
                if (rsaExports.containsKey(rsa)) {
                    // already handled....
                    LOG.fine("TopologyManager: service from bundle " + bundleName + "is already handled by this RSA");
                } else {
                    // trigger export of this service....
                    LOG.fine("TopologyManager: service from bundle " + bundleName + " is to be exported by this RSA");
                    exportService(serviceRef);
                }
            }
        }

    }

    public void start() {
        epListenerNotifier.start();
        bctx.addServiceListener(serviceListerner);
        remoteServiceAdminTracker.open();
        try {
            exportExistingServices();
        } catch (InvalidSyntaxException e) {
            LOG.log(Level.FINER, "Failed to check existing services.", e);
        }
    }

    public void stop() {
        execService.shutdown();
        remoteServiceAdminTracker.close();
        bctx.removeServiceListener(serviceListerner);
        epListenerNotifier.stop();
    }

    void removeService(ServiceReference sref) {
        synchronized (exportedServices) {
            if (exportedServices.containsKey(sref)) {
                Map<RemoteServiceAdmin, Collection<ExportRegistration>> rsas = exportedServices.get(sref);
                for (Map.Entry<RemoteServiceAdmin, Collection<ExportRegistration>> entry : rsas.entrySet()) {
                    if (entry.getValue() != null) {
                    	Collection<ExportRegistration> registrations = entry.getValue();
                    	this.epListenerNotifier.notifyListenersOfRemoval(registrations);
                        for (ExportRegistration exReg : registrations) {
                            if (exReg != null) {
                                 exReg.close();
                            }
                        }
                    }
                }

                exportedServices.remove(sref);
            }
        }
    }
    
    protected void exportService(ServiceReference sref) {
        // add to local list of services that should/are be exported
        synchronized (exportedServices) {
            LOG.info("TopologyManager: adding service to exportedServices list to export it --- from bundle:  "
                      + sref.getBundle().getSymbolicName());
            exportedServices.put(sref,
                                 new LinkedHashMap<RemoteServiceAdmin, Collection<ExportRegistration>>());
        }
        triggerExport(sref);
    }

    private void triggerExport(final ServiceReference sref) {
        execService.execute(new Runnable() {
            public void run() {
                doExportService(sref);
            }
        });
    }

    private void doExportService(final ServiceReference sref) {
        LOG.finer("TopologyManager: exporting service ...");

        Map<RemoteServiceAdmin, Collection<ExportRegistration>> exports = null;

        synchronized (exportedServices) {
            exports = Collections.synchronizedMap(exportedServices.get(sref));
        }
        // FIXME: Not thread safe...?
        if (exports == null) {
            return;
        }
        if (remoteServiceAdminTracker == null || remoteServiceAdminTracker.size() == 0) {
            LOG.log(Level.SEVERE,
                    "No RemoteServiceAdmin available! Unable to export service from bundle {0}, interfaces: {1}",
                    new Object[] {
                            sref.getBundle().getSymbolicName(),
                            sref.getProperty(org.osgi.framework.Constants.OBJECTCLASS) });
        }


        for (final RemoteServiceAdmin remoteServiceAdmin : remoteServiceAdminTracker
                .getList()) {
            LOG.info("TopologyManager: handling remoteServiceAdmin "
                    + remoteServiceAdmin);

            if (exports.containsKey(remoteServiceAdmin)) {
                // already handled by this remoteServiceAdmin
                LOG.fine("TopologyManager: already handled by this remoteServiceAdmin -> skipping");
            } else {
                // TODO: additional parameter Map ?
                LOG.fine("TopologyManager: exporting ...");
                Collection<ExportRegistration> endpoints = remoteServiceAdmin
                        .exportService(sref, null);
                if (endpoints == null) {
                    // TODO export failed -> What should be done here?
                    LOG.severe("TopologyManager: export failed");
                    exports.put(remoteServiceAdmin, null);
                } else {
                    LOG.info("TopologyManager: export sucessful Endpoints:"
                            + endpoints);
                    // enqueue in local list of endpoints
                    exports.put(remoteServiceAdmin, endpoints);

                    epListenerNotifier
                            .nofifyEndpointListenersOfAdding(endpoints);
                }
            }
        }
    }
    
    public Collection<ExportRegistration> getAllExportRegistrations() {
        List<ExportRegistration> registrations = new ArrayList<ExportRegistration>();
        synchronized (exportedServices) {
            for (Map<RemoteServiceAdmin, Collection<ExportRegistration>> exports : exportedServices.values()) {
                for (Collection<ExportRegistration> regs : exports.values()) {
                    if (regs != null) {
                        registrations.addAll(regs);
                    }
                }
            }
        }
        return registrations;
    }

    private void exportExistingServices() throws InvalidSyntaxException {
        String filter = "(" + RemoteConstants.SERVICE_EXPORTED_INTERFACES + "=*)";
        ServiceReference[] references = bctx.getServiceReferences(null, filter);

        if (references != null) {
            for (ServiceReference sref : references) {
                exportService(sref);
            }
        }
    }

    public void removeExportRegistration(ExportRegistration exportRegistration) {
        ServiceReference sref = exportRegistration.getExportReference().getExportedService();
        if (sref != null) {
            synchronized (exportedServices) {

                Map<RemoteServiceAdmin, Collection<ExportRegistration>> ex = exportedServices.get(sref);
                if (ex != null) {
                    for (Map.Entry<RemoteServiceAdmin, Collection<ExportRegistration>> export : ex.entrySet()) {
                        export.getValue().contains(exportRegistration);
                    }
                }
            }
        } else {
            // the manager will be notified by its own service listener about this case
        }
    }

    /**
     * This method is called once a RemoteServiceAdminEvent for an removed export reference is received.
     * However the current implementation has no special support for multiple topology managers, therefore this method
     * does nothing for the moment.
     */
    public void removeExportReference(ExportReference anyObject) {
        // TODO Auto-generated method stub
        // LOG.severe("NOT implemented !!!");
    }

    public void remoteAdminEvent(RemoteServiceAdminEvent event) {
        if (event.getType() == RemoteServiceAdminEvent.EXPORT_UNREGISTRATION) {
            removeExportReference(event.getExportReference());
        }
    }

}
