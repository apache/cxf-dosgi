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

// *************************** FIXME: some old methods might be in here ****
public class ImportRegistrationImpl implements ImportRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(ImportRegistrationImpl.class);

    private Throwable exception;
    private ServiceRegistration importedService;
    private EndpointDescription importedEndpoint;
    private ClientServiceFactory clientServiceFactory;
    private RemoteServiceAdminCore rsaCore;
    private boolean closed;
    private boolean detatched;

    private ImportRegistrationImpl parent;
    private List<ImportRegistrationImpl> childs;

    private ImportReference importReference;
    
    public ImportRegistrationImpl(Throwable ex) {
        exception = ex;
        init();
    }

    public ImportRegistrationImpl(EndpointDescription endpoint, RemoteServiceAdminCore rsac) {
        importedEndpoint = endpoint;
        rsaCore = rsac;
        init();
    }

    /**
     * Create a clone of this object which is linked to this object
     */
    public ImportRegistrationImpl(ImportRegistrationImpl ir) {
        // we always want a link to the parent ...
        ir = ir.getParent();

        parent = ir;
        exception = ir.getException();
        importedEndpoint = ir.getImportedEndpointDescription();
        importedService = ir.getImportedServiceRegistration();
        clientServiceFactory = ir.getClientServiceFactory();
        rsaCore = ir.getRsaCore();

        parent.instanceAdded(this);
    }

    private void init() {
        parent = this;
        childs = new ArrayList<ImportRegistrationImpl>(1);
    }

    private synchronized void instanceAdded(ImportRegistrationImpl i) {
        childs.add(i);
    }

    public synchronized void close() {
        LOG.debug("close() called ");

        if (isFailure()) {
            return;
        }

        if (closed) {
            return;
        }

        closed = true;
        rsaCore.removeImportRegistration(this);
        parent.instanceClosed(this);
    }

    /**
     * only called on the parent object
     */
    private synchronized void instanceClosed(ImportRegistrationImpl i) {
        childs.remove(i);

        if (childs.isEmpty() && !detatched && closed) {
            detatched = true; 
            
            LOG.debug("really closing ImportRegistartion now! ");

            if (clientServiceFactory != null) {
                clientServiceFactory.setCloseable(true);
            }
            if (importedService != null) {
                importedService.unregister();
            }
        }
    }

    /**
     * used to close all ImportRegistrations in the case of an error ...
     */
    public synchronized void closeAll() {
        if (this == parent) {
            LOG.info("closing down all child ImportRegistrations");

            for (ImportRegistrationImpl ir : new ArrayList<ImportRegistrationImpl>(childs)) {
                ir.close();
            }
            if (!closed) {
                this.close();
            }
        } else {
            parent.closeAll();
        }
    }

    private ServiceRegistration getImportedServiceRegistration() {
        return importedService;
    }

    public Throwable getException() {
        return exception;
    }

    public EndpointDescription getImportedEndpointDescription() {
        if (isFailure()) {
            return null;
        }
        if (closed) {
            return null;
        }
        
        return importedEndpoint;
    }

    public ServiceReference getImportedService() {
        if (isFailure() || closed) {
            return null;
        }

        if (importedService == null) {
            return null;
        }

        return importedService.getReference();
    }

    public void setException(Throwable ex) {
        exception = ex;
    }

    private boolean isFailure() {
        return exception != null;
    }

    private void _setImportedServiceRegistration(ServiceRegistration proxyRegistration) {
        importedService = proxyRegistration;
    }

    public synchronized void setImportedServiceRegistration(ServiceRegistration proxyRegistration) {
        if (parent != this) {
            throw new IllegalStateException("this method may only be called on the parent !");
        }

        _setImportedServiceRegistration(proxyRegistration);
        for (ImportRegistrationImpl ir : childs) {
            ir._setImportedServiceRegistration(proxyRegistration);
        }
    }

    public void setClientServiceFactory(ClientServiceFactory csf) {
        clientServiceFactory = csf;
    }

    public RemoteServiceAdminCore getRsaCore() {
        return rsaCore;
    }

    public void setRsaCore(RemoteServiceAdminCore rsaCore) {
        this.rsaCore = rsaCore;
    }

    public ClientServiceFactory getClientServiceFactory() {
        return clientServiceFactory;
    }

    public void setParent(ImportRegistrationImpl parent) {
        this.parent = parent;
    }

    public ImportRegistrationImpl getParent() {
        return parent;
    }

    public ImportReference getImportReference() {
        if (importReference == null) {
            importReference = new ImportReferenceImpl(this);
        }
        return importReference;
    }

    public EndpointDescription getImportedEndpointAlways() {
        return importedEndpoint;
    }

}
