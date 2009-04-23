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
package org.apache.cxf.dosgi.dsw;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.dosgi.dsw.service.ServiceEndpointDescriptionImpl;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.distribution.DistributionConstants;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;


public final class OsgiUtils {

    private static final Logger LOG = 
        Logger.getLogger(OsgiUtils.class.getName());    
    
    private static final String REMOTE_SERVICES_HEADER_NAME = "Remote-Service";
    private static final String REMOTE_SERVICES_DIRECTORY =
        "OSGI-INF/remote-service";
    private static final String REMOTE_SERVICES_NS =
        "http://www.osgi.org/xmlns/sd/v1.0.0";
    
    static final String[] INTENT_MAP = {"/OSGI-INF/cxf/intents/intent-map.xml"};
    
    private static final String SERVICE_DESCRIPTION_ELEMENT = "service-description";
    
    private static final String PROVIDE_INTERFACE_ELEMENT = "provide";
    private static final String PROVIDE_INTERFACE_NAME_ATTRIBUTE = "interface";

    private static final String PROPERTY_ELEMENT = "property";
    private static final String PROPERTY_NAME_ATTRIBUTE = "name";
    private static final String PROPERTY_VALUE_ATTRIBUTE = "value";
    private static final String PROPERTY_INTERFACE_ATTRIBUTE = "interface";

    private static final String INTERFACE_WILDCARD = "*";
    private static final String INTERFACE_SEPARATOR = ":";
    
    private OsgiUtils() {
    }

    
    // Used by PublishHook 
    public static ServiceEndpointDescription getRemoteReference(ServiceReference sref, boolean matchAllNames) {
        
        String[] names = (String[])sref.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
        if (names == null || names.length == 0) {
            return null;
        }
        
        Map<String, Object> userProperties = new HashMap<String, Object>();
        for (String key : sref.getPropertyKeys()) {
            // we're after remote properties only
            if (key.startsWith(DistributionConstants.PROP_KEY_REMOTE_SERVICE)) {
                userProperties.put(key, sref.getProperty(key));
            }
        }
        List<ServiceEndpointDescription> srefs = getRemoteReferences(sref.getBundle(), 
                                                             names, 
                                                             userProperties,
                                                             matchAllNames);
        
        if (srefs.isEmpty()) {
            return new ServiceEndpointDescriptionImpl(Arrays.asList(names), userProperties);
        }
        
        return srefs.get(0); 
    }
    
    @SuppressWarnings("unchecked")
    public static List<ServiceEndpointDescription> getRemoteReferences(
        Bundle b, String[] names, Map<String, Object> userProperties, boolean matchAllNames) {
        
        List<Element> references = getAllDescriptionElements(b);
        
        List<ServiceEndpointDescription> srefs = new ArrayList<ServiceEndpointDescription>();
        Namespace ns = Namespace.getNamespace(REMOTE_SERVICES_NS);
        for (Element ref : references) {
            List<String> iNames = getProvidedInterfaces(ref.getChildren(PROVIDE_INTERFACE_ELEMENT, ns));
            if (!serviceNamesMatch(names, iNames, matchAllNames)) {
                continue;
            }

            Map<String, Object> remoteProps = getProperties(ref.getChildren(PROPERTY_ELEMENT, ns));
            remoteProps.putAll(userProperties);
            srefs.add(new ServiceEndpointDescriptionImpl(iNames, remoteProps));
            
        }
        return srefs;
        
    }
    
    @SuppressWarnings("unchecked")
    public static List<ServiceEndpointDescription> getAllRemoteReferences(Bundle b) {
        List<Element> references = getAllDescriptionElements(b);
        
        List<ServiceEndpointDescription> srefs = new ArrayList<ServiceEndpointDescription>();
        Namespace ns = Namespace.getNamespace(REMOTE_SERVICES_NS);
        for (Element ref : references) {
            List<String> iNames = getProvidedInterfaces(ref.getChildren(PROVIDE_INTERFACE_ELEMENT, ns));
            Map<String, Object> remoteProps = getProperties(ref.getChildren(PROPERTY_ELEMENT, ns));
            srefs.add(new ServiceEndpointDescriptionImpl(iNames, remoteProps));
        }
        return srefs;
        
    }

    public static ServiceEndpointDescription[] flattenServiceDescription(ServiceEndpointDescription sd) {
        ServiceEndpointDescription[] list = null;
        int interfaceNameCount = sd.getProvidedInterfaces().size();
        if (sd.getProvidedInterfaces() == null || interfaceNameCount <= 1) {
            list = new ServiceEndpointDescription[] {sd};
        } else {
            String[] iNames = (String[])
                sd.getProvidedInterfaces().toArray(new String[interfaceNameCount]);
            list = new ServiceEndpointDescription[iNames.length];
            for (int i = 0; i < iNames.length; i++) {
                Map<String, Object> props = excludeProperty(sd.getProperties(),  
                        DistributionConstants.PROP_KEY_SERVICE_REMOTE_INTERFACES);
                
                String keys[] = props.keySet().toArray(new String[props.size()]);
                for (int j = 0; j < keys.length; j++) {
                    int sep = keys[j].indexOf(INTERFACE_SEPARATOR);
                    if (sep > -1) {
                        String value = (String)props.remove(keys[j]);
                        String root = keys[j].substring(0, sep);
                        String iface = 
                          sep + INTERFACE_SEPARATOR.length() < keys[j].length()
                          ? keys[j].substring(sep + INTERFACE_SEPARATOR.length())
                          : "";
                        if (iNames[i].equals(iface)) {
                            props.put(root, value);
                        }
                    }
                }
                list[i] = new ServiceEndpointDescriptionImpl(iNames[i], props);
            }
        }
        return list;
    }

    private static Map<String, Object> excludeProperty(Map properties, 
                                                       String exclude) {
        Map<String, Object> pruned = new HashMap<String, Object>();
        for (Object key : properties.keySet()) {
            if (key.equals(exclude)) {
            } else {
                pruned.put((String)key, properties.get(key));
            }
        }
        return pruned;
    }
    
    @SuppressWarnings("unchecked")
    public static List<Element> getAllDescriptionElements(Bundle b) {
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
    
    private static boolean serviceNamesMatch(
        String[] names, List<String> iNames, boolean matchAllNames) {
        if (names == null || names.length == 0) {
            return false;
        }
        if (matchAllNames) {
            for (String name : names) {
                if (!iNames.contains(name)) {
                    return false;
                }
            }
            return true;
        } else {
            for (String name : names) {
                if (iNames.contains(name)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    // TODO : consider creating a new List rather than modifyiing the existing one
    public static void matchServiceDescriptions(List<ServiceEndpointDescription> sds,
                                                String interfaceName,
                                                Filter filter, 
                                                boolean matchAll) {
        
        for (Iterator<ServiceEndpointDescription> it = sds.iterator(); it.hasNext();) {
            ServiceEndpointDescription sd = it.next();
             
            if (filter != null && !OsgiUtils.matchAgainstFilter(sd, interfaceName, filter, matchAll)) {
                it.remove();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static boolean matchAgainstFilter(ServiceEndpointDescription sd, 
        String interfaceName, Filter filter, boolean matchAll) {
        Dictionary props = new Hashtable();
        for (Object key : sd.getPropertyKeys()) {
            if (matchAll || key.toString().startsWith(DistributionConstants.PROP_KEY_REMOTE_SERVICE)) {
                props.put(key, sd.getProperty(key.toString()));
            }
        }
        String[] interfaceNames = getProvidedInterfaces(sd, interfaceName);
        if (interfaceNames != null) {
            props.put(org.osgi.framework.Constants.OBJECTCLASS, 
                      interfaceNames);
        }
        return filter.match(props);
    }
    
    public static String[] getProvidedInterfaces(ServiceEndpointDescription sd, String interfaceName) {
        
        int interfaceNameCount = sd.getProvidedInterfaces().size();
        String[] interfaceNames = (String[])
            sd.getProvidedInterfaces().toArray(new String[interfaceNameCount]);
        if (interfaceName == null) {
            return interfaceNames;
        }
        
        for (String s : interfaceNames) {
            if (s.equals(interfaceName)) {
                return new String[]{s};
            }
        }
        return null;
    }
    
    public static Filter createFilter(BundleContext bc, String filterValue) {
        
        if (filterValue == null) {
            return null;
        }
        
        try {
            return bc.createFilter(filterValue); 
        } catch (InvalidSyntaxException ex) {
            System.out.println("Invalid filter expression " + filterValue);
        } catch (Exception ex) {
            System.out.println("Problem creating a Filter from " + filterValue); 
        }
        return null;
    }
    
    public static String[] parseIntents(String intentsSequence) {
        return intentsSequence == null ? new String[]{} : intentsSequence.split(" ");
    }
    
    public static String formatIntents(String [] intents) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String intent : intents) {
            if (first) {
                first = false;
            } else {
                sb.append(' ');
            }
            sb.append(intent);
        }
        return sb.toString();
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
    
    @SuppressWarnings("unchecked")
    public static <T> OsgiService<T> getOsgiService(
        BundleContext bc, Class<T> serviceClass) {
        try {
            ServiceReference sr = bc.getServiceReference(serviceClass.getName());
            if (sr != null) {
                Object o = bc.getService(sr);
                if (o != null && serviceClass.isAssignableFrom(o.getClass())) {
                    return new OsgiService(sr, o);
                }
            }
        } catch (Exception ex) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Problem retrieving an OSGI service " + serviceClass.getName()
                + ", exception : " + ex.getMessage());
            }
        }
        return null;
    }
    
    public static IntentMap getIntentMap(BundleContext bundleContext) {
        IntentMap im = readIntentMap(bundleContext);
        if (im == null) {
            // Couldn't read an intent map
            LOG.log(Level.FINE, "Using default intent map");
            im = new IntentMap();
            im.setIntents(new HashMap<String, Object>());
        }
        return im;        
    }
    
    static IntentMap readIntentMap(BundleContext bundleContext) {
        List<String> springIntentLocations = new ArrayList<String>();
        for (String mapFile : INTENT_MAP) {
            if (bundleContext.getBundle().getResource(mapFile) == null) {
                LOG.info("Could not find intent map file " + mapFile);
                return null;
            }
            springIntentLocations.add("classpath:" + mapFile);
        }
        
        try {
            OsgiBundleXmlApplicationContext ctx = 
                new OsgiBundleXmlApplicationContext(springIntentLocations.toArray(new String [] {}));
            ctx.setPublishContextAsService(false);
            ctx.setBundleContext(bundleContext);
            ctx.refresh();
            LOG.fine("application context: " + ctx);
            IntentMap im = (IntentMap)ctx.getBean("intentMap");
            LOG.fine("retrieved intent map: " + im);
            return im;
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Intent map load failed: ", t);
            return null;
        }
    }
    
    //Eg. "(&(" + Constants.OBJECTCLASS + "=Person)(|(sn=Jensen)(cn=Babs J*)))"
    public static Filter createFilterFromProperties(BundleContext bc, 
                                             Dictionary properties) {
        
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("(&");
        for (Enumeration keys = properties.keys();keys.hasMoreElements();) {
            String key = keys.nextElement().toString();
            String value = properties.get(key).toString();
            sb.append('(').append(key).append('=').append(value).append(')');
        }
        sb.append(')');
        return createFilter(bc, sb.toString());
    }
    
    
    // TODO : move these property helpers into PropertyUtils ?
    
    public static boolean getBooleanProperty(ServiceEndpointDescription sd, String name) {
        Object value = sd.getProperty(name);
        return toBoolean(value);
    }
    
    public static boolean toBoolean(Object value) {
        return value instanceof Boolean && ((Boolean)value).booleanValue()
            || value instanceof String && Boolean.parseBoolean((String)value);
    }
    
    public static String getProperty(ServiceEndpointDescription sd, String name) { 
        return getProperty(sd, name, String.class, null); 
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T getProperty(ServiceEndpointDescription sd, String name, Class<T> type,
                             T defaultValue) { 
        Object o = sd.getProperty(name);
        if (o == null) {
            return defaultValue;
        }
        return type.isAssignableFrom(o.getClass()) ? (T)o : null;
    }

    public static String[] getPublishableInterfaces(ServiceEndpointDescription sd,
                                                    ServiceReference sref) {
        Object publishProperty = 
            sd.getProperty(DistributionConstants.PROP_KEY_SERVICE_REMOTE_INTERFACES);
        String[] actualInterfaces = 
            (String[])sref.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
        String[] publishableInterfaces = null;


        if (actualInterfaces != null
            && actualInterfaces.length > 0
            && publishProperty != null) {

            if (INTERFACE_WILDCARD.equals(publishProperty)) {
                // wildcard indicates all interfaces should be published
                //
                publishableInterfaces = actualInterfaces;
            } else {

                String[] requestedInterfaces =
                    publishProperty instanceof String
                    ? tokenize((String)publishProperty, ",")
                    : publishProperty instanceof String[]
                      ? (String[])publishProperty
                      : publishProperty instanceof Collection
                      ? (String[])((Collection)publishProperty).toArray(
                            new String[((Collection)publishProperty).size()])
                         : null;

                ArrayList<String> publishableList = new ArrayList<String>();

                for (int i = 0; requestedInterfaces != null 
                                && i < requestedInterfaces.length; i++) {
                    if (contains(actualInterfaces, requestedInterfaces[i])) {
                        publishableList.add(requestedInterfaces[i]);
                    } else {
                        // simply ignore non-exposed interfaces
                        //
                        LOG.warning("ignoring publish interface, " 
                                    + requestedInterfaces[i] 
                                    + ", not exposed by service");
                    }
                }

                if (publishableList.size() > 0) {
                    publishableInterfaces = 
                        publishableList.toArray(new String[publishableList.size()]);
                }
            }
        }

        return publishableInterfaces;
    }

    private static String[] tokenize(String str, String delim) {
        StringTokenizer tokenizer = new StringTokenizer(str, delim);
        String[] tokens = new String[tokenizer.countTokens()];
        for (int i = 0; tokenizer.hasMoreTokens(); i++) {
            tokens[i] = tokenizer.nextToken();
        }
        return tokens;
    }

    private static boolean contains(String[] list, String member) {
        boolean found = false;
        for (int i = 0; i < list.length && !found; i++) {
            found = member.equals(list[i]);
        }
        return found;
    }
}
