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
package org.apache.cxf.dosgi.dsw.service;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.endpoint.Server;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportRegistrationImpl implements ExportRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(ExportRegistrationImpl.class);

    private Server server;
    private boolean closed;
    private Throwable exception;

    private ExportRegistrationImpl parent;
    private volatile int instanceCount = 1;

    private RemoteServiceAdminCore rsaCore;

    private ExportReferenceImpl exportReference;

    private ServiceTracker serviceTracker;

    // provide a clone of the provided exp.Reg that is linked to this instance
    public ExportRegistrationImpl(ExportRegistrationImpl exportRegistration) {
        parent = exportRegistration;
        exportReference = new ExportReferenceImpl(parent.getExportReference());
        exception = parent.getException();
        rsaCore = parent.getRsaCore();
        parent.instanceAdded();
    }
    public ExportRegistrationImpl(ServiceReference sref,
                                  EndpointDescription endpoint,
                                  RemoteServiceAdminCore remoteServiceAdminCore) {
        exportReference = new ExportReferenceImpl(sref, endpoint);
        parent = this;
        rsaCore = remoteServiceAdminCore;
    }

    private void instanceAdded() {
        ++instanceCount;
    }


    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;

        rsaCore.removeExportRegistration(this);

        parent.instanceClosed();
        if (server != null) {
            server.stop();
            server = null;
        }
        exportReference.close();
    }

    private void instanceClosed() {
        --instanceCount;
        if (instanceCount <= 0) {
            // really close the ExReg
            // TODO close it and remove from management structure .... !

            LOG.debug("really closing ExportRegistartion now! ");

            synchronized (this) {
                if (server != null) {
                    // FIXME: is this done like this ?
                    server.stop();
                    server = null;
                }
            }
        }
    }

    public Throwable getException() {
        return closed ? null : exception;
    }

    @Override
    public String toString() {
        if (exportReference == null) {
            return "Exportregistration closed";
        }
        EndpointDescription endpointDescription = getExportReference().getExportedEndpoint();
        ServiceReference serviceReference = getExportReference().getExportedService();
        String r = "Endpoint Desctiption for ServiceReference " + serviceReference;
        r += "\n";

        r += "*** EndpointDescription: **** \n";
        if (endpointDescription == null) {
            r += "---> NULL <---- \n";
        } else {
            Set<Map.Entry<String, Object>> props = endpointDescription.getProperties().entrySet();
            for (Map.Entry<String, Object> entry : props) {
                Object value = entry.getValue();
                r += entry.getKey() + "  => " 
                    + (value instanceof Object[] ? Arrays.toString((Object []) value) : value) + "\n";
            }
        }
        return r;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Server getServer() {
        return server;
    }

    public void setException(Throwable ex) {
        exception = ex;
    }

    public ExportReference getExportReference() {
        return exportReference;
    }

    /**
     * Start the service tracker that monitors the osgi service that
     * is exported by this exportRegistration
     * */
    public void startServiceTracker(BundleContext bctx) {

        // only the parent should do this
        if (parent != this) {
            parent.startServiceTracker(bctx);
            return;
        }

        // do it only once
        if (serviceTracker != null) {
            return;
        }

        Filter f;
        final Long sid = (Long)getExportReference().getExportedService().getProperty(Constants.SERVICE_ID);
        try {
            f = bctx.createFilter("(" + Constants.SERVICE_ID + "=" + sid + ")");
        } catch (InvalidSyntaxException e) {
            LOG.warn("Service tracker could not be started. The service will not be automatically unexported "
                + e.getMessage(), e);
            return;
        }
        serviceTracker = new ServiceTracker(bctx, f, new ServiceTrackerCustomizer() {

            public void removedService(ServiceReference sr, Object s) {
                LOG.info("Service [" + sid + "] has been unregistered: Removing service export");
                close();
            }

            public void modifiedService(ServiceReference sr, Object s) {
                // FIXME:
                LOG.warn("Service modifications after the service is exported are "
                         + "currently not supported. The export is not modified!");
            }

            public Object addingService(ServiceReference sr) {
                return sr;
            }
        });
        serviceTracker.open();
    }

    public void setRsaCore(RemoteServiceAdminCore rsaCore) {
        this.rsaCore = rsaCore;
    }

    public RemoteServiceAdminCore getRsaCore() {
        return rsaCore;
    }
}
