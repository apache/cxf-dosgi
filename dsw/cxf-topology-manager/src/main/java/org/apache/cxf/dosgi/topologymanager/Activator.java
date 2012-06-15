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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private TopologyManager topManager;
    private TopologyManagerImport topManagerImport;

    private RemoteServiceAdminList remoteServiceAdminList;
    
    protected RemoteServiceAdminListenerImpl remoteServiceAdminListener;
    
    // separated for testing
    protected TopologyManager createTopologyManager(BundleContext bc,RemoteServiceAdminList rl) {
        return new TopologyManager(bc,rl);
    }

 // separated for testing
    protected TopologyManagerImport createTopologyManagerImport(BundleContext bc,RemoteServiceAdminList rl) {
        return new TopologyManagerImport(bc,rl);
    }
    
 // separated for testing
    protected RemoteServiceAdminList createRemoteServiceAdminList(BundleContext bc) {
        return new RemoteServiceAdminList(bc);
    }

    // separated for testing
    protected RemoteServiceAdminListenerImpl createRemoteServiceAdminListenerImpl(BundleContext bc,TopologyManager topManager,TopologyManagerImport topManagerImport) {
        return new RemoteServiceAdminListenerImpl(bc, topManager, topManagerImport);
    }
    
    public void start(BundleContext bc) throws Exception {
        LOG.fine("TopologyManager: start()");
        remoteServiceAdminList = createRemoteServiceAdminList(bc);
        topManager = createTopologyManager(bc,remoteServiceAdminList);
        topManagerImport = createTopologyManagerImport(bc,remoteServiceAdminList);

        remoteServiceAdminList.setTopologyManager(topManager);
        remoteServiceAdminList.setTopologyManagerImport(topManagerImport);
        
        remoteServiceAdminListener = createRemoteServiceAdminListenerImpl(bc, topManager, topManagerImport);        
        
        remoteServiceAdminListener.start();
        
        topManager.start();
        
        remoteServiceAdminList.start();
        
        topManagerImport.start();
        
        
    }

    public void stop(BundleContext bc) throws Exception {
        LOG.fine("TopologyManager: stop()");
        topManager.stop();
        topManagerImport.stop();
        remoteServiceAdminList.stop();
        remoteServiceAdminListener.stop();
    }

}
