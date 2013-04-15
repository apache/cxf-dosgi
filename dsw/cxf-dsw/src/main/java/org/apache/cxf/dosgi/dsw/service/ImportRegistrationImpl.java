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

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportRegistrationImpl implements ImportRegistration, ImportReference {

    private static final Logger LOG = LoggerFactory.getLogger(ImportRegistrationImpl.class);

    private Throwable exception;
    private ServiceRegistration importedService; // used only in parent
    private EndpointDescription importedEndpoint;
    private ClientServiceFactory clientServiceFactory;
    private RemoteServiceAdminCore rsaCore;
    private boolean closed;
    private boolean detached; // used only in parent

    private ImportRegistrationImpl parent;
    private List<ImportRegistrationImpl> children; // used only in parent

    public ImportRegistrationImpl(Throwable ex) {
        exception = ex;
        initParent();
    }

    public ImportRegistrationImpl(EndpointDescription endpoint, RemoteServiceAdminCore rsac) {
        importedEndpoint = endpoint;
        rsaCore = rsac;
        initParent();
    }

    /**
     * Creates a clone of the given parent instance.
     */
    public ImportRegistrationImpl(ImportRegistrationImpl ir) {
        // we always want a link to the parent ...
        parent = ir.getParent();
        exception = parent.getException();
        importedEndpoint = parent.getImportedEndpointDescription();
        clientServiceFactory = parent.clientServiceFactory;
        rsaCore = parent.rsaCore;

        parent.instanceAdded(this);
    }

    private void initParent() {
        parent = this;
        children = new ArrayList<ImportRegistrationImpl>(1);
    }

    /**
     * Called on parent when a child is added.
     *
     * @param iri the child
     */
    private synchronized void instanceAdded(ImportRegistrationImpl iri) {
        children.add(iri);
    }

    /**
     * Called on parent when a child is closed.
     *
     * @param iri the child
     */
    private synchronized void instanceClosed(ImportRegistrationImpl iri) {
        children.remove(iri);

        if (children.isEmpty() && !detached && closed) {
            detached = true;

            LOG.debug("really closing ImportRegistartion now");

            if (clientServiceFactory != null) {
                clientServiceFactory.setCloseable(true);
            }
            if (importedService != null) {
                importedService.unregister();
            }
        }
    }

    public synchronized void close() {
        LOG.debug("close() called");

        if (isInvalid()) {
            return;
        }

        closed = true;
        rsaCore.removeImportRegistration(this);
        parent.instanceClosed(this);
    }

    /**
     * Closes all ImportRegistrations which share the same parent as this one.
     */
    public synchronized void closeAll() {
        if (this == parent) {
            LOG.info("closing down all child ImportRegistrations");

            // we must iterate over a copy of children since close() removes the child
            // from the list (which would cause a ConcurrentModificationException)
            for (ImportRegistrationImpl ir : new ArrayList<ImportRegistrationImpl>(children)) {
                ir.close();
            }
            this.close();
        } else {
            parent.closeAll();
        }
    }

    public EndpointDescription getImportedEndpointDescription() {
        return isInvalid() ? null : importedEndpoint;
    }

    @Override
    public EndpointDescription getImportedEndpoint() {
        return getImportedEndpointDescription();
    }

    @Override
    public ServiceReference getImportedService() {
        return isInvalid() || parent.importedService == null ? null : parent.importedService.getReference();
    }

    @Override
    public ImportReference getImportReference() {
        return this;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable ex) {
        exception = ex;
    }

    private boolean isInvalid() {
        return exception != null || closed;
    }

    public synchronized void setImportedServiceRegistration(ServiceRegistration proxyRegistration) {
        if (parent != this) {
            throw new IllegalStateException("this method may only be called on the parent");
        }

        importedService = proxyRegistration;
    }

    public void setClientServiceFactory(ClientServiceFactory csf) {
        clientServiceFactory = csf;
    }

    public ImportRegistrationImpl getParent() {
        return parent;
    }

    /**
     * Returns the imported endpoint even if this
     * instance is closed or has an exception.
     *
     * @return the imported endpoint
     */
    public EndpointDescription getImportedEndpointAlways() {
        return importedEndpoint;
    }

}
