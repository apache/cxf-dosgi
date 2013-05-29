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

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointListener;

public final class Util {

    static final String PATH_PREFIX = "/osgi/service_registry";

    private Util() {
        // never constructed
    }

    static String getZooKeeperPath(String name) {
        if (name == null || "".equals(name)) {
            return PATH_PREFIX;
        }
        return PATH_PREFIX + '/' + name.replace('.', '/');
    }

    /**
     * Returns the value of a "string+" property as an array of strings.
     * <p>
     * A "string+" property can have a value which is either a string,
     * an array of strings, or a collection of strings.
     * <p>
     * If the given value is not of one of the valid types, or is null,
     * an empty array is returned.
     *
     * @param property a "string+" property value
     * @return the property value as an array of strings, or an empty array
     */
    public static String[] getStringPlusProperty(Object property) {
        if (property instanceof String) {
            return new String[] {(String)property};
        } else if (property instanceof String[]) {
            return (String[])property;
        } else if (property instanceof Collection) {
            try {
                @SuppressWarnings("unchecked")
                Collection<String> strings = (Collection<String>)property;
                return strings.toArray(new String[strings.size()]);
            } catch (ArrayStoreException ase) {
                // ignore collections with wrong type
            }
        }
        return new String[0];
    }

    /**
     * Removes nulls and empty strings from the given string array.
     *
     * @param strings an array of strings
     * @return a new array containing the non-null and non-empty
     *         elements of the original array in the same order
     */
    public static String[] removeEmpty(String[] strings) {
        String[] result = new String[strings.length];
        int copied = 0;
        for (String s : strings) {
            if (s != null && !s.isEmpty()) {
                result[copied++] = s;
            }
        }
        return copied == result.length ? result : Arrays.copyOf(result, copied);
    }

    public static String[] getScopes(ServiceReference sref) {
        return removeEmpty(getStringPlusProperty(sref.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE)));
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
