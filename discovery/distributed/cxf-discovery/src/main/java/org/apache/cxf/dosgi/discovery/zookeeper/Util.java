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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointListener;

public class Util {
    static final String PATH_PREFIX = "/osgi/service_registry";

    @SuppressWarnings("unchecked")
    static Collection<String> getMultiValueProperty(Object property) {
        if (property instanceof Collection) {
            return (Collection<String>)property;
        } else if (property instanceof String[]) {
            return Arrays.asList((String[])property);
        } else if (property == null) {
            return Collections.emptySet();
        } else {
            return Collections.singleton(property.toString());
        }
    }

    static String getZooKeeperPath(String name) {
        if(name==null || "".equals(name)) return PATH_PREFIX;
        return PATH_PREFIX + '/' + name.replace('.', '/');
    }

    
    static String[] getStringPlusProperty(Object property) {

        if (property instanceof String) {
            // System.out.println("String");
            String[] ret = new String[1];
            ret[0] = (String)property;
            return ret;
        }

        if (property instanceof String[]) {
            // System.out.println("String[]");
            return (String[])property;
        }

        if (property instanceof Collection) {
            @SuppressWarnings("rawtypes")
            Collection col = (Collection)property;
            // System.out.println("Collection: size "+col.size());
            String[] ret = new String[col.size()];
            int x = 0;
            for (Object s : col) {
                ret[x] = (String)s;
                ++x;
            }
            return ret;
        }

        return new String[0];
    }

    public static String[] getScopes(ServiceReference sref) {
        String[] scopes = Util.getStringPlusProperty(sref.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE));
        ArrayList<String> normalizedScopes = new ArrayList<String>(scopes.length);
        for (String scope : scopes) {
            if (scope != null && !"".equals(scope)) {
                normalizedScopes.add(scope);
            }
        }
        return normalizedScopes.toArray(new String[normalizedScopes.size()]);
    }

    // copied from the DSW OSGiUtils class
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
