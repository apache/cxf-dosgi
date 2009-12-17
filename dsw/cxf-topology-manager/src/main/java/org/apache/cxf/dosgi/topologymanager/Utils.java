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
package org.apache.cxf.dosgi.topologymanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;


public class Utils {

    private static final Logger LOG = Logger.getLogger(Utils.class.getName());
    
    protected static ServiceReference[] getEndpointListeners(BundleContext bctx) throws InvalidSyntaxException {
        ServiceReference[] refs = bctx
            .getServiceReferences(EndpointListener.class.getName(),
                                  "(" + EndpointListener.ENDPOINT_LISTENER_SCOPE + "=*)");
        return refs;
    }

    public static List<Filter> normalizeScope(ServiceReference sref,BundleContext bctx) throws InvalidSyntaxException {
        List<Filter> filters = new ArrayList<Filter>();
    
        Object fo = sref.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE);
        if (fo instanceof String) {
            filters.add(bctx.createFilter((String)fo));
        } else if (fo instanceof String[]) {
            String[] foArray = (String[])fo;
            for (String f : foArray) {
                filters.add(bctx.createFilter(f));
            }
        } else if (fo instanceof Collection) {
            Collection c = (Collection)fo;
            for (Object o : c) {
                if (o instanceof String) {
                    filters.add(bctx.createFilter((String)o));
                } else {
                    LOG.info("Component of a filter is not a string -> skipped !");
                }
            }
        }
    
        return filters;
    }

    
    public static String getUUID(BundleContext bctx) {
        synchronized ("org.osgi.framework.uuid") {
            String uuid = bctx.getProperty("org.osgi.framework.uuid");
            if(uuid==null){
                uuid = UUID.randomUUID().toString();
                System.setProperty("org.osgi.framework.uuid", uuid);
            }
            return uuid;
        }
    }
    
    
    public static  String extendFilter(String filter,BundleContext bctx) {
        return "(&"+filter+"(!("+RemoteConstants.ENDPOINT_FRAMEWORK_UUID+"="+Utils.getUUID(bctx)+")))";
    }
    
}
