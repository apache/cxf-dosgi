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
package org.apache.cxf.dosgi.discovery.local.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        // prevent instantiation
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
            for (Object v : values) {
                if (v instanceof String) {
                    result.add((String) v);
                }
            }
            return Collections.unmodifiableList(result);
        }

        return Collections.emptyList();
    }

    public static boolean matchFilter(BundleContext bctx, String filter, EndpointDescription endpoint) {
        if (filter == null) {
            return false;
        }

        try {
            Filter f = bctx.createFilter(filter);
            Dictionary<String, Object> dict = new Hashtable<String, Object>(endpoint.getProperties());
            return f.match(dict);
        } catch (Exception e) {
            LOG.error("Problem creating a Filter from " + filter, e);
            return false;
        }
    }
}
