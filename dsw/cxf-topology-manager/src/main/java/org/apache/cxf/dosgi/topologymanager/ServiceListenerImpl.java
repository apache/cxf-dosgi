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
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class ServiceListenerImpl implements ServiceListener {
    
    private Logger LOG = Logger.getLogger(ServiceListenerImpl.class.getName());

    private BundleContext bctx;
    private TopologyManager topManager;
    
    public ServiceListenerImpl(BundleContext bc,TopologyManager tm) {
        bctx = bc; topManager = tm;
    }
    
    
    protected void start(){
        bctx.addServiceListener(this);
    }
    
    protected void stop(){
        bctx.removeServiceListener(this);
    }
    
    
    
    
    
    
    
    public void serviceChanged(ServiceEvent event) {
        LOG.fine("Received ServiceEvent: " + event);

        ServiceReference sref = event.getServiceReference();

        if (event.getType() == ServiceEvent.REGISTERED) {
            LOG.fine("Registered");
            if (analyzeService(sref)) {
                LOG.fine("calling TopologyManager -> registered service");
                topManager.exportService(sref);
            }
        } else if (event.getType() == ServiceEvent.UNREGISTERING) {
            topManager.removeService(sref);
        }
    }

    
    /**
     * checks if a Service is intended to be exported
     */
    private boolean analyzeService(ServiceReference sref) {

        if (sref.getProperty(RemoteConstants.SERVICE_EXPORTED_INTERFACES) != null) {
            return true;
        }
        return false;
    }
    
}
