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
package org.apache.cxf.dosgi.dsw.qos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.dosgi.dsw.util.Utils;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntentUtils {
    private static final Logger LOG = LoggerFactory.getLogger(IntentUtils.class);

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

    public static String[] parseIntents(String intentsSequence) {
        return intentsSequence == null ? new String[] {} : intentsSequence.split(" ");
    }

    @SuppressWarnings("rawtypes")
    public static String[] getInetntsImplementedByTheService(Map serviceProperties) {
        // Get the Intents that are implemented by the service
        String[] serviceIntents = Utils.normalizeStringPlus(serviceProperties.get(RemoteConstants.SERVICE_INTENTS));

        return serviceIntents;
    }

    public static String[] mergeArrays(String[] a1, String[] a2) {
        if(a1==null) return a2;
        if(a2==null) return a1;

        List<String> list = new ArrayList<String>(a1.length + a2.length);

        for (String s : a1) {
            list.add(s);
        }

        for (String s : a2) {
            if (!list.contains(s))
                list.add(s);
        }

        return list.toArray(new String[list.size()]);
    }
    
    public static Set<String> getRequestedIntents(Map<?, ?> sd) {
        Collection<String> intents = Arrays.asList(
            IntentUtils.parseIntents(OsgiUtils.getProperty(sd, RemoteConstants.SERVICE_EXPORTED_INTENTS)));        
        Collection<String> oldIntents = Arrays.asList(
            IntentUtils.parseIntents(OsgiUtils.getProperty(sd, Constants.EXPORTED_INTENTS_OLD))); 
        
        Set<String> allIntents = new HashSet<String>(intents.size() + oldIntents.size());
        allIntents.addAll(intents);
        allIntents.addAll(oldIntents);
        LOG.debug("Intents asserted: " + allIntents);
        return allIntents;
    }

}
