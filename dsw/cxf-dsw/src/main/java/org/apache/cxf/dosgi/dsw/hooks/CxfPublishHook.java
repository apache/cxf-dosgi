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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.apache.cxf.endpoint.Server;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.discovery.ServiceEndpointDescription;

public class CxfPublishHook extends AbstractHook {
    
    protected Map<ServiceReference, List<EndpointInfo>> endpoints =
        new LinkedHashMap<ServiceReference, List<EndpointInfo>>();
    private Map<ServiceReference, ServiceRegistration> publications =
        new LinkedHashMap<ServiceReference, ServiceRegistration>();
    
    public CxfPublishHook(BundleContext bc, CxfDistributionProvider dpService) {
        super(bc, dpService);
    }
        
    public void publishEndpoint(ServiceReference sref) {
        synchronized (endpoints) {
            if (endpoints.containsKey(sref)) {
                return;
            }
        }
        
        if (ServiceHookUtils.isCreatedByDsw(sref)) {
            return;
        }
        
        ServiceEndpointDescription sd = null;
        if (checkBundle()) {
            sd = OsgiUtils.getRemoteReference(sref, true);
        } 
        // TODO 
        // update DSW to check 'local' remote-services metadata either 
        // by accepting Configuration Admin Service updates or checking 
        // local configuration data, if any available - it will let
        // this code to check for SDs from the additional source

        String[] publishableInterfaces = 
            sd != null
            ? OsgiUtils.getPublishableInterfaces(sd, sref)
            : null; 
        if (publishableInterfaces == null || publishableInterfaces.length == 0) {
            return;
        }

        ServiceEndpointDescription[] flatList = 
            OsgiUtils.flattenServiceDescription(sd);
        for (int i = 0; i < publishableInterfaces.length; i++) {    
            boolean isPublished = false;
            Server server = createServer(sref, flatList[i]);
            if (server != null) {
                ServiceRegistration publication = 
                    ServiceHookUtils.publish(getContext(), sref, flatList[i]);
                publications.put(sref, publication);
                isPublished = publication != null;
            }
        
            synchronized(endpoints) {
                EndpointInfo ei = new EndpointInfo(getContext(),
                                                   flatList[i],
                                                   server,
                                                   isPublished);
                if (endpoints.containsKey(sref)) {
                    endpoints.get(sref).add(ei);
                } else {
                    List<EndpointInfo> endpointList = 
                        new ArrayList<EndpointInfo>();
                    endpointList.add(ei);
                    endpoints.put(sref, endpointList);
                }
            }
        }
    }

    Server createServer(ServiceReference sref, ServiceEndpointDescription sd) {
        return ServiceHookUtils.createServer(
             getHandler(sd, getHandlerProperties()), sref, getContext(), 
             sref.getBundle().getBundleContext(), sd, getContext().getService(sref));
    }
    
    public void removeEndpoint(ServiceReference sref) {
        List<EndpointInfo> endpointList = null;
        synchronized(endpoints) {
            endpointList = endpoints.remove(sref);
        }
        if (endpointList != null) {
            for (EndpointInfo ei : endpointList) {
                ServiceHookUtils.unregisterServer(publications.get(sref), ei);
            }
        }
    }
    
    public void removeEndpoints() {
        synchronized(endpoints) {
            for (ServiceReference sref : endpoints.keySet()) {
                List<EndpointInfo> endpointList = endpoints.get(sref);
                for (EndpointInfo ei : endpointList) {
                    ServiceHookUtils.unregisterServer(publications.get(sref), 
                                                      ei);
                }
            }
            endpoints.clear();
        }
        
    }
    
    public Map<ServiceReference, List<EndpointInfo>> getEndpoints() {
        return Collections.unmodifiableMap(endpoints);
    }
    
    protected ConfigurationTypeHandler getHandler(ServiceEndpointDescription sd,
                                                  Map<String, Object> props) {
        return ServiceHookUtils.getHandler(getContext(), sd, getDistributionProvider(), props);
    }
}
