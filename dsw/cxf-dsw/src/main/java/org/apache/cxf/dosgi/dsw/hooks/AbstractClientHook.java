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

import static org.osgi.service.discovery.DiscoveredServiceNotification.AVAILABLE;
import static org.osgi.service.discovery.DiscoveredServiceNotification.MODIFIED;
import static org.osgi.service.discovery.DiscoveredServiceNotification.UNAVAILABLE;
import static org.osgi.service.discovery.DiscoveredServiceTracker.PROP_KEY_MATCH_CRITERIA_FILTERS;
import static org.osgi.service.discovery.DiscoveredServiceTracker.PROP_KEY_MATCH_CRITERIA_INTERFACES;
import static org.osgi.service.discovery.ServicePublication.PROP_KEY_ENDPOINT_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.handlers.ClientServiceFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServiceEndpointDescription;


public class AbstractClientHook extends AbstractHook {
    
    private static final Logger LOG = Logger.getLogger(AbstractClientHook.class.getName());

    private DiscoveredServiceTracker tracker;
    private Dictionary trackerProperties = new Hashtable();
    private ServiceRegistration trackerRegistration;
    private Map<String, ServiceEndpointDescription> discoveredServices =
        new HashMap<String, ServiceEndpointDescription>();

    protected AbstractClientHook(BundleContext bc, CxfDistributionProvider dp) {
        super(bc, dp);
        tracker = new DiscoveryCallback();
        trackerRegistration = 
            bc.registerService(DiscoveredServiceTracker.class.getName(),
                               tracker,
                               trackerProperties);
    }
    
    protected void processClientDescriptions(BundleContext requestingContext, 
                                             String interfaceName, 
                                             String filter) {
        
        lookupDiscoveryService(interfaceName, filter);
    }
    
    protected void processServiceDescription(ServiceEndpointDescription sd,
                                             BundleContext requestingContext, 
                                             String interfaceName) {
            
        if (sd.getProperty(Constants.REMOTE_INTERFACES_PROPERTY) == null) {
            return;
        }
            
        ConfigurationTypeHandler handler = 
            ServiceHookUtils.getHandler(getContext(), sd, getDistributionProvider(), getHandlerProperties());
        if (handler == null) {
            return;
        }
            
        try {
            Class<?> iClass = getContext().getBundle().loadClass(interfaceName);
            if (iClass != null) {
                BundleContext actualContext = getContext();
                Class<?> actualClass = requestingContext.getBundle().loadClass(interfaceName);
                if (actualClass != iClass) {
                    LOG.info("Class " + interfaceName + " loaded by DSW's bundle context is not "
                                 + "equal to the one loaded by the requesting bundle context, "
                                 + "DSW will use the requesting bundle context to register "
                                 + "a proxy service");
                    iClass = actualClass;
                    actualContext = requestingContext;
                }

                synchronized(this) {
                    if (cacheEndpointId(sd)) {
                        actualContext.registerService(new String[]{interfaceName},
                                                      new ClientServiceFactory(actualContext, iClass, sd, handler),
                                                      new Hashtable<String, Object>(getProperties(sd)));
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            LOG.warning("No class can be found for " + interfaceName);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getProperties(ServiceEndpointDescription sd) {
        Map<String, Object> props = new HashMap<String, Object>();        
        props.putAll(sd.getProperties());
        props.put(Constants.DSW_CLIENT_ID, getIdentificationProperty());
        props.put(Constants.REMOTE_PROPERTY_PREFIX, "true");
        return props;
    }

    protected synchronized void lookupDiscoveryService(String interfaceName, String filterValue) {

        if (interfaceName != null) {
            append(trackerProperties,
                   PROP_KEY_MATCH_CRITERIA_INTERFACES,
                   interfaceName); 
        }

        if (filterValue != null) {
            append(trackerProperties,
                   PROP_KEY_MATCH_CRITERIA_FILTERS,
                   filterValue); 
        }

        trackerRegistration.setProperties(trackerProperties);
    }

    @SuppressWarnings("unchecked")
    private void append(Dictionary properties, String key, String additional) {
        Collection existing = (Collection)properties.get(key);
        if (existing == null) {
            existing = new ArrayList<String>();
            properties.put(key, existing);
        }
        existing.add(additional);
    }

    private synchronized boolean cacheEndpointId(ServiceEndpointDescription notified) {
        String endpointId = (String)notified.getProperty(PROP_KEY_ENDPOINT_ID);
        if (endpointId != null) {
            boolean duplicate = discoveredServices.containsKey(endpointId);
            if (!duplicate) {
                discoveredServices.put(endpointId, notified);
                LOG.info("registering proxy for endpoint ID: " + endpointId);
            } else {
                LOG.warning("ignoring duplicate notification for endpoint ID: "
                            + endpointId);  
            }
            return !duplicate;
        } else {
            LOG.warning("registering proxy with unknown duplicate status as endpoint ID unset");  
            return true;
        }
    }

    private synchronized void unCacheEndpointId(ServiceEndpointDescription notified) {
        String endpointId = (String)notified.getProperty(PROP_KEY_ENDPOINT_ID);
        if (endpointId != null) {
            discoveredServices.remove(endpointId);
        }
    }

    private class DiscoveryCallback implements DiscoveredServiceTracker {
        public void serviceChanged(DiscoveredServiceNotification notification) {
            ServiceEndpointDescription notified =
                notification.getServiceEndpointDescription();
            switch (notification.getType()) {

            case AVAILABLE:
               LOG.info("Notified - AVAILABLE: " 
                         + notified.getProvidedInterfaces() 
                         + " endpoint id: " 
                         + notification.getServiceEndpointDescription().getProperty(PROP_KEY_ENDPOINT_ID));
                // REVISIT: OSGi bug 1022 will allow the matching interface
                // name to be gleaned from the notification, for now we just
                // assume its the first interface exposed by the SED
                String interfaceName =
                    (String)notified.getProvidedInterfaces().toArray()[0];
                processServiceDescription(notified,
                                          getContext(),
                                          interfaceName);
                break;

            case UNAVAILABLE:
                LOG.info("Notified - UNAVAILABLE: " + notified.getProvidedInterfaces()
                         + notified.getProvidedInterfaces() 
                         + " endpoint id: " 
                         + notification.getServiceEndpointDescription().getProperty(PROP_KEY_ENDPOINT_ID));
                unCacheEndpointId(notified);
                // we don't currently use this notification, but we could do
                // so to allow to drive transparent fail-over
                break;

            case MODIFIED:
                LOG.info("Notified - MODIFIED: " + notified.getProvidedInterfaces());
                // we don't currently use this notification, but we could do
                // so to allow to support transparent service re-location
                break;
            }
        }
    }    
}
