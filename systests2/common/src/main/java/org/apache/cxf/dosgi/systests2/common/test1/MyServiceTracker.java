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
package org.apache.cxf.dosgi.systests2.common.test1;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.dosgi.samples.greeter.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class MyServiceTracker extends ServiceTracker {
    private static StringBuffer invocationResult = new StringBuffer();

    public MyServiceTracker(BundleContext context) {
        super(context, GreeterService.class.getName(), null);
    }

    public Object addingService(ServiceReference reference) {
        Object svc = super.addingService(reference);
        if (svc instanceof GreeterService) {
            System.out.println("[client] Got a GreeterService...");
            invokeGreeter((GreeterService) svc);
        }
        return svc;
    }
    
    public static String getResult() {
        return invocationResult.toString();
    }

    private void invokeGreeter(GreeterService svc) {
        try {
            Map<GreetingPhrase, String> result = svc.greetMe("OSGi");
            for (Map.Entry<GreetingPhrase, String> e : result.entrySet()) {
                GreetingPhrase key = e.getKey();
                invocationResult.append(key.getPhrase());
                invocationResult.append(e.getValue());
            }
            try {
                svc.greetMe(new GreeterDataImpl());
            } catch (GreeterException ex) {
                invocationResult.append(";exception");
            } 
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("result", invocationResult.toString());
            props.put("testResult", "test1");

            System.out.println("[client] Successfully invoked remote service. Registering test response service...");
            context.registerService(String.class.getName(), "test1", props);
        } catch (Exception x) {
            System.err.println("[client] Error during remote service invocation:");
            x.printStackTrace(System.err);
        }
    }
}