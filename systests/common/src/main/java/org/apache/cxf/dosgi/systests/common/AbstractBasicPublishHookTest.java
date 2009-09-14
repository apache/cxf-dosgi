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
package org.apache.cxf.dosgi.systests.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.samples.greeter.GreeterData;
import org.apache.cxf.dosgi.samples.greeter.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;

public abstract class AbstractBasicPublishHookTest extends AbstractDosgiSystemTest  {
    @Override
    protected String[] getTestBundlesNames() {
        return new String [] {
            getBundle("org.apache.cxf.dosgi.systests", "cxf-dosgi-ri-systests-common"),
            getBundle("org.apache.cxf.dosgi.samples", "cxf-dosgi-ri-samples-greeter-interface")};
    }

    public void testBasicInvocation() throws Exception {
        
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "cxf-dsw");
        
        ServiceReference[] srefs
            = bundleContext.getAllServiceReferences(ManagedService.class.getName(),
                                                    "(" 
                                                    + Constants.SERVICE_PID
                                                    + "="
                                                    + "cxf-dsw"
                                                    + ")");
        assertNotNull(srefs);
        assertEquals(1, srefs.length);
        ManagedService ms = (ManagedService)bundleContext.getService(srefs[0]);
        ms.updated(props);
        
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ClientProxyFactoryBean.class.getClassLoader());
        installBundle("org.apache.cxf.dosgi.samples", "cxf-dosgi-ri-samples-greeter-impl", null, "jar");
        
        // TODO : get this address using a DistributionProvider interface
        String address = "http://localhost:9090/greeter";
        waitForEndpoint(address);
        
        //do the invocation using a CXF api 
        GreeterService greeter1 = null;
        try {
            ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
            factory.setServiceClass(GreeterService.class);
            factory.setAddress(address);
            factory.getServiceFactory().setDataBinding(new AegisDatabinding());
            greeter1 = (GreeterService)factory.create();
            useService(greeter1);
        } finally {
            Thread.currentThread().setContextClassLoader(contextLoader);
        }
            
    }
    
    private void waitForEndpoint(String address) throws Exception {
        
        URL wsdlURL = new URL(address + "?wsdl");
        
        for (int counter = 1; counter <= 10; counter++) {
            Thread.sleep(2000);
            try {
                BufferedReader is = new BufferedReader(
                                        new InputStreamReader(wsdlURL.openStream()));
                String line;
                while ((line = is.readLine()) != null) {
                    if (line.contains("definitions")) {
                        System.out.println("Waited for endpoint for " + counter * 2 + " secs");
                        return;
                    }
                }
            } catch (IOException ex) {
                // continue
            }
        }
        System.out.println("Failed to retrieve service wsdl during 20 sec");
    }
    
    private void useService(GreeterService greeter) throws Exception {
        assertNotNull(greeter);
        
        Map<GreetingPhrase, String> greetings = greeter.greetMe("Fred");
        assertEquals("Fred", greetings.get(new GreetingPhrase("Hello")));
        
        try {
            class GreeterDataImpl implements GreeterData {
                private String name;
                private int age;
                private boolean exception;

                GreeterDataImpl(String n, int a, boolean ex) {
                    name = n;
                    age = a;
                    exception = ex;
                }
                
                public String getName() {
                    return name;
                }

                public int getAge() {
                    return age;
                }

                public boolean isException() {
                    return exception;
                }                
            }
            
            GreeterData gd = new GreeterDataImpl("Stranger", 11, true);
            greeter.greetMe(gd);
            fail("GreeterException has to be thrown");
        } catch (GreeterException ex) {
            assertEquals("Wrong exception message", 
                         "GreeterService can not greet Stranger", 
                         ex.toString());
        }
    }
}
