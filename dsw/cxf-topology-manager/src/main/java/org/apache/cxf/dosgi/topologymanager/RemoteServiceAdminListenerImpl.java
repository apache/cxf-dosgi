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

import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

public class RemoteServiceAdminListenerImpl implements RemoteServiceAdminListener {

    private Logger LOG = Logger.getLogger(RemoteServiceAdminListenerImpl.class.getName());

    private BundleContext bctx;
    private ServiceRegistration serviceRegistration;
    private TopologyManager topManager;
    private TopologyManagerImport topManagerImport;

    public RemoteServiceAdminListenerImpl(BundleContext bctx,TopologyManager tm, TopologyManagerImport tmi) {
        this.bctx = bctx;
        this.topManager = tm;
        this.topManagerImport = tmi;
    }

    public void start() {
        // TODO: properties needed ?
        serviceRegistration = bctx.registerService(RemoteServiceAdminListener.class.getName(), this, null);
    }

    public void stop() {
        serviceRegistration.unregister();
    }

    public void remoteAdminEvent(RemoteServiceAdminEvent event) {
        LOG.fine("Received RemoteAdminEvent: " + event  + "   TYPE: "  + event.getType());

        switch (event.getType()) {
        case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION:
            topManager.removeExportReference(event.getExportReference());
            break;

        case RemoteServiceAdminEvent.IMPORT_UNREGISTRATION:
            topManagerImport.removeImportReference(event.getImportReference());
            break;
            
        default:
            LOG.info("Unhandled event type received: " + event.getType());

        }

    }

}
