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
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;

// TODO: realize as ServiceFactory ! 
public class EndpointListenerImpl implements EndpointListener {

    private static final Logger LOG = Logger.getLogger(EndpointListenerImpl.class.getName());
    
    private final BundleContext bctx;
    private ServiceRegistration serviceRegistration;
    private List<String> filters = new ArrayList<String>();
    private TopologyManagerImport topManager;

    protected EndpointListenerImpl(BundleContext bc, TopologyManagerImport tm) {
        bctx = bc;
        topManager = tm;
    }

    protected void start() {
        serviceRegistration = bctx.registerService(EndpointListener.class.getName(), this,
                                                   getRegistrationProperties());
    }



    protected void stop() {
        serviceRegistration.unregister();
    }

    protected void extendScope(String filter) {
        if (filter == null)
            return;
        
        
        LOG.fine("EndpointListener: extending scope by " + filter);

        synchronized (filters) {
            filters.add(filter);
        }
        updateRegistration();
    }

   

    protected void reduceScope(String filter) {
        if (filter == null)
            return;
        
        
        LOG.fine("EndpointListener: reducing scope by " + filter);
        synchronized (filters) {
            filters.remove(filter);
        }
        updateRegistration();
    }

    private Dictionary getRegistrationProperties() {
        Properties p = new Properties();

        synchronized (filters) {
            LOG.finer("EndpointListener: current filter: " + filters);
            // TODO: make a copy of the filter list
            p.put(EndpointListener.ENDPOINT_LISTENER_SCOPE, filters);
        }

        return p;
    }

    private void updateRegistration() {
        // This tends to be verbose.
        LOG.finer("EndpointListenerImpl: filters: " + filters);

        serviceRegistration.setProperties(getRegistrationProperties());
    }

    public void endpointAdded(EndpointDescription epd, String filter) {
        LOG.fine("EndpointListenerImpl: EndpointAdded() filter:"+filter+"  EndpointDesc:"+epd);
        
        if(filter==null){
            LOG.severe("Endpoint is not handled because no matching filter was provided! Filter: "+filter);
            return;
        }
        // Decide if it is worth it ? 
        
        topManager.addImportableService(filter,epd);
        
    }

    public void endpointRemoved(EndpointDescription epd, String filter) {
        LOG.fine("EndpointListenerImpl: EndpointRemoved() -> "+epd);
        topManager.removeImportableService(filter, epd);
    }

}
