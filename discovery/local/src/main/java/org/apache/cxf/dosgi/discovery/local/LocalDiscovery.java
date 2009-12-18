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
package org.apache.cxf.dosgi.discovery.local;

import java.util.List;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class LocalDiscovery implements BundleListener {   
    private static final Logger LOG = Logger.getLogger(LocalDiscovery.class.getName());
    
    private final BundleContext bundleContext;

    public LocalDiscovery(BundleContext bc) {
        bundleContext = bc;
        
        bundleContext.addBundleListener(this);
    }

    public void shutDown() {
        bundleContext.removeBundleListener(this);
    }

    // BundleListener method
    public void bundleChanged(BundleEvent be) {
        switch (be.getType()) {
        case BundleEvent.STARTED:
            findDeclaredRemoteServices(be.getBundle());
            break;
        case BundleEvent.STOPPING:
            break;
        }
    }

    private void findDeclaredRemoteServices(Bundle bundle) {
//        List<EndpointDescription> refs = LocalDiscoveryUtils.getAllRemoteReferences(bundle);
    }
}
