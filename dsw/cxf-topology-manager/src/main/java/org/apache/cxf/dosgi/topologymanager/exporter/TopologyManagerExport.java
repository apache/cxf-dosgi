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
package org.apache.cxf.dosgi.topologymanager.exporter;

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

import org.apache.cxf.dosgi.topologymanager.rsatracker.RemoteServiceAdminLifeCycleListener;
import org.apache.cxf.dosgi.topologymanager.rsatracker.RemoteServiceAdminTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages exported endpoints of DOSGi services and notifies EndpointListeners of changes.
 * 
 * <li> Tracks local RemoteServiceAdmin instances by using a ServiceTracker
 * <li> Uses a ServiceListener to track local OSGi services
 * <li> When a service is published that is supported by DOSGi the
 *      known RemoteServiceAdmins are instructed to export the service and
 *      the EndpointListeners are notified
 * <li> When a service is unpublished the EndpointListeners are notified.
 *      The endpoints are not closed as the ExportRegistration takes care of this  
 */
public class TopologyManagerExport implements EndpointRepository {

    private static final String DOSGI_SERVICES = "(" + RemoteConstants.SERVICE_EXPORTED_INTERFACES + "=*)";

	private static final Logger LOG = LoggerFactory.getLogger(TopologyManagerExport.class);

    private final BundleContext bctx;
    private final EndpointListenerNotifier epListenerNotifier;
    private final ExecutorService execService;
    private final RemoteServiceAdminTracker remoteServiceAdminTracker;
    private final ServiceListener serviceListener;

    /**
     * Holds all services that are exported by this TopologyManager for each ServiceReference that should be
     * exported a map is maintained which contains information on the endpoints for each RemoteAdminService
     */
    private final Map<ServiceReference, 
                      Map<RemoteServiceAdmin, Collection<EndpointDescription>>> exportedServices = 
        new LinkedHashMap<ServiceReference, Map<RemoteServiceAdmin, Collection<EndpointDescription>>>();

    public TopologyManagerExport(BundleContext ctx, RemoteServiceAdminTracker rsaTracker) {
        execService = new ThreadPoolExecutor(5, 10, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        bctx = ctx;
        this.remoteServiceAdminTracker = rsaTracker;
        this.remoteServiceAdminTracker.addListener(new RemoteServiceAdminLifeCycleListener() {

            public void added(RemoteServiceAdmin rsa) {
                triggerExportForRemoteServiceAdmin(rsa);
            }

            public void removed(RemoteServiceAdmin rsa) {
                removeRemoteServiceAdmin(rsa);
            }
        });
        serviceListener = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                ServiceReference sref = event.getServiceReference();
                if (event.getType() == ServiceEvent.REGISTERED) {
                    LOG.debug("Received REGISTERED ServiceEvent: {}", event);
                    if (shouldExportService(sref)) {
                        exportService(sref);
                    }
                } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                    LOG.debug("Received UNREGISTERING ServiceEvent: {}", event);
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
        return sref.getProperty(RemoteConstants.SERVICE_EXPORTED_INTERFACES) != null;
    }

    /**
     * Remove all services exported by the given rsa and notify listeners
     * @param rsa
     */
    protected void removeRemoteServiceAdmin(RemoteServiceAdmin rsa) {
        synchronized (exportedServices) {
            for (Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports : exportedServices
                .values()) {
                if (exports.containsKey(rsa)) {
                    Collection<EndpointDescription> endpoints = exports.get(rsa);
                    this.epListenerNotifier.notifyAllListenersOfRemoval(endpoints);
                    exports.remove(rsa);
                }
            }
        }
    }

    protected void triggerExportForRemoteServiceAdmin(RemoteServiceAdmin rsa) {
        LOG.debug("triggerExportImportForRemoteSericeAdmin()");

        synchronized (exportedServices) {
            for (ServiceReference serviceRef : exportedServices.keySet()) {
                Map<RemoteServiceAdmin, Collection<EndpointDescription>> rsaExports = exportedServices.get(serviceRef);
                String bundleName = serviceRef.getBundle().getSymbolicName();
                if (rsaExports.containsKey(rsa)) {
                    // already handled....
                    LOG.debug("service from bundle {} is already handled by this RSA", bundleName);
                } else {
                    // trigger export of this service....
                    LOG.debug("service from bundle {} is to be exported by this RSA", bundleName);
                    exportService(serviceRef);
                }
            }
        }

    }

    public void start() {
        epListenerNotifier.start();
        bctx.addServiceListener(serviceListener);
        remoteServiceAdminTracker.open();
        exportExistingServices();
    }

    public void stop() {
        execService.shutdown();
        remoteServiceAdminTracker.close();
        bctx.removeServiceListener(serviceListener);
        epListenerNotifier.stop();
    }

    void removeService(ServiceReference sref) {
        synchronized (exportedServices) {
            if (exportedServices.containsKey(sref)) {
                Map<RemoteServiceAdmin, Collection<EndpointDescription>> rsas = exportedServices.get(sref);
                for (Map.Entry<RemoteServiceAdmin, Collection<EndpointDescription>> entry : rsas.entrySet()) {
                    if (entry.getValue() != null) {
                        Collection<EndpointDescription> registrations = entry.getValue();
                        this.epListenerNotifier.notifyListenersOfRemoval(registrations);
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
                                 new LinkedHashMap<RemoteServiceAdmin, Collection<EndpointDescription>>());
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
        LOG.debug("Exporting service");

        Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports = null;

        synchronized (exportedServices) {
            exports = Collections.synchronizedMap(exportedServices.get(sref));
        }
        // FIXME: Not thread safe...?
        if (exports == null) {
            return;
        }
        if (remoteServiceAdminTracker == null || remoteServiceAdminTracker.size() == 0) {
            LOG.error(
                    "No RemoteServiceAdmin available! Unable to export service from bundle {}, interfaces: {}",
                    sref.getBundle().getSymbolicName(),
                    sref.getProperty(org.osgi.framework.Constants.OBJECTCLASS));
        }


        for (final RemoteServiceAdmin remoteServiceAdmin : remoteServiceAdminTracker
                .getList()) {
            LOG.info("TopologyManager: handling remoteServiceAdmin "
                    + remoteServiceAdmin);

            if (exports.containsKey(remoteServiceAdmin)) {
                // already handled by this remoteServiceAdmin
                LOG.debug("already handled by this remoteServiceAdmin -> skipping");
            } else {
                // TODO: additional parameter Map ?
                LOG.debug("exporting ...");
                Collection<ExportRegistration> exportRegs = remoteServiceAdmin
                        .exportService(sref, null);
                if (exportRegs == null) {
                    // TODO export failed -> What should be done here?
                    LOG.error("export failed");
                    exports.put(remoteServiceAdmin, null);
                } else {
                	List<EndpointDescription> endpoints = new ArrayList<EndpointDescription>();
                	for (ExportRegistration exportReg : exportRegs) {
						endpoints.add(getExportedEndpoint(exportReg));
					}
                    LOG.info("TopologyManager: export sucessful Endpoints: {}", endpoints);
                    // enqueue in local list of endpoints
                    exports.put(remoteServiceAdmin, endpoints);

                    epListenerNotifier.nofifyEndpointListenersOfAdding(endpoints);
                }
            }
        }
    }
    
    /**
     * Retrieve exported Endpoint while handling null
     * @param exReg
     * @return exported Endpoint or null if not present
     */
    private EndpointDescription getExportedEndpoint(ExportRegistration exReg) {
        ExportReference ref = (exReg == null) ? null : exReg.getExportReference();
        return (ref == null) ? null : ref.getExportedEndpoint(); 
    }
    
    public Collection<EndpointDescription> getAllEndpoints() {
        List<EndpointDescription> registrations = new ArrayList<EndpointDescription>();
        synchronized (exportedServices) {
            for (Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports : exportedServices.values()) {
                for (Collection<EndpointDescription> regs : exports.values()) {
                    if (regs != null) {
                        registrations.addAll(regs);
                    }
                }
            }
        }
        return registrations;
    }

    private void exportExistingServices() {
        try {
			ServiceReference[] references = bctx.getServiceReferences(null, DOSGI_SERVICES);
			if (references != null) {
			    for (ServiceReference sref : references) {
			        exportService(sref);
			    }
			}
		} catch (InvalidSyntaxException e) {
			LOG.error("Error in filter {}. This should not occur !", DOSGI_SERVICES);
		}
    }

}
