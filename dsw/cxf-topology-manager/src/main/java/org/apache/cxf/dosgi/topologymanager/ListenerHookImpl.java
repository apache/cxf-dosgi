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
package org.apache.cxf.dosgi.topologymanager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

/**
 * Listens for service listeners and informs ServiceInterestListener about added and removed interest
 * in services
 */
public class ListenerHookImpl implements ListenerHook {
    private static final Logger LOG = Logger.getLogger(ListenerHookImpl.class.getName());
    private BundleContext bctx;
    private ServiceInterestListener serviceInterestListener;

    private final static String CLASS_NAME_EXPRESSION = ".*\\(" + Constants.OBJECTCLASS
                                                        + "=([a-zA-Z_0-9.]+)\\).*";
    private final static Pattern CLASS_NAME_PATTERN = Pattern.compile(CLASS_NAME_EXPRESSION);

    // From the old impl.
    private static final Set<String> SYSTEM_PACKAGES;
    static {
        SYSTEM_PACKAGES = new HashSet<String>();
        SYSTEM_PACKAGES.add("org.osgi.service");
        SYSTEM_PACKAGES.add("org.apache.felix");
        SYSTEM_PACKAGES.add("org.ops4j.pax.logging");
        SYSTEM_PACKAGES.add("ch.ethz.iks.slp");
        SYSTEM_PACKAGES.add("org.ungoverned.osgi.service");
        SYSTEM_PACKAGES.add("org.springframework.osgi.context.event.OsgiBundleApplicationContextListener");
        SYSTEM_PACKAGES.add("java.net.ContentHandler");
    }

    public ListenerHookImpl(BundleContext bc, ServiceInterestListener serviceInterestListener) {
        bctx = bc;
        this.serviceInterestListener = serviceInterestListener;
    }

    @SuppressWarnings("rawtypes")
    public void added(Collection/* <ListenerInfo> */ listeners) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("ListenerHookImpl: added() " + listeners);
        }
        for (Object li : listeners) {
            ListenerInfo listenerInfo = (ListenerInfo)li;
            LOG.fine("*** Filter: " + listenerInfo.getFilter());

            String className = getClassNameFromFilter(listenerInfo.getFilter());

            if (listenerInfo.getBundleContext().getBundle().equals(bctx.getBundle())) {
                LOG.fine("ListenerHookImpl: skipping request from myself");
                continue;
            }

            if (listenerInfo.getFilter() == null) {
                LOG.fine("ListenerHookImpl: skipping empty filter");
                continue;
            }

            if (isClassExcluded(className)) {
                LOG.fine("ListenerHookImpl: skipping import request for excluded classs ["
                                   + className + "]");
                continue;
            }
            String exFilter = extendFilter(listenerInfo.getFilter(), bctx);
            serviceInterestListener.addServiceInterest(exFilter);

        }

    }

    @SuppressWarnings("rawtypes")
    public void removed(Collection/* <ListenerInfo> */ listeners) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("ListenerHookImpl: removed: " + listeners);
        }

        for (Object li : listeners) {
            ListenerInfo listenerInfo = (ListenerInfo)li;
            LOG.fine("*** Filter: " + listenerInfo.getFilter());

            // TODO: determine if service was handled ? 
            String exFilter = extendFilter(listenerInfo.getFilter(), bctx);
            serviceInterestListener.removeServiceInterest(exFilter);

        }

    }

    private String getClassNameFromFilter(String filter) {
        if (filter != null) {
            Matcher matcher = CLASS_NAME_PATTERN.matcher(filter);
            if (matcher.matches() && matcher.groupCount() >= 1) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static boolean isClassExcluded(String className) {
        if (className == null) {
            return true;
        }

        for (String p : SYSTEM_PACKAGES) {
            if (className.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    static String getUUID(BundleContext bctx) {
        synchronized ("org.osgi.framework.uuid") {
            String uuid = bctx.getProperty("org.osgi.framework.uuid");
            if(uuid==null){
                uuid = UUID.randomUUID().toString();
                System.setProperty("org.osgi.framework.uuid", uuid);
            }
            return uuid;
        }
    }
    
    static String extendFilter(String filter, BundleContext bctx) {
        return "(&" + filter + "(!(" + RemoteConstants.ENDPOINT_FRAMEWORK_UUID + "=" + getUUID(bctx) + ")))";
    }

}
