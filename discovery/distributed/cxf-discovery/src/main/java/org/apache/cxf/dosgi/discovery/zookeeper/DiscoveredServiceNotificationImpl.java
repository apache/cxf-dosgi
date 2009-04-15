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

import java.util.Collection;

import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.ServiceEndpointDescription;

@SuppressWarnings("unchecked") 
class DiscoveredServiceNotificationImpl implements DiscoveredServiceNotification {
    private final Collection filters;
    private final Collection interfaces;
    private final int type;
    private final ServiceEndpointDescription sed;    
    
    DiscoveredServiceNotificationImpl(Collection filters, Collection interfaces, 
        int type, ServiceEndpointDescription sed) {
        this.filters = filters;
        this.interfaces = interfaces;
        this.type = type;
        this.sed = sed;
    }

    public Collection getFilters() {
        return filters;
    }

    public Collection getInterfaces() {
        return interfaces;
    }

    public ServiceEndpointDescription getServiceEndpointDescription() {
        return sed;
    }

    public int getType() {
        return type;
    }        
}