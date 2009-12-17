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

import java.util.Collection;

import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;

public class Utils {

    public static String[] normalizeStringPlus(Object object) {

        if (object instanceof String) {
            String s = (String)object;
            String[] ret = new String[1];
            ret[0] = s;
            return ret;
        }

        if (object instanceof String[]) {
            return (String[])object;
        }
        // FIXME: This needs to be tested !!!!!
        if (object instanceof Collection) {
            Collection col = (Collection)object;
            if (col.toArray() instanceof String[]) {
                return (String[])col.toArray();
            }
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

}
