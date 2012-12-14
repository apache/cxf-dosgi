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
package org.apache.cxf.dosgi.systests2.common.test2.client;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.cxf.dosgi.systests2.common.test2.Test2Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Test2ServiceTracker extends ServiceTracker {
    public Test2ServiceTracker(BundleContext context) {
        super(context, getFilter(context), null);
    }

    private static Filter getFilter(BundleContext context) {
        Filter f = null;
        try {
            // It's very important that the service.imported condition is there too
            // otherwise the tracker will make a local 'direct' invocation on the service.
            // The service.imported forces a proxy lookup.
            f = context.createFilter("(&(objectClass=" + Test2Service.class.getName() + ")(service.imported=*))");
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        return f;
    }

    @Override
    public Object addingService(ServiceReference reference) {
        Object svc = super.addingService(reference);        
        if (svc instanceof Test2Service) {
            System.out.println("*** Ref: " + reference);
            for (String key : reference.getPropertyKeys()) {
                System.out.println("  " + key + "-" + reference.getProperty(key));
            }
            
            invokeRemoteTestService(context, (Test2Service) svc);
        }
        return svc;
    }

    private void invokeRemoteTestService(BundleContext bc, Test2Service svc) {
        String res = svc.getRemoteStackTrace();
        
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("result", res);
        props.put("testResult", "test2");
        bc.registerService(String.class.getName(), "test2", props);
    }
}
