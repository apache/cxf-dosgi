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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.dosgi.dsw.service.RemoteServiceAdminCore;
import org.apache.cxf.ws.policy.spring.PolicyNamespaceHandler;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

public final class OsgiUtils {
    
    private static final Logger LOG = LogUtils.getL7dLogger(OsgiUtils.class);

    private static final String REMOTE_SERVICES_HEADER_NAME = "Remote-Service";
    private static final String REMOTE_SERVICES_DIRECTORY = "OSGI-INF/remote-service";
    private static final String REMOTE_SERVICES_NS = "http://www.osgi.org/xmlns/sd/v1.0.0";

    static final String[] INTENT_MAP = {
        "/OSGI-INF/cxf/intents/intent-map.xml"
    };

    private static final String SERVICE_DESCRIPTION_ELEMENT = "service-description";

    private OsgiUtils() {
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
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
            URL resourceURL = (URL)urls.nextElement();
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

    public static Filter createFilter(BundleContext bc, String filterValue) {

        if (filterValue == null) {
            return null;
        }

        try {
            return bc.createFilter(filterValue);
        } catch (InvalidSyntaxException ex) {
            LOG.warning("Invalid filter expression " + filterValue);
        } catch (Exception ex) {
            LOG.warning("Problem creating a Filter from " + filterValue);
        }
        return null;
    }

    public static String[] parseIntents(String intentsSequence) {
        return intentsSequence == null ? new String[] {} : intentsSequence.split(" ");
    }

    public static String formatIntents(String[] intents) {
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> OsgiService<T> getOsgiService(BundleContext bc, Class<T> serviceClass) {
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
                LOG.fine("Problem retrieving an OSGI service " + serviceClass.getName() + ", exception : "
                         + ex.getMessage());
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
            
            // switch to cxf bundle classloader for spring
            Thread.currentThread().setContextClassLoader(PolicyNamespaceHandler.class.getClassLoader());
            
            LOG.fine("Loading Intent map from "+springIntentLocations);
            OsgiBundleXmlApplicationContext ctx = new OsgiBundleXmlApplicationContext(springIntentLocations
                .toArray(new String[] {}));
            ctx.setPublishContextAsService(false);
            ctx.setBundleContext(bundleContext);
            ctx.refresh();
            LOG.fine("application context: " + ctx);
            IntentMap im = (IntentMap)ctx.getBean("intentMap");
            LOG.fine("retrieved intent map: " + im);
            // switch back 
            Thread.currentThread().setContextClassLoader(RemoteServiceAdminCore.class.getClassLoader());
            
            return im;
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Intent map load failed: ", t);
            return null;
        }
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
            LOG.finest("Interface source bundle: " + b.getSymbolicName());

            ExportedPackage[] ep = pa.getExportedPackages(b);
            LOG.finest("Exported Packages of the source bundle: " + ep);

            String pack = iClass.getPackage().getName();
            LOG.finest("Looking for Package: " + pack);
            if (ep != null) {
	            for (ExportedPackage p : ep) {
	            	if (p != null) {
		                if (pack.equals(p.getName())) {
		                    LOG.fine("found package -> Version: " + p.getVersion());
		                    return p.getVersion().toString();
		                }
	            	}
	            }
            }
        } else {
            LOG.severe("Was unable to obtain the package admin service -> can't resolve interface versions");
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
    
}
