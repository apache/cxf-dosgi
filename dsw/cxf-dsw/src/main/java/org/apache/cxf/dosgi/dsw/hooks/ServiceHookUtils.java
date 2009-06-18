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
package org.apache.cxf.dosgi.dsw.hooks;

import static org.osgi.service.discovery.ServicePublication.ENDPOINT_ID;
import static org.osgi.service.discovery.ServicePublication.ENDPOINT_LOCATION;
import static org.osgi.service.discovery.ServicePublication.SERVICE_INTERFACE_NAME;
import static org.osgi.service.discovery.ServicePublication.SERVICE_PROPERTIES;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.dsw.ClassUtils;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.handlers.ConfigTypeHandlerFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.handlers.IntentUnsatifiedException;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.apache.cxf.endpoint.Server;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.discovery.ServicePublication;

public final class ServiceHookUtils {
    
    private static final Logger LOG = Logger.getLogger(ServiceHookUtils.class.getName());
    
    private ServiceHookUtils() {
        
    }
    
    public static ServiceRegistration publish(BundleContext bc, final ServiceReference sr, ServiceEndpointDescription sd) {
        ServiceRegistration publication = bc.registerService(
                ServicePublication.class.getName(), new ServicePublication() {
                    public ServiceReference getReference() {
                        return sr;
                    }
                }, getPublicationProperties(sd));

        if (publication != null) {
            LOG.info("Remote " + sd.getProvidedInterfaces().toArray()[0]
                     + " endpoint published via Discovery service");
        }
        return publication;
    }
    
    public static Server createServer(ConfigurationTypeHandler handler,
                                      ServiceReference serviceReference,
                                      BundleContext dswContext,
                                      BundleContext callingContext, 
                                      ServiceEndpointDescription sd,
                                      Object serviceObject) {
        
        if (handler == null) {
            return null;
        }

        String interfaceName = (String)sd.getProvidedInterfaces().toArray()[0];
        // this is an extra sanity check, but do we really need it now ?
        Class<?> interfaceClass = ClassUtils.getInterfaceClass(serviceObject, interfaceName); 
                                                               
        if (interfaceClass != null) {
            try {
                return handler.createServer(serviceReference,
                                            dswContext,                                            
                                            callingContext, 
                                            sd, 
                                            interfaceClass, 
                                            serviceObject);
            } catch (IntentUnsatifiedException iue) {
                LOG.info("Did not remote service " + interfaceName
                         + " because intent " + iue.getIntent()
                         + " could not be satisfied");
            } catch (Exception ex) {
                LOG.warning("WARNING : Problem creating a remote endpoint for " + interfaceName
                        + " from CXF PublishHook, reason is " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        
        return null;
    }
    
    public static void unregisterServer(ServiceRegistration publication, 
                                        EndpointInfo ei) {
        
        try {
            Server server = ei.getServer();
            
            LOG.info("Stopping CXF Endpoint at " 
                + server.getDestination().getAddress().getAddress().getValue());
            server.getDestination().shutdown();
            server.stop();
        } catch (Exception ex) {
            // continue
        }
        
        if (ei.isPublished()) {
            LOG.info("Unpublishing Service Description for "  
                     + ei.getServiceDescription().getProvidedInterfaces().toArray()[0] 
                     + " from a Discovery service "); 
            publication.unregister();
        }
    }
    
    public static ConfigurationTypeHandler getHandler(BundleContext dswBC,
                                                      ServiceEndpointDescription sd,
                                                      CxfDistributionProvider dp,
                                                      Map<String, Object> dswProperties) {
        return ConfigTypeHandlerFactory.getInstance().getHandler(dswBC, sd, dp,
                                                                 dswProperties);
    }
    
    public static boolean isCreatedByDsw(ServiceReference sref) {
        return sref != null && sref.getProperty(Constants.DSW_CLIENT_ID) != null;
    }
    
    private static Map<String, Object> getServiceProperties(ServiceEndpointDescription sd) {
        Map<String, Object> props = new HashMap<String, Object>();
        for (Object key : sd.getPropertyKeys()) {
            props.put(key.toString(), sd.getProperty(key.toString()));
        }
        LOG.info("service properties: " + props);
        return props;
    }

    @SuppressWarnings("unchecked")
    private static Dictionary getPublicationProperties(ServiceEndpointDescription sd) {
        Dictionary props = new Hashtable();
        props.put(SERVICE_INTERFACE_NAME, sd.getProvidedInterfaces());
        props.put(SERVICE_PROPERTIES, getServiceProperties(sd));
        props.put(ENDPOINT_ID, UUID.randomUUID().toString());
        if (sd.getLocation() != null) {
            props.put(ENDPOINT_LOCATION, sd.getLocation());
        }
        LOG.info("publication properties: " + props);
        return props;
    }
}
