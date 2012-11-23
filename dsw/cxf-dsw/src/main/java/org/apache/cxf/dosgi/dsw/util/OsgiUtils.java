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
package org.apache.cxf.dosgi.dsw.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OsgiUtils {
    
    public static final Logger LOG = LoggerFactory.getLogger(OsgiUtils.class);

    private OsgiUtils() {
    }

    // TODO : move these property helpers into PropertyUtils ?

    @SuppressWarnings("rawtypes")
	public static boolean getBooleanProperty(Map sd, String name) {
        Object value = sd.get(name);
        return toBoolean(value);
    }

    public static boolean toBoolean(Object value) {
        return value instanceof Boolean && ((Boolean)value).booleanValue() || value instanceof String
               && Boolean.parseBoolean((String)value);
    }

    @SuppressWarnings("unchecked")
    public static Collection<String> getMultiValueProperty(Object property) {
        if (property == null) {
            return null;
        }

        if (property instanceof Collection) {
            return (Collection<String>)property;
        } else if (property instanceof String[]) {
            return Arrays.asList((String[])property);
        } else {
            return Collections.singleton(property.toString());
        }
    }

    public static String getProperty(EndpointDescription sd, String name) {
        Object o = sd.getProperties().get(name);

        if (o != null && o instanceof String) {
            return (String)o;
        }

        return null;

    }

    @SuppressWarnings("rawtypes")
	public static String getProperty(Map dict, String name) {
        Object o = dict.get(name);

        if (o != null && o instanceof String) {
            return (String)o;
        }
        return null;
    }
    
    public static String getFirstNonEmptyStringProperty(@SuppressWarnings("rawtypes") Map dict, String ... keys) {
        for (String key : keys) {
            String value = getStringProperty(dict, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
    
    @SuppressWarnings("rawtypes")
    private static String getStringProperty(Map dict, String name) {
        Object o = dict.get(name);

        if (o != null) {
            if (o instanceof String) {
                return (String)o;
            } else {
                throw new RuntimeException("Could not use property " + name + " as the value is not a String");
            }
        }
        return null;
    }

    /**
     * Tries to retrieve the version of iClass via the PackageAdmin
     * 
     * @param iClass - The interface for which the version should be found
     * @param bc - any valid BundleContext
     * @return the version of the interface or "0.0.0" if no version information could be found or an error
     *         occurred during the retrieval
     */
    public static String getVersion(Class<?> iClass, BundleContext bc) {

        ServiceReference paRef = bc.getServiceReference(PackageAdmin.class.getName());
        if (paRef != null) {
            PackageAdmin pa = (PackageAdmin)bc.getService(paRef);

            Bundle b = pa.getBundle(iClass);
            if (b == null) {
                LOG.info("Unable to find interface version for interface " + iClass.getName()
                        + ". Falling back to 0.0.0");
                return "0.0.0";
            }
            LOG.debug("Interface source bundle: {}", b.getSymbolicName());

            ExportedPackage[] ep = pa.getExportedPackages(b);
            LOG.debug("Exported Packages of the source bundle: {}", ep);

            String pack = iClass.getPackage().getName();
            LOG.debug("Looking for Package: {}", pack);
            if (ep != null) {
	            for (ExportedPackage p : ep) {
	            	if (p != null) {
		                if (pack.equals(p.getName())) {
		                    if (LOG.isDebugEnabled()) {
		                        LOG.debug("found package -> Version: {}", p.getVersion());
		                    }
		                    return p.getVersion().toString();
		                }
	            	}
	            }
            }
        } else {
            LOG.error("Was unable to obtain the package admin service -> can't resolve interface versions");
        }

        LOG.info("Unable to find interface version for interface " + iClass.getName()
                 + ". Falling back to 0.0.0");
        return "0.0.0";
    }

    public static String getUUID(BundleContext bc) {
        synchronized ("org.osgi.framework.uuid") {
            String uuid = bc.getProperty("org.osgi.framework.uuid");
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                System.setProperty("org.osgi.framework.uuid", uuid);
            }
            return uuid;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void overlayProperties(Properties serviceProperties, Map additionalProperties) {
        Enumeration<Object> keys = serviceProperties.keys();
        // Maps lower case key to original key
        HashMap<String,String> keysLowerCase = new HashMap<String, String>();
        while(keys.hasMoreElements()){
            Object o = keys.nextElement(); 
            if (o instanceof String) {
                String ks = (String)o;
                keysLowerCase.put(ks.toLowerCase(), ks);
            }
        }
        
        Set<Map.Entry> adProps = additionalProperties.entrySet();
        for (Map.Entry e : adProps) {
            // objectClass and service.id must not be overwritten
            Object keyObj = e.getKey();
            if (keyObj instanceof String && keyObj != null) {
                String key = ((String)keyObj).toLowerCase();
                if (org.osgi.framework.Constants.SERVICE_ID.toLowerCase().equals(key)
                    || org.osgi.framework.Constants.OBJECTCLASS.toLowerCase().equals(key)) {
                    LOG.info("exportService called with additional properties map that contained illegal key: "
                              + key + "   The key is ignored");
                    continue;
                }else if(keysLowerCase.containsKey(key)){
                    String origKey = keysLowerCase.get(key);
                    serviceProperties.put(origKey, e.getValue());
                    LOG.debug("Overwriting property [{}]  with value [{}]", origKey, e.getValue());
                }else{
                    serviceProperties.put(e.getKey(), e.getValue());
                    keysLowerCase.put(e.getKey().toString().toLowerCase(), e.getKey().toString());
                }
            }
            
            
        }
    }
    
}
