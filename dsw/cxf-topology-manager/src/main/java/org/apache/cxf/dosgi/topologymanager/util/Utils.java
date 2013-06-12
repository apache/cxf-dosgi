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
package org.apache.cxf.dosgi.topologymanager.util;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public final class Utils {

    private static final String OBJECTCLASS_EXPRESSION = ".*\\(" + Constants.OBJECTCLASS + "=([a-zA-Z_0-9.]+)\\).*";
    private static final Pattern OBJECTCLASS_PATTERN = Pattern.compile(OBJECTCLASS_EXPRESSION);

    private Utils() {
        // prevent instantiation
    }

    /**
     * Retrieves an endpoint's properties as a Dictionary.
     *
     * @param ep an endpoint description
     * @return endpoint properties (will never return null)
     */
    public static Dictionary<String, Object> getEndpointProperties(EndpointDescription ep) {
        if (ep == null || ep.getProperties() == null) {
            return new Hashtable<String, Object>();
        } else {
            return new Hashtable<String, Object>(ep.getProperties());
        }
    }

    public static String getObjectClass(String filter) {
        if (filter != null) {
            Matcher matcher = OBJECTCLASS_PATTERN.matcher(filter);
            if (matcher.matches() && matcher.groupCount() >= 1) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public static String getUUID(BundleContext bctx) {
        synchronized ("org.osgi.framework.uuid") {
            String uuid = bctx.getProperty("org.osgi.framework.uuid");
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                System.setProperty("org.osgi.framework.uuid", uuid);
            }
            return uuid;
        }
    }

    public static String getBundleName(ServiceReference sref) {
        Bundle bundle = sref.getBundle();
        return bundle == null ? "<unregistered>" : bundle.getSymbolicName();
    }
}
