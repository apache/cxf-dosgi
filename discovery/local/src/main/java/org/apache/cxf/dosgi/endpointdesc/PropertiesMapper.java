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
package org.apache.cxf.dosgi.endpointdesc;

import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;
import org.osgi.xmlns.rsa.v1_0.ArrayType;
import org.osgi.xmlns.rsa.v1_0.ObjectFactory;
import org.osgi.xmlns.rsa.v1_0.PropertyType;
import org.osgi.xmlns.rsa.v1_0.ValueType;
import org.osgi.xmlns.rsa.v1_0.XmlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesMapper {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesMapper.class);

    public Map<String, Object> toProps(List<PropertyType> properties) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (PropertyType prop : properties) {
            map.put(prop.getName(), getValue(prop));
        }
        return map;
    }

    private Object getValue(PropertyType prop) {
        Object value = null;
        String type = getTypeName(prop);
        Object content = getFirstNonText(prop.getContent());
        if (content instanceof JAXBElement<?>) {
            JAXBElement<?> el = (JAXBElement<?>)content;
            if (el.getDeclaredType() == ArrayType.class) {
                String elName = el.getName().getLocalPart();
                ArrayType inValue = (ArrayType)el.getValue();
                if ("array".equals(elName)) {
                    value = getArray(inValue, type);
                } else if ("set".equals(elName)) {
                    value = handleCollection(inValue, new HashSet<Object>(), type);
                } else if ("list".equals(elName)) {
                    value = handleCollection(inValue, new ArrayList<Object>(), type);
                }
            } else if (el.getDeclaredType() == XmlType.class) {
                value = readXML((XmlType)el.getValue(), type);
            }
        } else {
            if (prop.getValue() != null) {
                value = instantiate(type, prop.getValue());
            } else {
                if (prop.getContent().size() > 0) {
                    value = instantiate(type, prop.getContent().get(0).toString());
                }
            }
        }
        return value;
    }

    private Object getFirstNonText(List<Serializable> contentList) {
        for (Object content : contentList) {
            if (content instanceof JAXBElement<?>) {
                return content;
            }
        }
        return null;
    }

    private static String getTypeName(PropertyType prop) {
        String type = prop.getValueType();
        return type == null ? "String" : type;
    }

    private Object getArray(ArrayType arrayEl, String type) {
        List<ValueType> values = arrayEl.getValue();
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

            for (int i = 0; i < values.size(); i++) {
                Object val = getValue(values.get(i), type);
                Array.set(array, i, val);
            }

            return array;
        } catch (Exception e) {
            LOG.warn("Could not create array for Endpoint Description", e);
            return null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Collection handleCollection(ArrayType el, Collection value, String type) {
        List<ValueType> values = el.getValue();
        for (ValueType val : values) {
            Object obj = getValue(val, type);
            value.add(obj);
        }
        return value;
    }
    
    private Object getValue(ValueType value, String type) {
        if (value.getContent().size() == 1 && value.getContent().get(0) instanceof String) {
            return handleValue((String)value.getContent().get(0), type);
        }
        JAXBElement<?> valueContent = (JAXBElement<?>)getFirstNonText(value.getContent());
        if (valueContent.getDeclaredType() == XmlType.class) {
            return readXML((XmlType)valueContent.getValue(), type);
        }
        return "";
    }

    private String readXML(XmlType el, String type) {
        if (el == null) {
            return null;
        }
        if (!"String".equals(type)) {
            LOG.warn("Embedded XML must be of type String, found: " + type);
            return null;
        }
        Node xmlContent = (Node)el.getAny();
        xmlContent.normalize();
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            StringWriter buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(xmlContent), new StreamResult(buffer));
            return buffer.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static Object handleValue(String val, String type) {
        return instantiate(type, val);
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
            if ("Character".equals(boxedType)) {
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
    
    public List<PropertyType> fromProps(Map<String, Object> m) {
        List<PropertyType> props = new ArrayList<PropertyType>();
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            PropertyType propEl = new PropertyType();
            propEl.setName(key);
            ObjectFactory factory = new ObjectFactory();
            if (val.getClass().isArray()) {
                ArrayType arrayEl = new ArrayType();
                propEl.getContent().add(factory.createArray(arrayEl));
                for (Object o : normalizeArray(val)) {
                    setValueType(propEl, o);
                    ValueType valueType =  new ValueType();
                    valueType.getContent().add(o.toString());
                    arrayEl.getValue().add(valueType);
                }
            } else if (val instanceof List) {
                ArrayType listEl = new ArrayType();
                propEl.getContent().add(factory.createList(listEl));
                handleCollectionValue((Collection<?>) val, propEl, listEl);
            } else if (val instanceof Set) {
                ArrayType setEl = new ArrayType();
                propEl.getContent().add(factory.createSet(setEl));
                handleCollectionValue((Collection<?>) val, propEl, setEl);
            } else if (val instanceof String
                    || val instanceof Character
                    || val instanceof Boolean
                    || val instanceof Byte) {
                setValueType(propEl, val);
                propEl.setValue(val.toString());
            } else if (val instanceof Long
                    || val instanceof Double
                    || val instanceof Float
                    || val instanceof Integer
                    || val instanceof Short) {
                // various numbers..   maybe "val instanceof Number"?
                setValueType(propEl, val);
                propEl.setValue(val.toString());
            } else {
                // Don't add this property as the value type is not supported
                continue;
            }
            props.add(propEl);
        }
        return props;
    }

    private static Object[] normalizeArray(Object val) {
        List<Object> l = new ArrayList<Object>();
        if (val instanceof int[]) {
            int[] ia = (int[]) val;
            for (int i : ia) {
                l.add(i);
            }
        } else if (val instanceof long[]) {
            long[] la = (long[]) val;
            for (long i : la) {
                l.add(i);
            }
        } else if (val instanceof float[]) {
            float[] fa = (float[]) val;
            for (float f : fa) {
                l.add(f);
            }
        } else if (val instanceof byte[]) {
            byte[] ba = (byte[]) val;
            for (byte b : ba) {
                l.add(b);
            }
        } else if (val instanceof boolean[]) {
            boolean[] ba = (boolean[]) val;
            for (boolean b : ba) {
                l.add(b);
            }
        } else if (val instanceof short[]) {
            short[] sa = (short[]) val;
            for (short s : sa) {
                l.add(s);
            }
        } else if (val instanceof char[]) {
            char[] ca = (char[]) val;
            for (char c : ca) {
                l.add(c);
            }
        } else {
            return (Object[]) val;
        }
        return l.toArray();
    }

    private static void handleCollectionValue(Collection<?> val, PropertyType propEl, ArrayType listEl) {
        for (Object o : val) {
            setValueType(propEl, o);
            ValueType valueType = new ValueType();
            valueType.getContent().add(o.toString());
            listEl.getValue().add(valueType);
        }
    }

    private static void setValueType(PropertyType propEl, Object val) {
        if (val instanceof String) {
            return;
        }

        String dataType = val.getClass().getName();
        if (dataType.startsWith("java.lang.")) {
            dataType = dataType.substring("java.lang.".length());
        }
        propEl.setValueType(dataType);
    }
}
