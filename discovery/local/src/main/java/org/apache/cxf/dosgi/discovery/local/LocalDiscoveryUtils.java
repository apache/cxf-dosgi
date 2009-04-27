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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.osgi.framework.Bundle;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.discovery.ServicePublication;


public final class LocalDiscoveryUtils {
    private static final Logger LOG = 
        Logger.getLogger(LocalDiscoveryUtils.class.getName());        
    
    private static final String REMOTE_SERVICES_HEADER_NAME = "Remote-Service";
    private static final String REMOTE_SERVICES_DIRECTORY =
        "OSGI-INF/remote-service";
    private static final String REMOTE_SERVICES_NS =
        "http://www.osgi.org/xmlns/sd/v1.0.0";
    
    
    private static final String SERVICE_DESCRIPTION_ELEMENT = "service-description";
    
    private static final String PROVIDE_INTERFACE_ELEMENT = "provide";
    private static final String PROVIDE_INTERFACE_NAME_ATTRIBUTE = "interface";

    private static final String PROPERTY_ELEMENT = "property";
    private static final String PROPERTY_NAME_ATTRIBUTE = "name";
    private static final String PROPERTY_VALUE_ATTRIBUTE = "value";
    private static final String PROPERTY_INTERFACE_ATTRIBUTE = "interface";

    private static final String INTERFACE_SEPARATOR = ":";
    
    static boolean addEndpointID = true; // for testing
    
    private LocalDiscoveryUtils() {
    }
    
    @SuppressWarnings("unchecked")
    public static List<ServiceEndpointDescription> getAllRemoteReferences(Bundle b) {
        List<Element> references = getAllDescriptionElements(b);
        
        List<ServiceEndpointDescription> srefs = new ArrayList<ServiceEndpointDescription>();
        Namespace ns = Namespace.getNamespace(REMOTE_SERVICES_NS);
        for (Element ref : references) {
            List<String> iNames = getProvidedInterfaces(ref.getChildren(PROVIDE_INTERFACE_ELEMENT, ns));
            Map<String, Object> remoteProps = getProperties(ref.getChildren(PROPERTY_ELEMENT, ns));
            if (addEndpointID) {
                remoteProps.put(ServicePublication.ENDPOINT_ID, UUID.randomUUID().toString());
            }
            srefs.add(new ServiceEndpointDescriptionImpl(iNames, remoteProps));
        }
        return srefs;
        
    }
    
    @SuppressWarnings("unchecked")
    static List<Element> getAllDescriptionElements(Bundle b) {
        Object directory = null;
        
        Dictionary headers = b.getHeaders();
        if (headers != null) {
            directory = headers.get(REMOTE_SERVICES_HEADER_NAME);
        }
        
        if (directory == null) {
            directory = REMOTE_SERVICES_DIRECTORY;
        }
        
        Enumeration urls = b.findEntries(directory.toString(), "*.xml", false);
        if (urls == null) {
            return Collections.emptyList();
        }

        List<Element> elements = new ArrayList<Element>();
        while (urls.hasMoreElements()) {
            URL resourceURL = (URL) urls.nextElement();
            try {
                Document d = new SAXBuilder().build(resourceURL.openStream());
                Namespace ns = Namespace.getNamespace(REMOTE_SERVICES_NS);
                elements.addAll(d.getRootElement().getChildren(SERVICE_DESCRIPTION_ELEMENT, ns));
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Problem parsing: " + resourceURL, ex);
            }            
        }
        return elements; 
    }
    
    private static Map<String, Object> getProperties(List<Element> elements) {       
        Map<String, Object> props = new HashMap<String, Object>();
        
        for (Element p : elements) {
            String key = p.getAttributeValue(PROPERTY_NAME_ATTRIBUTE);
            String value = p.getAttributeValue(PROPERTY_VALUE_ATTRIBUTE);
            if (value == null) {
                value = p.getTextTrim();
            }
            
            String iface = p.getAttributeValue(PROPERTY_INTERFACE_ATTRIBUTE);
            if (key != null) {
                props.put(iface == null || iface.length() == 0
                          ? key
                          : key + INTERFACE_SEPARATOR + iface,
                          value);
            }
        }
        
        return props;
    }
    
    private static List<String> getProvidedInterfaces(List<Element> elements) {        
        List<String> names = new ArrayList<String>();
        
        for (Element p : elements) {
            String name = p.getAttributeValue(PROVIDE_INTERFACE_NAME_ATTRIBUTE);
            if (name != null) {
                names.add(name);
            }
        }
        
        return names;
    } 
}
