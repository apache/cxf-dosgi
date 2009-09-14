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
import org.apache.cxf.dosgi.samples.greeter.GreeterData;
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

    private final static String ADDRESS1 = "http://localhost:9090/greeter";
    private final static String ADDRESS2 = "http://localhost:9089/greeter";
    private FutureTask<Map<GreetingPhrase, String>> task1;
    private Object mutex1 = new Object(); 
    private FutureTask<Map<GreetingPhrase, String>> task2;
    private Object mutex2 = new Object(); 

    @Override
    protected String[] getTestBundlesNames() {
        return new String [] {
            getBundle("org.apache.cxf.dosgi.systests", "cxf-dosgi-ri-systests-common"),
            getBundle("org.apache.cxf.dosgi.samples", "cxf-dosgi-ri-samples-greeter-interface")};
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

        Server server1 = null;
        Server server2 = null;
        ServiceTracker tracker = null;
        try {
            server1 = startServer(ADDRESS1, 
                                  GreeterService.class, new GreeterServiceImpl());
            
            server2 = startServer(ADDRESS2, 
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
                    synchronized (mutex1) {
                        synchronized (mutex2) {
                            if (task1 == null) {
                                task1 = future;
                                mutex1.notify();
                            } else if (task2 == null) {
                                task2 = future;
                                mutex2.notify();
                            }
                        }
                    }
                    return result;
                }
            };
            tracker.open();
            // sleep for a bit
            Thread.sleep(2000);
            
            installDswIfNeeded();

            verifyGreeterResponse(task1, mutex1);
            verifyGreeterResponse(task2, mutex2);
        } finally {
            if (tracker != null) {
                tracker.close();
            }
            
            if (server1 != null) {
                server1.getDestination().shutdown();
                server1.stop();
            }

            if (server2 != null) {
                server2.getDestination().shutdown();
                server2.stop();
            }
            
        }
    }

    public void testMultiServiceProxification() throws Exception {

        Thread.currentThread().setContextClassLoader(ClientProxyFactoryBean.class.getClassLoader());

        installDswIfNeeded();

        // sleep for a bit
        Thread.sleep(2000);

        ServiceReference[] srefs =
            bundleContext.getAllServiceReferences(GreeterService.class.getName(), null);
        assertNotNull(srefs);
        assertEquals(2, srefs.length);
        String addr1 = (String) 
            srefs[0].getProperty("osgi.remote.configuration.pojo.address");
        String addr2 = (String)
            srefs[1].getProperty("osgi.remote.configuration.pojo.address");
        assertNotNull(addr1);
        assertNotNull(addr2);
        assertTrue("unexpected address property: " + addr1,
                   ADDRESS1.equals(addr1) ^ ADDRESS2.equals(addr1));
        assertTrue("unexpected address property: " + addr2,
                   ADDRESS1.equals(addr2) ^ ADDRESS2.equals(addr2));
    }

    protected abstract boolean usingIntegralDsw();

    private void installDswIfNeeded() throws Exception {
        if (!usingIntegralDsw()) {
            // now install dsw
            installBundle("org.apache.cxf.dosgi", "cxf-dosgi-ri-dsw-cxf", null, "jar");
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
    
    private void verifyGreeterResponse(FutureTask<Map<GreetingPhrase, String>> task, Object mutex) throws Exception {
        Map<GreetingPhrase, String> greetings = null;
        synchronized (mutex) {
            while (task == null) {
                mutex.wait(500);    
            }
            greetings = task.get();
        }
        
        assertEquals("Fred", greetings.get(new GreetingPhrase("Hello")));
    }
    
    private class GreeterServiceImpl implements GreeterService {
        public Map<GreetingPhrase, String> greetMe(String name) {
            System.out.println("Invoking: greetMe(" + name + ")");
            
            Map<GreetingPhrase, String> greetings = 
                new HashMap<GreetingPhrase, String>();
            
            greetings.put(new GreetingPhrase("Hello"), name);
            greetings.put(new GreetingPhrase("Hoi"), name);
            greetings.put(new GreetingPhrase("Hola"), name);
            greetings.put(new GreetingPhrase("Bonjour"), name);
            
            
            return greetings;
        }

        public GreetingPhrase [] greetMe(GreeterData gd) throws GreeterException {
            if (gd.isException()) {
                System.out.println("Throwing custom exception from: greetMe(" + gd.getName() + ")");
                throw new GreeterException(gd.getName());
            }
            
            String details = gd.getName() + "(" + gd.getAge() + ")";
            System.out.println("Invoking: greetMe(" + details + ")");
            
            GreetingPhrase [] greetings = new GreetingPhrase [] {
                new GreetingPhrase("Howdy " + details),
                new GreetingPhrase("Hallo " + details),
                new GreetingPhrase("Ni hao " + details)
            };
            
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
