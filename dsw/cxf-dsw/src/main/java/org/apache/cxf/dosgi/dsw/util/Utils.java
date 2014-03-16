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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        // never constructed
    }

    @SuppressWarnings("rawtypes")
    public static String[] normalizeStringPlus(Object object) {
        if (object instanceof String) {
            String s = (String)object;
            String[] values = s.split(",");
            List<String> list = new ArrayList<String>();
            for (String val : values) {
                String actualValue = val.trim();
                if (!actualValue.isEmpty()) {
                    list.add(actualValue);
                }
            }
            return list.toArray(new String[list.size()]);
        }

        if (object instanceof String[]) {
            return (String[])object;
        }
        
        if (object instanceof Collection) {
            Collection col = (Collection)object;
            List<String> ar = new ArrayList<String>(col.size());
            for (Object o : col) {
                if (o instanceof String) {
                    String s = (String)o;
                    ar.add(s);
                } else {
                    LOG.warn("stringPlus contained non string element in list! Element was skipped");
                }
            }
            return ar.toArray(new String[ar.size()]);
        }

        return null;
    }

    /**
     * Converts the given Dictionary to a Map.
     *
     * @param dict a dictionary
     * @param <K> the key type
     * @param <V> the value type
     * @return the converted map, or an empty map if the given dictionary is null
     */
    public static <K, V> Map<K, V> toMap(Dictionary<K, V> dict) {
        Map<K, V> map = new HashMap<K, V>();
        if (dict != null) {
            Enumeration<K> keys = dict.keys();
            while (keys.hasMoreElements()) {
                K key = keys.nextElement();
                map.put(key, dict.get(key));
            }
        }
        return map;
    }

    public static <K, V> void setIfNotNull(Map<K, V> map, K key, V val) {
        if (val != null) {
            map.put(key, val);
        }
    }
}
