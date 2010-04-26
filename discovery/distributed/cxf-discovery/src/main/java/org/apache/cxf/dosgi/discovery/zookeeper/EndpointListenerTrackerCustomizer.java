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
package org.apache.cxf.dosgi.discovery.zookeeper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class EndpointListenerTrackerCustomizer implements ServiceTrackerCustomizer {

    private static final Logger LOG = Logger.getLogger(EndpointListenerTrackerCustomizer.class.getName());
    private ZooKeeperDiscovery zooKeeperDiscovery;
    private static final Pattern OBJECTCLASS_PATTERN = Pattern.compile(".*\\(objectClass=([^)]+)\\).*");

    private Map<String /* scope */, Interest> interestingScopes = new HashMap<String, Interest>();
    private Map<ServiceReference, List<String> /* scopes of the epl */> handledEndpointlisteners = new HashMap<ServiceReference, List<String>>();

    private BundleContext bctx;

    protected static class Interest {
        List<ServiceReference> relatedServiceListeners = new ArrayList<ServiceReference>(1);
        InterfaceMonitor im;
    }

    public EndpointListenerTrackerCustomizer(ZooKeeperDiscovery zooKeeperDiscovery, BundleContext bc) {
        this.zooKeeperDiscovery = zooKeeperDiscovery;
        bctx = bc;
    }

    public Object addingService(ServiceReference sref) {
        LOG.fine("addingService: " + sref);
        handleEndpointListener(sref);
        return sref;
    }

    public void modifiedService(ServiceReference sref, Object service) {
        LOG.fine("modifiedService: " + sref);
        handleEndpointListener(sref);
    }

    private void handleEndpointListener(ServiceReference sref) {
        for (String key : sref.getPropertyKeys()) {
            LOG.finest("modifiedService: property: " + key + " => " + sref.getProperty(key));
        }

        String[] scopes = Util.getScopes(sref);
        
        LOG.info("trying to discover services for scopes[" + scopes.length + "]: ");
        if(scopes!=null) for (String scope : scopes) {
            LOG.info("Scope: "+scope);
        }
        if (scopes.length > 0) {
            for (String scope : scopes) {
                LOG.fine("***********  Handling scope: " + scope);
                if("".equals(scope) || scope == null){
                    LOG.warning("skipping empty scope from EndpointListener from " + sref.getBundle().getSymbolicName());
                    continue;
                }
                
                String objClass = getObjectClass(scope);
                LOG.fine("***********  objectClass: " + objClass);

                synchronized (interestingScopes) {
                    synchronized (handledEndpointlisteners) {
                        Interest interest = interestingScopes.get(scope);
                        if (interest == null) {
                            interest = new Interest();
                            interestingScopes.put(scope, interest);
                        }

                        
                        if (!interest.relatedServiceListeners.contains(sref)) {
                            interest.relatedServiceListeners.add(sref);
                        }

                        if (interest.im != null) {
                            // close old Monitor
                            interest.im.close();
                            interest.im = null;
                        }
                        
                        InterfaceMonitor dm = new InterfaceMonitor(zooKeeperDiscovery.getZookeeper(),
                                                                   objClass, interest, scope, bctx);
                        dm.start();
                        interest.im = dm;

                        List<String> handledScopes = handledEndpointlisteners.get(sref);
                        if (handledScopes == null) {
                            handledScopes = new ArrayList<String>(1);
                            handledEndpointlisteners.put(sref, handledScopes);
                        }

                        if (!handledScopes.contains(scope))
                            handledScopes.add(scope);

                    }
                }

            }
        }
    }

    private String getObjectClass(String scope) {
        Matcher m = OBJECTCLASS_PATTERN.matcher(scope);
        if (m.matches())
            return m.group(1);
        return null;
    }

    public void removedService(ServiceReference sref, Object service) {
        LOG.info("removedService: " + sref);

        List<String> handledScopes = handledEndpointlisteners.get(sref);
        if (handledScopes != null) {
            for (String scope : handledScopes) {
                Interest i = interestingScopes.get(scope);
                if (i != null) {
                    i.relatedServiceListeners.remove(sref);
                    if (i.relatedServiceListeners.size() == 0) {
                        i.im.close();
                        interestingScopes.remove(scope);
                    }
                }
            }
            handledEndpointlisteners.remove(sref);
        }

    }

    

//    public void discoveredEndpont(EndpointDescription epd) {
//        LOG.info("Endpoint Discovered: " + epd.getProperties());
//    }
}
