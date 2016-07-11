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
package org.apache.cxf.dosgi.common.intent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.dosgi.common.util.OsgiUtils;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public final class IntentHelper {
    private IntentHelper() {
    }

    public static Set<String> getExported(Map<String, Object> sd) {
        Set<String> allIntents = new HashSet<String>();
        Collection<String> intents = OsgiUtils
            .getMultiValueProperty(sd.get(RemoteConstants.SERVICE_EXPORTED_INTENTS));
        if (intents != null) {
            allIntents.addAll(parseIntents(intents));
        }
        Collection<String> intents2 = OsgiUtils
            .getMultiValueProperty(sd.get(RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA));
        if (intents2 != null) {
            allIntents.addAll(parseIntents(intents2));
        }
        return allIntents;
    }
    
    public static Set<String> getImported(Map<String, Object> sd) {
        Collection<String> intents = OsgiUtils.getMultiValueProperty(sd.get(RemoteConstants.SERVICE_INTENTS));
        return intents == null ? new HashSet<String>() : new HashSet<String>(intents);
    }
    
    private static Collection<String> parseIntents(Collection<String> intents) {
        List<String> parsed = new ArrayList<String>();
        for (String intent : intents) {
            parsed.addAll(Arrays.asList(intent.split("[ ]")));
        }
        return parsed;
    }
}
