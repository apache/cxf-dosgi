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
import java.util.List;
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
public class TopologyManagerExport {

    private static final String DOSGI_SERVICES = "(" + RemoteConstants.SERVICE_EXPORTED_INTERFACES + "=*)";

    private static final Logger LOG = LoggerFactory.getLogger(TopologyManagerExport.class);

    private final BundleContext bctx;
    private final EndpointListenerNotifier epListenerNotifier;
    private final ExecutorService execService;
    private final RemoteServiceAdminTracker remoteServiceAdminTracker;
    private final ServiceListener serviceListener;
    private final EndpointRepository endpointRepo;

    public TopologyManagerExport(BundleContext ctx, RemoteServiceAdminTracker rsaTracker) {
        this(ctx, rsaTracker, null);
    }
    public TopologyManagerExport(BundleContext ctx, RemoteServiceAdminTracker rsaTracker,
                                 EndpointListenerNotifier notif) {
        endpointRepo = new EndpointRepository();
        if (notif == null) {
            epListenerNotifier = new EndpointListenerNotifier(ctx, endpointRepo);
        } else {
            epListenerNotifier = notif;
        }
        execService = new ThreadPoolExecutor(5, 10, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        bctx = ctx;
        this.remoteServiceAdminTracker = rsaTracker;
        this.remoteServiceAdminTracker.addListener(new RemoteServiceAdminLifeCycleListener() {
            public void added(RemoteServiceAdmin rsa) {
                for (ServiceReference serviceRef : endpointRepo.getServicesToBeExportedFor(rsa)) {
                    triggerExport(serviceRef);
                }
            }

            public void removed(RemoteServiceAdmin rsa) {
                List<EndpointDescription> endpoints = endpointRepo.removeRemoteServiceAdmin(rsa);
                epListenerNotifier.notifyListenersOfRemoval(endpoints);
            }
        });
        serviceListener = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                ServiceReference sref = event.getServiceReference();
                if (event.getType() == ServiceEvent.REGISTERED) {
                    LOG.debug("Received REGISTERED ServiceEvent: {}", event);
                    if (shouldExportService(sref)) {
                        triggerExport(sref);
                    }
                } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                    LOG.debug("Received UNREGISTERING ServiceEvent: {}", event);
                    List<EndpointDescription> endpoints = endpointRepo.removeService(sref);
                    epListenerNotifier.notifyListenersOfRemoval(endpoints);
                }
            }
        };
    }
    
    /**
     * checks if a Service is intended to be exported
     */
    private boolean shouldExportService(ServiceReference sref) {
        return sref.getProperty(RemoteConstants.SERVICE_EXPORTED_INTERFACES) != null;
    }

    public void start() {
        epListenerNotifier.start();
        bctx.addServiceListener(serviceListener);
        exportExistingServices();
    }

    public void stop() {
        execService.shutdown();
        bctx.removeServiceListener(serviceListener);
        epListenerNotifier.stop();
    }
    
    protected void triggerExport(final ServiceReference sref) {
        execService.execute(new Runnable() {
            public void run() {
                doExportService(sref);
            }
        });
    }

    protected void doExportService(final ServiceReference sref) {
        endpointRepo.addService(sref);
        List<RemoteServiceAdmin> rsaList = remoteServiceAdminTracker.getList();
        if (rsaList.size() == 0) {
            LOG.error(
                    "No RemoteServiceAdmin available! Unable to export service from bundle {}, interfaces: {}",
                    sref.getBundle().getSymbolicName(),
                    sref.getProperty(org.osgi.framework.Constants.OBJECTCLASS));
        }

        for (final RemoteServiceAdmin remoteServiceAdmin : rsaList) {
            LOG.info("TopologyManager: handling remoteServiceAdmin "
                    + remoteServiceAdmin);

            if (endpointRepo.isAlreadyExportedForRsa(sref, remoteServiceAdmin)) {
                // already handled by this remoteServiceAdmin
                LOG.debug("already handled by this remoteServiceAdmin -> skipping");
            } else {
                exportServiceUsingRemoteServiceAdmin(sref, remoteServiceAdmin);
            }
        }
    }

    private void exportServiceUsingRemoteServiceAdmin(final ServiceReference sref,
                                                      final RemoteServiceAdmin remoteServiceAdmin) {
        // TODO: additional parameter Map ?
        LOG.debug("exporting ...");
        Collection<ExportRegistration> exportRegs = remoteServiceAdmin.exportService(sref, null);
        List<EndpointDescription> endpoints = new ArrayList<EndpointDescription>();
        if (exportRegs.isEmpty()) {
            // TODO export failed -> What should be done here?
            LOG.error("export failed");
        } else {
            for (ExportRegistration exportReg : exportRegs) {
                endpoints.add(getExportedEndpoint(exportReg));
            }
            LOG.info("TopologyManager: export successful Endpoints: {}", endpoints);
            epListenerNotifier.nofifyEndpointListenersOfAdding(endpoints);
        }
        endpointRepo.addEndpoints(sref, remoteServiceAdmin, endpoints);
    }

    /**
     * Retrieves an exported Endpoint (while safely handling nulls).
     *
     * @param exReg an export registration
     * @return exported Endpoint or null if not present
     */
    private EndpointDescription getExportedEndpoint(ExportRegistration exReg) {
        ExportReference ref = (exReg == null) ? null : exReg.getExportReference();
        return (ref == null) ? null : ref.getExportedEndpoint(); 
    }
    
    private void exportExistingServices() {
        try {
            // cast to String is necessary for compiling against OSGi core version >= 4.3
            ServiceReference[] references = bctx.getServiceReferences((String)null, DOSGI_SERVICES);
            if (references != null) {
                for (ServiceReference sref : references) {
                    triggerExport(sref);
                }
            }
        } catch (InvalidSyntaxException e) {
            LOG.error("Error in filter {}. This should not occur !", DOSGI_SERVICES);
        }
    }

}
