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
package org.apache.cxf.dosgi.dsw.handlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.discovery.ServiceEndpointDescription;

public class ClientServiceFactory implements ServiceFactory {

    private static final Logger LOG = 
        Logger.getLogger(ClientServiceFactory.class.getName());

    private BundleContext dswContext;
    private Class<?> iClass;    
    private ServiceEndpointDescription sd;
    private ConfigurationTypeHandler handler;
    
    public ClientServiceFactory(BundleContext dswContext, Class<?> iClass,
                                ServiceEndpointDescription sd, ConfigurationTypeHandler handler) {
        this.dswContext = dswContext;
        this.iClass = iClass;
        this.sd = sd;
        this.handler = handler;
    }
    
    public Object getService(Bundle requestingBundle, ServiceRegistration sreg) {        
        String interfaceName = sd.getProvidedInterfaces() != null && sd.getProvidedInterfaces().size() > 0
	                       ? (String)sd.getProvidedInterfaces().toArray()[0]
			       : null;
        try {
            return handler.createProxy(sreg.getReference(), 
                                       dswContext, 
                                       requestingBundle.getBundleContext(),
                                       iClass,
                                       sd);
        } catch (IntentUnsatifiedException iue) {
            LOG.info("Did not create proxy for " + interfaceName + " because intent " +
                    iue.getIntent() + " could not be satisfied");
        } catch (Exception ex) {
            LOG.log(Level.WARNING,
                    "Problem creating a remote proxy for " 
                    + interfaceName + " from CXF FindHook: ", 
                    ex);
        }
        
        return null;
    }

    public void ungetService(Bundle requestingBundle, 
                             ServiceRegistration sreg, 
                             Object serviceObject) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("Releasing a client object");
        Object objectClass = 
            sreg.getReference().getProperty(org.osgi.framework.Constants.OBJECTCLASS);
        if (objectClass != null) {
            sb.append(", interfaces : ");
            for (String s : (String[])objectClass) { 
                sb.append(" " + s);
            }
        }
        LOG.info(sb.toString());
    }

}
