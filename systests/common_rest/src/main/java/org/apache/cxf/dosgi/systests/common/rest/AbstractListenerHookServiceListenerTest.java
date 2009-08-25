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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.cxf.dosgi.samples.greeter.rest.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.rest.GreeterInfo;
import org.apache.cxf.dosgi.samples.greeter.rest.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.rest.GreetingPhrase;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.AegisElementProvider;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.core.io.Resource;

public abstract class AbstractListenerHookServiceListenerTest extends AbstractDosgiSystemTest  {       

    private final static String ADDRESS1 = "http://localhost:9090/greeter";
    private final static String ADDRESS2 = "http://localhost:9089/greeter";
    private FutureTask<GreeterInfo> task1;
    private Object mutex1 = new Object(); 
    private FutureTask<GreeterInfo> task2;
    private Object mutex2 = new Object(); 

    @Override
    protected String[] getTestBundlesNames() {
        return new String [] {
            getBundle("org.apache.cxf.dosgi.systests", "cxf-dosgi-ri-systests-common-rest"),
            getBundle("org.apache.cxf.dosgi.samples", "cxf-dosgi-ri-samples-greeter-rest-interface"),
            getBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.jsr311-api-1.0")};
        
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

        Thread.currentThread().setContextClassLoader(JAXRSClientFactoryBean.class.getClassLoader());
        bundleContext.registerService(new String[]{"javax.ws.rs.ext.MessageBodyReader"}, 
       		new AegisElementProvider(), new Hashtable()); 
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

                    FutureTask<GreeterInfo> future =
                        new FutureTask<GreeterInfo>(new Callable<GreeterInfo>() {
                          public GreeterInfo call() {
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

        Thread.currentThread().setContextClassLoader(JAXRSClientFactoryBean.class.getClassLoader());
        bundleContext.registerService(new String[]{"javax.ws.rs.ext.MessageBodyReader"}, 
        		new AegisElementProvider(), new Hashtable()); 
        installDswIfNeeded();

        // sleep for a bit
        Thread.sleep(2000);

        ServiceReference[] srefs =
            bundleContext.getAllServiceReferences(GreeterService.class.getName(), null);
        assertNotNull(srefs);
        assertEquals(2, srefs.length);
        String addr1 = (String) 
            srefs[0].getProperty("org.apache.cxf.rs.address");
        String addr2 = (String)
            srefs[1].getProperty("org.apache.cxf.rs.address");
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

    private GreeterInfo useService(ServiceReference sref) {
        GreeterService hs = (GreeterService)bundleContext.getService(sref);
        assertNotNull(hs);
        try {
            return hs.greetMe("Fred");
        } catch (Exception ex) {
            fail("unexpected exception");
        }
        return null; 
    }
    
    private void verifyGreeterResponse(FutureTask<GreeterInfo> task, Object mutex) throws Exception {
    	GreeterInfo greetings = null;
        synchronized (mutex) {
            while (task == null) {
                mutex.wait(500);    
            }
            greetings = task.get();
        }
        
        assertEquals("4 greetings expected", 4, greetings.getGreetings().size());
    }
    
    private class GreeterServiceImpl implements GreeterService {

        private final static String STRANGER_NAME = "Stranger";
                
        public GreeterInfo greetMe(String name) throws GreeterException {
            System.out.println("Invoking: greetMe(" + name + ")");
            
            if (name.equals(STRANGER_NAME)) {
                throw new GreeterException(name);
            }

            GreeterInfo info = new GreeterInfo();
            List<GreetingPhrase> list = new ArrayList<GreetingPhrase>();
            list.add(new GreetingPhrase("Hello", name));
            list.add(new GreetingPhrase("Hoi", name));
            list.add(new GreetingPhrase("Hola", name));
            list.add(new GreetingPhrase("Bonjour", name));
            info.setGreetings(list);
            return info;
        }

    } 
    
    private Server startServer(String address, Class<?> type, Object impl) {
    	JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setServiceClass(type);
        factory.setAddress(address);
        factory.setResourceProvider(type, new SingletonResourceProvider(impl));
        List<Object> providers = new ArrayList<Object>(); 
	    providers.add(new AegisElementProvider());
	    factory.setProviders(providers);
        
	    return factory.create();
    }
    
}
