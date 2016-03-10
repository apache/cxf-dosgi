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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.dosgi.topologymanager.exporter.EndpointListenerNotifier;
import org.apache.cxf.dosgi.topologymanager.exporter.EndpointRepository;
import org.apache.cxf.dosgi.topologymanager.exporter.TopologyManagerExport;
import org.apache.cxf.dosgi.topologymanager.importer.TopologyManagerImport;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {
    private static final String DOSGI_SERVICES = "(" + RemoteConstants.SERVICE_EXPORTED_INTERFACES + "=*)";
    private static final String ENDPOINT_LISTENER_FILTER =
        "(&(" + Constants.OBJECTCLASS + "=" + EndpointListener.class.getName() + ")"
        + "(" + EndpointListener.ENDPOINT_LISTENER_SCOPE + "=*))";
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private TopologyManagerExport exportManager;
    private TopologyManagerImport importManager;
    private EndpointListenerNotifier notifier;
    private ServiceTracker<RemoteServiceAdmin, RemoteServiceAdmin> rsaTracker;
    private ThreadPoolExecutor exportExecutor;
    private ServiceTracker<EndpointListener, EndpointListener> epListenerTracker;

    public void start(final BundleContext bc) throws Exception {
        LOG.debug("TopologyManager: start()");
        EndpointRepository endpointRepo = new EndpointRepository();
        notifier = new EndpointListenerNotifier(endpointRepo);
        epListenerTracker = new ServiceTracker<EndpointListener, EndpointListener>(bc, EndpointListener.class, null) {
            @Override
            public EndpointListener addingService(ServiceReference<EndpointListener> reference) {
                EndpointListener listener = super.addingService(reference);
                notifier.add(listener, EndpointListenerNotifier.getFiltersFromEndpointListenerScope(reference));
                return listener;
            }
            
            @Override
            public void modifiedService(ServiceReference<EndpointListener> reference,
                                        EndpointListener listener) {
                super.modifiedService(reference, listener);
                notifier.add(listener, EndpointListenerNotifier.getFiltersFromEndpointListenerScope(reference));
            }
            
            @Override
            public void removedService(ServiceReference<EndpointListener> reference,
                                       EndpointListener listener) {
                notifier.remove(listener);
                super.removedService(reference, listener);
            }
        };
        endpointRepo.setNotifier(notifier);
        exportExecutor = new ThreadPoolExecutor(5, 10, 50, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        exportManager = new TopologyManagerExport(endpointRepo, exportExecutor);
        importManager = new TopologyManagerImport(bc);
        rsaTracker = new RSATracker(bc, RemoteServiceAdmin.class, null);
        bc.addServiceListener(exportManager);
        rsaTracker.open();
        epListenerTracker.open();
        exportExistingServices(bc);
        importManager.start();
    }

    public void stop(BundleContext bc) throws Exception {
        LOG.debug("TopologyManager: stop()");
        epListenerTracker.close();
        bc.removeServiceListener(exportManager);
        exportExecutor.shutdown();
        importManager.stop();
        rsaTracker.close();
    }
    
    public static Filter epListenerFilter(BundleContext bctx) {
        try {
            return bctx.createFilter(ENDPOINT_LISTENER_FILTER);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException("Unexpected exception creating filter", e);
        }
    }
    
    public void exportExistingServices(BundleContext context) {
        try {
            // cast to String is necessary for compiling against OSGi core version >= 4.3
            ServiceReference<?>[] references = context.getServiceReferences((String)null, DOSGI_SERVICES);
            if (references != null) {
                for (ServiceReference<?> sref : references) {
                    exportManager.export(sref);
                }
            }
        } catch (InvalidSyntaxException e) {
            LOG.error("Error in filter {}. This should not occur!", DOSGI_SERVICES);
        }
    }
    
    private final class RSATracker extends ServiceTracker<RemoteServiceAdmin, RemoteServiceAdmin> {
        private RSATracker(BundleContext context, Class<RemoteServiceAdmin> clazz,
                           ServiceTrackerCustomizer<RemoteServiceAdmin, RemoteServiceAdmin> customizer) {
            super(context, clazz, customizer);
        }

        @Override
        public RemoteServiceAdmin addingService(ServiceReference<RemoteServiceAdmin> reference) {
            RemoteServiceAdmin rsa = super.addingService(reference);
            LOG.debug("New RemoteServiceAdmin {} detected, trying to import and export services with it", rsa);
            importManager.add(rsa);
            exportManager.add(rsa);
            return rsa;
        }

        @Override
        public void removedService(ServiceReference<RemoteServiceAdmin> reference,
                                   RemoteServiceAdmin rsa) {
            exportManager.remove(rsa);
            importManager.remove(rsa);
            super.removedService(reference, rsa);
        }
    }
}
