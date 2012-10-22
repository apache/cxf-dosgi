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
package org.apache.cxf.dosgi.dsw.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;

public class Utils {

    private static final Logger LOG = LogUtils.getL7dLogger(Utils.class);
    
    @SuppressWarnings("rawtypes")
	public static String[] normalizeStringPlus(Object object) {

        if (object instanceof String) {
            String s = (String)object;
            String[] values = s.split(",");
            List<String> list = new ArrayList<String>();
            for (String val : values) {
            	String actualValue = val.trim();
            	if (actualValue.length() > 0) {
            		list.add(actualValue);
            	}
            }
            return list.toArray(new String[0]);
        }

        if (object instanceof String[]) {
            return (String[])object;
        }
        if (object instanceof Collection) {
            Collection col = (Collection)object;
            ArrayList<String> ar = new ArrayList<String>(col.size());
            for (Object o : col) {
                if (o instanceof String) {
                    String s = (String)o;
                    ar.add(s);
                }else{
                    LOG.warning("stringPlus contained non string element in list ! Element was skipped");
                }
            }
            return ar.toArray(new String[ar.size()]);
        }

        return null;
    }
    
    
    
    
    public static String remoteServiceAdminEventTypeToString(int type){
        switch (type) {
        case RemoteServiceAdminEvent.EXPORT_ERROR:
            return "EXPORT_ERROR";
        case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
            return "EXPORT_REGISTRATION";
        case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION:
            return "EXPORT_UNREGISTRATION";
        case RemoteServiceAdminEvent.EXPORT_WARNING:
            return "EXPORT_WARNING";
        case RemoteServiceAdminEvent.IMPORT_ERROR:
            return "IMPORT_ERROR";
        case RemoteServiceAdminEvent.IMPORT_REGISTRATION:
            return "IMPORT_REGISTRATION";
        case RemoteServiceAdminEvent.IMPORT_UNREGISTRATION:
            return "IMPORT_UNREGISTRATION";
        case RemoteServiceAdminEvent.IMPORT_WARNING:
            return "IMPORT_WARNING";    
        default:
            return "UNKNOWN_EVENT";
        }
    }
    
    
    @SuppressWarnings("rawtypes")
	public static String[] getAllRequiredIntents(Map serviceProperties){
        // Get the intents that need to be supported by the RSA
        String[] requiredIntents = Utils.normalizeStringPlus(serviceProperties.get(RemoteConstants.SERVICE_EXPORTED_INTENTS));
        if(requiredIntents==null){
            requiredIntents = new String[0];
        }
        
        { // merge with extra intents;
            String[] requiredExtraIntents = Utils.normalizeStringPlus(serviceProperties.get(RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA));
            if(requiredExtraIntents!= null && requiredExtraIntents.length>0){
                         
                requiredIntents = mergeArrays(requiredIntents, requiredExtraIntents);
            }
        }
        
        return requiredIntents;
    }

    @SuppressWarnings("rawtypes")
	public static String[] getInetntsImplementedByTheService(Map serviceProperties){
        // Get the Intents that are implemented by the service 
        String[] serviceIntents = Utils.normalizeStringPlus(serviceProperties.get(RemoteConstants.SERVICE_INTENTS));
        
        return serviceIntents;
    }
 
    
    public static String[] mergeArrays(String[] a1,String[] a2){
        if(a1==null) return a2;
        if(a2==null) return a1;
        
        List<String> list = new ArrayList<String>(a1.length+a2.length);

        for (String s : a1) {
            list.add(s);  
        }
        
        for (String s : a2) {
            if(!list.contains(s))
                list.add(s);  
        }
        
        return list.toArray(new String[list.size()]);
    }
    
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static void overlayProperties(Properties serviceProperties, Map additionalProperties) {
        Enumeration<Object> keys = serviceProperties.keys();
        // Maps lower case key to original key
        HashMap<String,String> keysLowerCase = new HashMap<String, String>();
        while(keys.hasMoreElements()){
            Object o = keys.nextElement(); 
            if (o instanceof String) {
                String ks = (String)o;
                keysLowerCase.put(ks.toLowerCase(), ks);
            }
        }
        
        Set<Map.Entry> adProps = additionalProperties.entrySet();
        for (Map.Entry e : adProps) {
            // objectClass and service.id must not be overwritten
            Object keyObj = e.getKey();
            if (keyObj instanceof String && keyObj != null) {
                String key = ((String)keyObj).toLowerCase();
                if (org.osgi.framework.Constants.SERVICE_ID.toLowerCase().equals(key)
                    || org.osgi.framework.Constants.OBJECTCLASS.toLowerCase().equals(key)) {
                    LOG.info("exportService called with additional properties map that contained illegal key: "
                              + key + "   The key is ignored");
                    continue;
                }else if(keysLowerCase.containsKey(key)){
                    String origKey = keysLowerCase.get(key);
                    serviceProperties.put(origKey, e.getValue());
                    LOG.fine("Overwriting property [" + origKey + "]  with value [" + e.getValue() + "]");
                }else{
                    serviceProperties.put(e.getKey(), e.getValue());
                    keysLowerCase.put(e.getKey().toString().toLowerCase(), e.getKey().toString());
                }
            }
            
            
        }
    }

    
}
