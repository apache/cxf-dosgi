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
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class RemoteServiceAdminList extends ArrayList<RemoteServiceAdmin>{

    private BundleContext bctx;
    private ServiceTracker stRemoteServiceAdmin;
    private TopologyManager topManager;
    private TopologyManagerImport topManagerImport;
    
    private final static Logger LOG = Logger.getLogger(RemoteServiceAdminList.class.getName()); 
    
    
    public RemoteServiceAdminList(BundleContext bc) {

        bctx = bc;
        
        final RemoteServiceAdminList rsal = this;
        
        stRemoteServiceAdmin = new ServiceTracker(bctx, RemoteServiceAdmin.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("Adding RemoteServiceAdmin to list of admins ");
                RemoteServiceAdmin rsa = (RemoteServiceAdmin)bctx.getService(reference);
                synchronized (rsal) {
                    rsal.add(rsa);
                }
                LOG.info("enlisted RemoteEventAdmins: " + this.size());

                triggerExportImportForRemoteServiceAdmin(rsa);

                return super.addingService(reference);
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                LOG.info("TopologyManager: Removing RemoteServiceAdmin from list of admins ");
                synchronized (rsal) {
                    rsal.remove(service);
                }

                // TODO: remove service exports from management structure and notify discovery stuff...
                removeRemoteServiceAdmin((RemoteServiceAdmin)service);

                LOG.info("TopologyManager: enlisted RemoteEventAdmins: " + rsal.size());

                super.removedService(reference, service);
            }
        };
    
    
        
        
    } 

    
    protected void removeRemoteServiceAdmin(RemoteServiceAdmin service) {
        topManager.removeRemoteServiceAdmin(service);
    }


    protected void triggerExportImportForRemoteServiceAdmin(RemoteServiceAdmin rsa) {
        topManager.triggerExportImportForRemoteServiceAdmin(rsa);
        topManagerImport.triggerExportImportForRemoteServiceAdmin(rsa);
    }


    public void start(){
        stRemoteServiceAdmin.open();
    }

    
    public void stop(){
        stRemoteServiceAdmin.close();
    }


    public void setTopologyManager(TopologyManager tm) {
        topManager = tm;
    }


    public void setTopologyManagerImport(TopologyManagerImport tmi) {
        topManagerImport = tmi;
    }
    
    
}
