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
package org.apache.cxf.dosgi.systests.common.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.cxf.dosgi.samples.greeter.rest.GreeterInfo;
import org.apache.cxf.dosgi.samples.greeter.rest.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.rest.GreetingPhrase;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.provider.AegisElementProvider;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;

public abstract class AbstractJaxRsPublishHookTest extends AbstractDosgiSystemTest  {
    @Override
    protected String[] getTestBundlesNames() {
        return new String [] {
            getBundle("org.apache.cxf.dosgi.systests", "cxf-dosgi-ri-systests-common-rest"),
            getBundle("org.apache.cxf.dosgi.samples", "cxf-dosgi-ri-samples-greeter-rest-interface"),
            getBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.jsr311-api-1.0")};
        
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
        Thread.currentThread().setContextClassLoader(JAXRSClientFactoryBean.class.getClassLoader());
        installBundle("org.apache.cxf.dosgi.samples", "cxf-dosgi-ri-samples-greeter-rest-impl", null, "jar");
        
        // TODO : get this address using a DistributionProvider interface
        String address = "http://localhost:9090/greeter";
        waitForEndpoint(address + "/greeter");
        
        //do the invocation using a CXF api 
        GreeterService greeter1 = null;
        boolean serviceUsed = false;
        try {
        	JAXRSClientFactoryBean factory = new JAXRSClientFactoryBean();
            factory.setServiceClass(GreeterService.class);
            factory.setAddress(address);
            factory.setProvider(new AegisElementProvider());
            greeter1 = (GreeterService)factory.create();
            serviceUsed = useService(greeter1);
        } finally {
        	assertTrue(serviceUsed);
            Thread.currentThread().setContextClassLoader(contextLoader);
        }
            
    }
    
    private void waitForEndpoint(String address) throws Exception {
        
        URL wsdlURL = new URL(address + "?_wadl");
        
        for (int counter = 1; counter <= 10; counter++) {
            Thread.sleep(2000);
            try {
                BufferedReader is = new BufferedReader(
                                        new InputStreamReader(wsdlURL.openStream()));
                String line;
                while ((line = is.readLine()) != null) {
                    if (line.contains("application")) {
                        System.out.println("Waited for endpoint for " + counter * 2 + " secs");
                        return;
                    }
                }
            } catch (IOException ex) {
                // continue
            }
        }
        System.out.println("Failed to retrieve service wadl during 20 sec");
        fail();
    }
    
    private boolean useService(GreeterService greeter) throws Exception {
        assertNotNull(greeter);
        
        GreeterInfo info = greeter.greetMe("Fred");
        assertTrue(info.getGreetings().size() > 0);
        for (GreetingPhrase greeting: info.getGreetings()) {
        	assertEquals("Fred", greeting.getName());
            System.out.println("  " + greeting.getPhrase() 
                    + " " + greeting.getName());
        }
        return true;
    }
}
