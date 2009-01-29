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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.samples.greeter.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.core.io.Resource;

public abstract class AbstractListenerHookServiceListenerTest extends AbstractDosgiSystemTest  {       
    private FutureTask<Map<GreetingPhrase, String>> task;

    @Override
    protected String[] getTestBundlesNames() {
        return new String [] {
            getBundle("org.apache.cxf", "cxf-dosgi-ri-systests-common"),
            getBundle("org.apache.cxf", "cxf-dosgi-ri-samples-greeter-interface")};
    }
    
    @Override
    protected Resource[] getTestBundles() {
        // Remove the CXF DSW bundle for this test as we'll be installing it later in the 
        // test itself        
        List<Resource> resources = new ArrayList<Resource>(Arrays.asList(super.getTestBundles()));
        for (Iterator<Resource> it = resources.iterator(); it.hasNext(); ) {
            String fn = it.next().getFilename();
            if (fn.startsWith("cxf-dosgi-ri-dsw-cxf") && fn.endsWith(".jar")) {
                it.remove();
            }
        }
        return resources.toArray(new Resource[resources.size()]);
    }

    public void testBasicInvocation() throws Exception {

        Thread.currentThread().setContextClassLoader(ClientProxyFactoryBean.class.getClassLoader());

        Server server = null;
        ServiceTracker tracker = null;
        try {
            server = startServer("http://localhost:9090/greeter", 
                                 GreeterService.class, new GreeterServiceImpl());
            
            tracker = new ServiceTracker(bundleContext, 
                                         GreeterService.class.getName(), null) {
                @Override
                public Object addingService(final ServiceReference reference) {
                    Object result = super.addingService(reference);

                    FutureTask<Map<GreetingPhrase, String>> future =
                        new FutureTask<Map<GreetingPhrase, String>>(new Callable<Map<GreetingPhrase, String>>() {
                          public Map<GreetingPhrase, String> call() {
                            return useService(reference);
                        }});
                    future.run();
                    setFuture(future);
                    return result;
                }
            };
            tracker.open();
            // sleep for a bit
            Thread.sleep(2000);
            
            // now install dsw
            installBundle("org.apache.cxf", "cxf-dosgi-ri-dsw-cxf", null, "jar");
            verifyGreeterResponse();
        } finally {
            if (tracker != null) {
                tracker.close();
            }
            
            if (server != null) {
                server.getDestination().shutdown();
                server.stop();
            }
            
        }
    }
    
    private Map<GreetingPhrase, String> useService(ServiceReference sref) {
        GreeterService hs = (GreeterService)bundleContext.getService(sref);
        assertNotNull(hs);
        try {
            return hs.greetMe("Fred");
        } catch (Exception ex) {
            fail("unexpected exception");
        }
        return null; 
    }
    
    private void verifyGreeterResponse() throws Exception {
        Map<GreetingPhrase, String> greetings = null;
        synchronized (this) {
            while (task == null) {
                wait(500);    
            }
            greetings = task.get();
        }
        
        assertEquals("Fred", greetings.get(new GreetingPhrase("Hello")));
    }
    
    private void setFuture(FutureTask<Map<GreetingPhrase, String>> future) {
        synchronized (this) {
            task = future;
            notify();
        }
    }
        
    
    private class GreeterServiceImpl implements GreeterService {

        private final static String STRANGER_NAME = "Stranger";
                
        public Map<GreetingPhrase, String> greetMe(String name) 
            throws GreeterException {

            if (name.equals(STRANGER_NAME)) {
                throw new GreeterException(name);
            }
            
            Map<GreetingPhrase, String> greetings = 
                new HashMap<GreetingPhrase, String>();
            
            greetings.put(new GreetingPhrase("Hello"), name);
            greetings.put(new GreetingPhrase("Hoi"), name);
            greetings.put(new GreetingPhrase("Hola"), name);
            greetings.put(new GreetingPhrase("Bonjour"), name);
            
            
            return greetings;
        }

    } 
    
    private Server startServer(String address, Class<?> type, Object impl) {
        ServerFactoryBean factory = new ServerFactoryBean();
        factory.setServiceClass(type);
        factory.setAddress(address);
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());
        factory.setServiceBean(impl);
        Server server = factory.create();
        server.start();
        return server;
    }
    
}
