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
package org.apache.cxf.dosgi.dsw.service;

import static org.osgi.service.discovery.ServicePublication.ENDPOINT_LOCATION;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.osgi.service.discovery.ServiceEndpointDescription;

public class ServiceEndpointDescriptionImpl implements ServiceEndpointDescription {

    private static final Logger LOG = 
        Logger.getLogger(ServiceEndpointDescriptionImpl.class.getName());
    
    private Set<String> interfaceNames; 
    private Map<String, Object> properties;
    
    @SuppressWarnings("unchecked")
    public ServiceEndpointDescriptionImpl(String interfaceName) {
        this(Collections.singletonList(interfaceName), Collections.EMPTY_MAP);
    }
    
    public ServiceEndpointDescriptionImpl(String interfaceName,
                                  Map<String, Object> remoteProperties) {
        this(Collections.singletonList(interfaceName), remoteProperties);
    }

    @SuppressWarnings("unchecked")
    public ServiceEndpointDescriptionImpl(List<String> interfaceNames) {
        this(interfaceNames, Collections.EMPTY_MAP);
    }
    
    public ServiceEndpointDescriptionImpl(List<String> interfaceNames,
                                  Map<String, Object> remoteProperties) {
        this.interfaceNames = new HashSet<String>(interfaceNames);
        properties = remoteProperties;
    }
    
    public Object getProperty(String key) {
        return properties.get(key);

    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @SuppressWarnings("unchecked")
    public Collection getPropertyKeys() {
        return getProperties().keySet();
    }
    
    @Override
    public int hashCode() {
        return interfaceNames.hashCode() + 37 * properties.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceEndpointDescription)) {
            return false;
        }
        ServiceEndpointDescription other = (ServiceEndpointDescription)obj;
        if (this == other) {
            return true;
        }
        
        
        return interfaceNames.equals(other.getProvidedInterfaces())
               && properties.equals(other.getProperties());
    }

    public URI getLocation() {
        Object value = properties.get(ENDPOINT_LOCATION);
        if (value == null) {
            return null;
        }
        
        try {
            return new URI(value.toString());
        } catch (URISyntaxException ex) {
            LOG.warning("Service document URL is malformed : " + value.toString());
        }
        
        return null;
    }

    public Collection<String> getProvidedInterfaces() {
        return interfaceNames;
    }

    public String getVersion(String interfaceName) {
        return "0.0";
    }

    public String getEndpointInterfaceName(String interfaceName) {
        return interfaceName;
    }

    public String getEndpointID() {
        return null;
    }
}
