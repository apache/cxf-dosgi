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
import java.util.Collections;

public class Util {
    private static final String DISCOVERY_STRINGARRAY_ID = "DISCOVERY_STRINGARRAY_TO_STRING;";
    static final String PATH_PREFIX = "/osgi/service_registry/";

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
        return PATH_PREFIX + name.replace('.', '/');
    }

    public static String convertStringArrayToString(String[] intents) {
        String ret = DISCOVERY_STRINGARRAY_ID;
        for (String s : intents) {
            ret += s + ";";
        }
        return ret;
    }

    public static String[] convertStringToStringArray(String intents) {
        if (intents == null)
            return null;
        intents = intents.substring(DISCOVERY_STRINGARRAY_ID.length());

        String[] arr = intents.split(";");
        return arr;
    }

    public static boolean isStringArray(String in) {
        return in != null && in.startsWith(DISCOVERY_STRINGARRAY_ID);
    }
}
