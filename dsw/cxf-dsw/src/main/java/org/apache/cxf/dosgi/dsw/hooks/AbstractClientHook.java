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
import static org.osgi.service.discovery.DiscoveredServiceTracker.FILTER_MATCH_CRITERIA;
import static org.osgi.service.discovery.DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA;
import static org.osgi.service.discovery.ServicePublication.ENDPOINT_ID;
import static org.osgi.service.discovery.ServicePublication.SERVICE_INTERFACE_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.handlers.ClientServiceFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServiceEndpointDescription;


public class AbstractClientHook extends AbstractHook {
    
    private static final Logger LOG = Logger.getLogger(AbstractClientHook.class.getName());

    private DiscoveredServiceTracker tracker;
    private Dictionary trackerProperties = new Hashtable();
    private Map<String, ServiceRegistration> discoveredServices =
        new HashMap<String, ServiceRegistration>();
    ServiceRegistration trackerRegistration;

    protected AbstractClientHook(BundleContext bc, CxfDistributionProvider dp) {
        super(bc, dp);
        tracker = new DiscoveryCallback();
        trackerRegistration = 
            bc.registerService(DiscoveredServiceTracker.class.getName(),
                               tracker,
                               trackerProperties);
    }
        
    protected void processNotification(DiscoveredServiceNotification notification,
                                       BundleContext requestingContext) {
            
        ServiceEndpointDescription sd = 
            notification.getServiceEndpointDescription();
        if ((sd.getProperty(Constants.EXPORTED_INTERFACES) == null) &&
            (sd.getProperty(Constants.EXPORTED_INTERFACES_OLD) == null)) {
            return;
        }
            
        ConfigurationTypeHandler handler = 
            ServiceHookUtils.getHandler(getContext(), sd, getDistributionProvider(), getHandlerProperties());
        if (handler == null) {
            LOG.info("not proxifying service, config type handler null");
            return;
        }
       
        Collection<String> matchingInterfaces =
            getMatchingInterfaces(notification, requestingContext);

        for (String interfaceName : matchingInterfaces) {
            proxifyMatchingInterface(interfaceName, sd, handler, requestingContext);
        }

    }
    

    private void proxifyMatchingInterface(String interfaceName,
                                          ServiceEndpointDescription sd,
                                          ConfigurationTypeHandler handler,
                                          BundleContext requestingContext) {

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

                synchronized(discoveredServices) {
                    if (unknownEndpointId(sd)) {
                        ServiceRegistration proxyRegistration = 
                            actualContext.registerService(interfaceName,
                                                          new ClientServiceFactory(actualContext, iClass, sd, handler),
                                                          new Hashtable<String, Object>(getProperties(sd, handler)));
                        cacheEndpointId(sd, proxyRegistration);
                    }
                }
            } else {
                LOG.info("not proxifying service, cannot load interface class: "
                + interfaceName);
            }
        } catch (ClassNotFoundException ex) {
            LOG.warning("No class can be found for " + interfaceName);
        }
    }

    private Collection<String> getMatchingInterfaces(DiscoveredServiceNotification notification, BundleContext context) {      
        Collection<String> matches = new ArrayList<String>();
        Iterator interfaces = notification.getServiceEndpointDescription().getProvidedInterfaces().iterator();

        while (interfaces.hasNext()) {
            String currInterface = (String)interfaces.next();
            boolean matched = false;
            Iterator matchedInterfaces = 
                notification.getInterfaces().iterator();
            while (matchedInterfaces.hasNext() && !matched) {
                matched = currInterface.equals(matchedInterfaces.next());
                if (matched) {
                    matches.add(currInterface);
                }
            }
            Iterator matchedFilters =
                notification.getFilters().iterator();
            while (matchedFilters.hasNext() && !matched) {
                String filterString = (String)matchedFilters.next();
                try {
                    Filter filter = context.createFilter(filterString);
                    matched = 
                        filter.match(getProperties(notification, currInterface));
                } catch (InvalidSyntaxException ise) {
                    LOG.warning("invalid filter syntax: " + filterString);
                }
                if (matched) {
                    matches.add(currInterface);
                }
            }
        }

        return matches;
    }
    
    private Hashtable getProperties(DiscoveredServiceNotification notification,
                                    String interfaceName) {
        Hashtable ret = new Hashtable();
        Map properties = notification.getServiceEndpointDescription().getProperties();
        Iterator keys = notification.getServiceEndpointDescription().getPropertyKeys().iterator();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            ret.put(key, properties.get(key));
        }
        ret.put(SERVICE_INTERFACE_NAME, interfaceName);
        return ret;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getProperties(ServiceEndpointDescription sd, ConfigurationTypeHandler handler) {
        Map<String, Object> props = new HashMap<String, Object>();        
        props.putAll(sd.getProperties());
        
        for (Iterator<Map.Entry<String, Object>> i = props.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<String, Object> e = i.next();
            if (e.getKey().startsWith("service.exported")) {
                i.remove();
            }
        }
        
        props.put(Constants.DSW_CLIENT_ID, getIdentificationProperty());
        props.put(Constants.IMPORTED, "true");
        props.put(Constants.IMPORTD_CONFIGS, handler.getType());
        return props;
    }

    protected synchronized void lookupDiscoveryService(String interfaceName, String filterValue) {
        LOG.info("lookup discovery service: interface: " + interfaceName
                 + " filter: " + filterValue);

        boolean change = false;
        if (interfaceName != null) {
            change |= append(trackerProperties,
                   INTERFACE_MATCH_CRITERIA,
                   interfaceName); 
        }

        if (filterValue != null) {
            change |= append(trackerProperties,
                   FILTER_MATCH_CRITERIA,
                   filterValue); 
        }

        if (change) {
            trackerRegistration.setProperties(trackerProperties);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean append(Dictionary properties, String key, String additional) {
        Collection existing = (Collection)properties.get(key);
        if (existing == null) {
            existing = new ArrayList<String>();
            properties.put(key, existing);
        }
        
        if (!existing.contains(additional)) {
            existing.add(additional);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @pre called with discoveredServices mutex held
     */
    private boolean unknownEndpointId(ServiceEndpointDescription notified) {
        String endpointId = (String)notified.getProperty(ENDPOINT_ID);
        if (endpointId != null) {
            boolean duplicate = discoveredServices.containsKey(endpointId);
            if (!duplicate) {
                LOG.info("registering proxy for endpoint ID: " + endpointId);
            } else {
                LOG.info("ignoring duplicate notification for endpoint ID: "
                            + endpointId);  
            }
            return !duplicate;
        } else {
            LOG.warning("registering proxy with unknown duplicate status as endpoint ID unset");  
            return true;
        }
    }

    /**
     * @pre called with discoveredServices mutex held
     */
    private void cacheEndpointId(ServiceEndpointDescription notified, ServiceRegistration registration) {
        String endpointId = (String)notified.getProperty(ENDPOINT_ID);
        if (endpointId != null) {
            discoveredServices.put(endpointId, registration);
            LOG.info("caching proxy registration for endpoint ID: " + endpointId);
        } else {
            LOG.warning("cannot cache proxy registration as endpoint ID unset");  
        }
    }

    private void unCacheEndpointId(ServiceEndpointDescription notified) {
        String endpointId = (String)notified.getProperty(ENDPOINT_ID);
        ServiceRegistration proxyRegistration = null;
        if (endpointId != null) {
            synchronized (discoveredServices) {
                proxyRegistration = discoveredServices.remove(endpointId);
            }
        }
        if (proxyRegistration != null) {
            LOG.info("unregistering proxy service for endpoint ID: "
                     + endpointId);
            proxyRegistration.unregister();
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
                         + notification.getServiceEndpointDescription().getProperty(ENDPOINT_ID));
                processNotification(notification, getContext());
                break;

            case UNAVAILABLE:
                LOG.info("Notified - UNAVAILABLE: " + notified.getProvidedInterfaces()
                         + notified.getProvidedInterfaces() 
                         + " endpoint id: " 
                         + notification.getServiceEndpointDescription().getProperty(ENDPOINT_ID));
                unCacheEndpointId(notified);
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
