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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.service.ListenerHook;

public class CxfListenerHook extends AbstractClientHook implements ListenerHook {

    private static final Logger LOG = Logger.getLogger(CxfListenerHook.class.getName());

    
    private final static String CLASS_NAME_EXPRESSION =
        ".*\\(" + Constants.OBJECTCLASS + "=([a-zA-Z_0-9.]+)\\).*";
    private final static Pattern CLASS_NAME_PATTERN = 
        Pattern.compile(CLASS_NAME_EXPRESSION);
    private static final Set<String> SYSTEM_PACKAGES;
    
    static {
        SYSTEM_PACKAGES = new HashSet<String>();
        SYSTEM_PACKAGES.add("org.osgi.service");
        SYSTEM_PACKAGES.add("org.apache.felix");
        SYSTEM_PACKAGES.add("org.ops4j.pax.logging");
        SYSTEM_PACKAGES.add("ch.ethz.iks.slp");
        SYSTEM_PACKAGES.add("org.ungoverned.osgi.service");
        SYSTEM_PACKAGES.add("org.springframework.osgi.context.event.OsgiBundleApplicationContextListener");
    }
    
    public CxfListenerHook(BundleContext bc, CxfDistributionProvider dp) {
        super(bc, dp);
    }
    
    public void added(Collection /*<? extends ListenerHook.ListenerInfo>*/ listeners) {
        handleListeners(listeners);
    }

    public void removed(Collection /*<? extends ListenerHook.ListenerInfo>*/ listener) {
        // todo add this in - need to unregister the endpoints
    }

    private void handleListeners(Collection/*<? extends ListenerHook.ListenerInfo>*/ listeners) {
        for (Iterator/*<? extends ListenerHook.ListenerInfo>*/ it = listeners.iterator(); it.hasNext(); ) {
            ListenerHook.ListenerInfo listener = (ListenerHook.ListenerInfo) it.next();
            
            if (listener.getFilter() == null
                    || listener.getBundleContext() == getContext()) {
                    continue;
                }
                String className = getClassNameFromFilter(listener.getFilter());
                if (!isClassSupported(className)) {
                    continue;
                }
                
                processClientDescriptions(listener.getBundleContext(), 
                                          className, 
                                          listener.getFilter());            
        }
    }
    
    private String getClassNameFromFilter(String filter) {
        if (filter == null) {
            return null;
        }
        Matcher matcher = CLASS_NAME_PATTERN.matcher(filter);
        if (matcher.matches() && matcher.groupCount() >= 1) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    private static boolean isClassSupported(String className) {
        if (className == null) {
            return false;
        }
        
        for (String p : SYSTEM_PACKAGES) {
            if (className.startsWith(p)) {
                LOG.fine("Lookup for " + className + " is ignored");
                return false;
            }
        }
        return true;
    }
}
