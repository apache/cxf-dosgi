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


import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.discovery.Discovery;



public class Activator implements BundleActivator, ManagedService {

    private static final Logger LOG = Logger.getLogger(Activator.class.getName());
    
    private static final String CONFIG_SERVICE_PID = "discovery";
    private LocalDiscoveryService discoveryService;
    
    public void start(BundleContext context) {

        LOG.info("Registering ManagedService for LocalDiscoveryService bundle with service PID "
                 + CONFIG_SERVICE_PID);
        context.registerService(ManagedService.class.getName(), 
                                this, getDefaults());
        
        LOG.info("Registering LocalDiscoveryService service object");
        discoveryService = new LocalDiscoveryService(context);
        
        context.registerService(
                Discovery.class.getName(), 
                discoveryService,
                new Hashtable());
    }

    public void stop(BundleContext context) {
        discoveryService.shutdown();
    }

    private Dictionary<String, String> getDefaults() {
        Dictionary<String, String> defaults = new Hashtable<String, String>();
        defaults.put(Constants.SERVICE_PID, CONFIG_SERVICE_PID);        
        return defaults;
    } 
    
    public void updated(Dictionary props) throws ConfigurationException {
        if (props != null 
            && CONFIG_SERVICE_PID.equals(props.get(Constants.SERVICE_PID))) {
            discoveryService.updateProperties(props);
        }
    }
}
