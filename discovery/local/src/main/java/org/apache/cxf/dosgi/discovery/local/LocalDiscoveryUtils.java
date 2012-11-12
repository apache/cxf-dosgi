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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.LoggerFactory;


public final class LocalDiscoveryUtils {
    private static final org.slf4j.Logger LOG =
        LoggerFactory.getLogger(LocalDiscoveryUtils.class);

    private static final String REMOTE_SERVICES_HEADER_NAME = "Remote-Service";
    private static final String REMOTE_SERVICES_DIRECTORY =
        "OSGI-INF/remote-service/";
    private static final String REMOTE_SERVICES_NS =
        "http://www.osgi.org/xmlns/sd/v1.0.0"; // this one was replaced by the RSA one in the spec
    private static final String REMOTE_SERVICES_ADMIN_NS =
        "http://www.osgi.org/xmlns/rsa/v1.0.0";


    private static final String SERVICE_DESCRIPTION_ELEMENT = "service-description";
    private static final String ENDPOINT_DESCRIPTION_ELEMENT = "endpoint-description";

    private static final String PROVIDE_INTERFACE_ELEMENT = "provide";
    private static final String PROVIDE_INTERFACE_NAME_ATTRIBUTE = "interface";

    private static final String PROPERTY_ELEMENT = "property";
    private static final String PROPERTY_NAME_ATTRIBUTE = "name";
    private static final String PROPERTY_VALUE_ATTRIBUTE = "value";
    private static final String PROPERTY_VALUE_TYPE_ATTRIBUTE = "value-type";
    private static final String PROPERTY_INTERFACE_ATTRIBUTE = "interface";

    private static final String INTERFACE_SEPARATOR = ":";

    static boolean addEndpointID = true; // for testing

    private LocalDiscoveryUtils() {}

    public static List<EndpointDescription> getAllEndpointDescriptions(Bundle b) {
        List<Element> elements = getAllDescriptionElements(b);

        List<EndpointDescription> eds = new ArrayList<EndpointDescription>(elements.size());
        for (Element el : elements) {
            if (ENDPOINT_DESCRIPTION_ELEMENT.equals(el.getName())) {
                eds.add(getEndpointDescription(el));
            } else if (SERVICE_DESCRIPTION_ELEMENT.equals(el.getName())) {
                eds.add(getLegacyEndpointDescription(el));
            }
        }
        return eds;
    }

    @SuppressWarnings("unchecked")
    public static EndpointDescription getEndpointDescription(Element endpointDescriptionElement) {
        Map<String, Object> map = new HashMap<String, Object>();

        List<Element> properties = endpointDescriptionElement.getChildren(PROPERTY_ELEMENT,
                Namespace.getNamespace(REMOTE_SERVICES_ADMIN_NS));
        for (Element prop : properties) {
            boolean handled = handleArray(prop, map);
            if (handled) {
                continue;
            }
            handled = handleCollection(prop, map);
            if (handled) {
                continue;
            }
            handled = handleXML(prop, map);
            if (handled) {
                continue;
            }

            String name = prop.getAttributeValue(PROPERTY_NAME_ATTRIBUTE);
            String value = prop.getAttributeValue(PROPERTY_VALUE_ATTRIBUTE);
            if (value == null) {
                value = prop.getText();
            }
            String type = getTypeName(prop);
            map.put(name, instantiate(type, value));
        }
        return new EndpointDescription(map);
    }

    @SuppressWarnings("unchecked")
    private static EndpointDescription getLegacyEndpointDescription(Element el) {
        Namespace ns = Namespace.getNamespace(REMOTE_SERVICES_NS);

        List<String> iNames = getProvidedInterfaces(el.getChildren(PROVIDE_INTERFACE_ELEMENT, ns));
        Map<String, Object> remoteProps = getProperties(el.getChildren(PROPERTY_ELEMENT, ns));

        if (remoteProps.get(Constants.OBJECTCLASS) == null) {
            remoteProps.put(Constants.OBJECTCLASS, iNames.toArray(new String[] {}));
        }

        Object uri = remoteProps.get("org.apache.cxf.ws.address");
        if (uri == null) {
            uri = remoteProps.get("osgi.remote.configuration.pojo.address");
        }
        if (uri == null) {
            String firstIntf = iNames.get(0);
            uri = "http://localhost:9000/" + firstIntf.replace('.', '/');
        }
        remoteProps.put(RemoteConstants.ENDPOINT_ID, uri.toString());

        Object exportedConfigs = remoteProps.get(RemoteConstants.SERVICE_EXPORTED_CONFIGS);
        if (exportedConfigs == null) {
            exportedConfigs = "org.apache.cxf.ws";
        }
        remoteProps.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, exportedConfigs);

        for(Iterator<String> it = remoteProps.keySet().iterator(); it.hasNext(); ) {
            if (it.next().startsWith("service.exported.")) {
                it.remove();
            }
        }

        return new EndpointDescription(remoteProps);
    }

    private static String getTypeName(Element prop) {
        String type = prop.getAttributeValue(PROPERTY_VALUE_TYPE_ATTRIBUTE);
        if (type == null) {
            type = "String";
        }
        return type;
    }

    @SuppressWarnings("unchecked")
    private static boolean handleArray(Element prop, Map<String, Object> map) {
        Element arrayEl = prop.getChild("array", Namespace.getNamespace(REMOTE_SERVICES_ADMIN_NS));
        if (arrayEl == null) {
            return false;
        }

        List<Element> values = arrayEl.getChildren(PROPERTY_VALUE_ATTRIBUTE,
                Namespace.getNamespace(REMOTE_SERVICES_ADMIN_NS));
        String type = getTypeName(prop);
        Class<?> cls = null;
        if ("long".equals(type)) {
            cls = long.class;
        } else if ("double".equals(type)) {
            cls = double.class;
        } else if ("float".equals(type)) {
            cls = float.class;
        } else if ("int".equals(type)) {
            cls = int.class;
        } else if ("byte".equals(type)) {
            cls = byte.class;
        } else if ("boolean".equals(type)) {
            cls = boolean.class;
        } else if ("short".equals(type)) {
            cls = short.class;
        }

        try {
            if (cls == null) {
                cls = ClassLoader.getSystemClassLoader().loadClass("java.lang." + type);
            }
            Object array = Array.newInstance(cls, values.size());

            for (int i=0; i < values.size(); i++) {
                Element vEl = values.get(i);
                Object val = handleValue(vEl, type);
                Array.set(array, i, val);
            }

            String name = prop.getAttributeValue(PROPERTY_NAME_ATTRIBUTE);
            map.put(name, array);
            return true;
        } catch (Exception e) {
            LOG.warn("Could not create array for Endpoint Description", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean handleCollection(Element prop, Map<String, Object> map) {
        Collection<Object> col = null;
        Element el = prop.getChild("list",
                Namespace.getNamespace(REMOTE_SERVICES_ADMIN_NS));
        if (el != null) {
            col = new ArrayList<Object>();
        } else {
            el = prop.getChild("set", Namespace.getNamespace(REMOTE_SERVICES_ADMIN_NS));
            if (el != null) {
                col = new HashSet<Object>();
            }
        }

        if (el == null) {
            return false;
        }

        String type = getTypeName(prop);
        List<Element> values = el.getChildren(PROPERTY_VALUE_ATTRIBUTE,
                Namespace.getNamespace(REMOTE_SERVICES_ADMIN_NS));
        for (Element val : values) {
            Object obj = handleValue(val, type);
            col.add(obj);
        }

        String name = prop.getAttributeValue(PROPERTY_NAME_ATTRIBUTE);
        map.put(name, col);
        return true;
    }

    private static boolean handleXML(Element prop, Map<String, Object> map) {
        String sb = readXML(prop);
        if (sb == null) {
            return false;
        }

        String name = prop.getAttributeValue(PROPERTY_NAME_ATTRIBUTE);
        map.put(name, sb);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static String readXML(Element prop) {
        Element el = prop.getChild("xml", Namespace.getNamespace(REMOTE_SERVICES_ADMIN_NS));
        if (el == null) {
            return null;
        }

        String type = getTypeName(prop);
        if (!"String".equals(type)) {
            LOG.warn("Embedded XML must be of type String, found: " + type);
            return null;
        }

        XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
        StringBuilder sb = new StringBuilder();
        List<Element> children = el.getChildren();
        for (Element child : children) {
            sb.append(outputter.outputString(child));
        }
        return sb.toString();
    }

    private static Object handleValue(Element val, String type) {
        String xml = readXML(val);
        if (xml != null) {
            return xml;
        } else {
            return instantiate(type, val.getText());
        }
    }

    private static Object instantiate(String type, String value) {
        if ("String".equals(type)) {
            return value;
        }

        value = value.trim();
        String boxedType = null;
        if ("long".equals(type)) {
            boxedType = "Long";
        } else if ("double".equals(type)) {
            boxedType = "Double";
        } else if ("float".equals(type)) {
            boxedType = "Float";
        } else if ("int".equals(type)) {
            boxedType = "Integer";
        } else if ("byte".equals(type)) {
            boxedType = "Byte";
        } else if ("char".equals(type)) {
            boxedType = "Character";
        } else if ("boolean".equals(type)) {
            boxedType = "Boolean";
        } else if ("short".equals(type)) {
            boxedType = "Short";
        }

        if (boxedType == null) {
            boxedType = type;
        }
        String javaType = "java.lang." + boxedType;

        try {
            if (boxedType.equals("Character")) {
                return new Character(value.charAt(0));
            } else {
                Class<?> cls = ClassLoader.getSystemClassLoader().loadClass(javaType);
                Constructor<?> ctor = cls.getConstructor(String.class);
                return ctor.newInstance(value);
            }
        } catch (Exception e) {
            LOG.warn("Could not create Endpoint Property of type " + type + " and value " + value);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static List<Element> getAllDescriptionElements(Bundle b) {
        Object header = null;

        Dictionary headers = b.getHeaders();
        if (headers != null) {
            header = headers.get(REMOTE_SERVICES_HEADER_NAME);
        }

        if (header == null) {
            header = REMOTE_SERVICES_DIRECTORY;
        }

        String dir = header.toString();
        String filePattern = "*.xml";
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length()-1);
        } else {
            int idx = dir.lastIndexOf('/');
            if (idx >= 0 & dir.length() > idx) {
                filePattern = dir.substring(idx + 1);
                dir = dir.substring(0, idx);
            } else {
                filePattern = dir;
                dir = "";
            }
        }

        Enumeration urls = b.findEntries(dir, filePattern, false);
        if (urls == null) {
            return Collections.emptyList();
        }

        List<Element> elements = new ArrayList<Element>();
        while (urls.hasMoreElements()) {
            URL resourceURL = (URL) urls.nextElement();
            try {
                elements.addAll(getElements(resourceURL.openStream()));
            } catch (Exception ex) {
                LOG.warn("Problem parsing: " + resourceURL, ex);
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

    public static List<String> getStringPlusProperty(ServiceReference sr, String key) {
        Object value = sr.getProperty(key);
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof String) {
            return Collections.singletonList((String) value);
        }

        if (value instanceof String[]) {
            String[] values = (String[]) value;
            List<String> result = new ArrayList<String>(values.length);
            for (String v : values) {
                if (v != null) {
                    result.add(v);
                }
            }
            return Collections.unmodifiableList(result);
        }

        if (value instanceof Collection<?>) {
            Collection<?> values = (Collection<?>) value;
            List<String> result = new ArrayList<String>(values.size());
            for (Iterator<?> iter = values.iterator(); iter.hasNext();) {
                Object v = iter.next();
                if ((v != null) && (v instanceof String)) {
                    result.add((String) v);
                }
            }
            return Collections.unmodifiableList(result);
        }

        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static String getEndpointDescriptionXML(Map m) {
        Document d = new Document();
        Namespace ns = Namespace.getNamespace("http://www.osgi.org/xmlns/rsa/v1.0.0");
        Element rootEl = new Element("endpoint-descriptions", ns);
        d.setRootElement(rootEl);
        Element contentEl = new Element("endpoint-description", ns);
        rootEl.addContent(contentEl);

        for (Map.Entry entry : (Set<Map.Entry>) m.entrySet()) {
            String key = entry.getKey().toString();
            Object val = entry.getValue();

            Element propEl = new Element("property", ns);
            propEl.setAttribute("name", key);
            if (val.getClass().isArray()) {
                Element arrayEl = new Element("array", ns);
                propEl.addContent(arrayEl);
                for (Object o : normalizeArray(val)) {
                    setValueType(propEl, o);
                    Element valueEl = new Element("value", ns);
                    arrayEl.addContent(valueEl);
                    valueEl.addContent(o.toString());
                }
            } else if (val instanceof List) {
                Element listEl = new Element("list", ns);
                propEl.addContent(listEl);
                handleCollectionValue(ns, (Collection) val, propEl, listEl);
            } else if (val instanceof Set) {
                Element setEl = new Element("set", ns);
                propEl.addContent(setEl);
                handleCollectionValue(ns, (Collection) val, propEl, setEl);
            } else if (val instanceof String
                    || val instanceof Long
                    || val instanceof Double
                    || val instanceof Float
                    || val instanceof Integer
                    || val instanceof Byte
                    || val instanceof Character
                    || val instanceof Boolean
                    || val instanceof Short) {
                setValueType(propEl, val);
                propEl.setAttribute("value", val.toString());
            } else {
                // Don't add this property as the value type is not supported
                continue;
            }
            contentEl.addContent(propEl);
        }

        return new XMLOutputter(Format.getPrettyFormat()).outputString(d);
    }

    @SuppressWarnings("unchecked")
    private static Object [] normalizeArray(Object val) {
        List l = new ArrayList();
        if (val instanceof int[]) {
            int[] ia = (int []) val;
            for (int i : ia) {
                l.add(i);
            }
        } else if (val instanceof long[]) {
            long[] la = (long []) val;
            for (long i : la) {
                l.add(i);
            }
        } else if (val instanceof float[]) {
            float[] fa = (float[]) val;
            for (float f : fa) {
                l.add(f);
            }
        } else if (val instanceof byte[]) {
            byte[] ba = (byte []) val;
            for (byte b : ba) {
                l.add(b);
            }
        } else if (val instanceof boolean[]) {
            boolean[] ba = (boolean []) val;
            for (boolean b : ba) {
                l.add(b);
            }
        } else if (val instanceof short[]) {
            short[] sa = (short []) val;
            for (short s : sa) {
                l.add(s);
            }
        } else if (val instanceof char[]) {
            char[] ca = (char []) val;
            for (char c : ca) {
                l.add(c);
            }
        } else {
            return (Object []) val;
        }
        return l.toArray();
    }

    @SuppressWarnings("unchecked")
    private static void handleCollectionValue(Namespace ns, Collection val, Element propEl, Element listEl) {
        for (Object o : val) {
            setValueType(propEl, o);
            Element valueEl = new Element("value", ns);
            listEl.addContent(valueEl);
            valueEl.addContent(o.toString());
        }
    }

    private static void setValueType(Element propEl, Object val) {
        if (val instanceof String) {
            return;
        }

        String dataType = val.getClass().getName();
        if (dataType.startsWith("java.lang.")) {
            dataType = dataType.substring("java.lang.".length());
        }
        propEl.setAttribute("value-type", dataType);
    }

    public static List<Element> getElements(InputStream in) throws JDOMException, IOException {

        List<Element> elements = new ArrayList<Element>();

        Document d = new SAXBuilder().build(in);
        if (d.getRootElement().getNamespaceURI().equals(REMOTE_SERVICES_ADMIN_NS)) {
            elements.addAll(d.getRootElement().getChildren(ENDPOINT_DESCRIPTION_ELEMENT,
                    Namespace.getNamespace(REMOTE_SERVICES_ADMIN_NS)));
        }

        Namespace nsOld = Namespace.getNamespace(REMOTE_SERVICES_NS);
        elements.addAll(d.getRootElement().getChildren(SERVICE_DESCRIPTION_ELEMENT, nsOld));

        return elements;
    }
}
