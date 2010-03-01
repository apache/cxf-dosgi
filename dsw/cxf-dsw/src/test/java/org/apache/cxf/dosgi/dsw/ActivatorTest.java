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
package org.apache.cxf.dosgi.dsw;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class ActivatorTest extends TestCase{
    private BundleContext getMockBundleContext(IMocksControl control) {
        
        Bundle b = control.createMock(Bundle.class);
        Hashtable<String, String> ht = new Hashtable<String, String>();
        EasyMock.expect(b.getHeaders()).andReturn(ht).anyTimes();        
        BundleContext bc = control.createMock(BundleContext.class);

        EasyMock.expect(b.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        //        EasyMock.expect(bc.registerService(
        //            (String) EasyMock.anyObject(), EasyMock.anyObject(), 
        //            (Dictionary) EasyMock.anyObject())).andAnswer(new IAnswer<ServiceRegistration>() {
        //                public ServiceRegistration answer() throws Throwable {
        //                    ServiceRegistration sr = EasyMock.createNiceMock(ServiceRegistration.class);
        //                    EasyMock.replay(sr);
        //                    return sr;
        //                }                
        //            }).anyTimes();
        return bc;
    }
    
    
    
    
    public void testCreateAndShutdownRemoteServiceAdminService() throws Exception {
        
       //IntentMap im = new IntentMap();
       //im.setIntents(new HashMap<String, Object>());

       // IntentMapLoader.initializeWithExistingMap(im);
        
        IMocksControl control = EasyMock.createNiceControl();
        BundleContext bc = getMockBundleContext(control);
        
        ServiceRegistration sr = control.createMock(ServiceRegistration.class);
        EasyMock.expect(bc.registerService(EasyMock.eq(RemoteServiceAdmin.class.getName()),EasyMock.anyObject(), (Dictionary)EasyMock.anyObject())).andReturn(sr).atLeastOnce();
        
        
        control.replay();
        
        
        Activator a = new Activator();   
        a.start(bc);
        
        control.verify();
        
    }
    
//    @SuppressWarnings("unchecked")
//    public void testCreateDistributionProviderService() throws Exception {
//        IMocksControl control = EasyMock.createNiceControl();
//        
//        Bundle b = control.createMock(Bundle.class);
//        Hashtable<String, String> ht = new Hashtable<String, String>();
//        EasyMock.expect(b.getHeaders()).andReturn(ht).anyTimes();
//        
//        final Map<Object, Dictionary> services = new HashMap<Object, Dictionary>();
//        BundleContext bc = control.createMock(BundleContext.class);
//        EasyMock.expect(bc.registerService(
//            (String) EasyMock.anyObject(),
//            EasyMock.anyObject(), 
//            (Dictionary) EasyMock.anyObject())).andAnswer(new IAnswer<ServiceRegistration>() {
//                public ServiceRegistration answer() throws Throwable {
//                    services.put(EasyMock.getCurrentArguments()[1],
//                        (Dictionary) EasyMock.getCurrentArguments()[2]);
//                    return null;
//                }                
//            }).anyTimes();            
//        EasyMock.expect(bc.registerService(
//            (String []) EasyMock.anyObject(),
//            EasyMock.anyObject(), 
//            (Dictionary) EasyMock.anyObject())).andAnswer(new IAnswer<ServiceRegistration>() {
//                public ServiceRegistration answer() throws Throwable {
//                    services.put(EasyMock.getCurrentArguments()[1],
//                        (Dictionary) EasyMock.getCurrentArguments()[2]);
//                    return null;
//                }                
//            }).anyTimes();            
//
//        EasyMock.expect(b.getBundleContext()).andReturn(bc).anyTimes();
//        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
//        control.replay();
//        
//        Activator a = new Activator() {
//            @Override
//            IntentMap getIntentMap() {
//                Map<String, Object> intents = new HashMap<String, Object>();
//                intents.put("A", new AbstractFeature() {});
//                intents.put("B", "PROVIDED");
//
//                IntentMap im = new IntentMap();
//                im.setIntents(intents);
//                return im;
//            }            
//        };
//        
//        assertEquals("Precondition failed", 0, services.size());
//        a.start(bc);
//        
//        CxfDistributionProvider dp = null;
//        for (Object o : services.keySet()) {
//            if (o instanceof CxfDistributionProvider) {
//                dp = ((CxfDistributionProvider) o);
//            }
//        }
//        
//        Dictionary serviceProps = services.get(dp);
//        Set<String> expected = new HashSet<String>(Arrays.asList("A", "B"));
//        assertEquals(expected, new HashSet<String>(Arrays.asList(
//            OsgiUtils.parseIntents((String) serviceProps.get(DistributionProvider.SUPPORTED_INTENTS)))));
//        assertNotNull(serviceProps.get(DistributionProvider.PRODUCT_NAME));
//        assertNotNull(serviceProps.get(DistributionProvider.PRODUCT_VERSION));
//        assertNotNull(serviceProps.get(DistributionProvider.VENDOR_NAME));
//    }
    
    // TODO: move to controller
    
//    public void testPublishPreexistingServices() throws Exception {
//        TestService serviceObject = new TestServiceImpl();
//
//        IMocksControl control = EasyMock.createNiceControl();
//        
//        Bundle b = control.createMock(Bundle.class);
//        Hashtable<String, String> ht = new Hashtable<String, String>();
//        EasyMock.expect(b.getHeaders()).andReturn(ht).anyTimes();
//        
//        BundleContext bc = control.createMock(BundleContext.class);       
//        
//        ServiceReference sref = control.createMock(ServiceReference.class);
//        EasyMock.expect(sref.getBundle()).andReturn(b).anyTimes();
//        EasyMock.expect(sref.getProperty(org.osgi.framework.Constants.OBJECTCLASS)).
//            andReturn(new String [] {TestService.class.getName()}).anyTimes();
//        EasyMock.expect(sref.getPropertyKeys()).
//            andReturn(new String [] {"service.exported.interfaces"}).anyTimes();
//        EasyMock.expect(sref.getProperty("service.exported.interfaces")).
//            andReturn("*").anyTimes();
//
//        EasyMock.expect(bc.getServiceReferences(null, 
//            "(|(service.exported.interfaces=*)(osgi.remote.interfaces=*))")).
//            andReturn(new ServiceReference[] {sref}).anyTimes();
//        EasyMock.expect(bc.getService(sref)).andReturn(serviceObject).anyTimes();
//
//        EasyMock.expect(b.getBundleContext()).andReturn(bc).anyTimes();
//        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
//        control.replay();
//        
//        Activator a = new Activator() {
//            @Override
//            IntentMap getIntentMap() {
//                IntentMap intentMap = new IntentMap();
//                intentMap.setIntents(new HashMap<String, Object>());
//                return intentMap;                
//            }            
//        };   
//        
//        a.start(bc);
//        assertEquals(1, a.pHook.getEndpoints().size());
//        ServiceReference key = a.pHook.getEndpoints().keySet().iterator().next();
//        assertSame(sref, key);
//        
//    }
    
//    public void testListenerHookRegistered() throws Exception {
//        testServiceRegistered(ListenerHook.class);
//    } 
//
//    public void testFindHookRegistered() throws Exception {
//        testServiceRegistered(FindHook.class);
//    } 
    
//    public void testDiscoveredServiceTrackerRegistered() throws Exception {
//        testServiceRegistered(DiscoveredServiceTracker.class);
//    } 
//
//    
//    public void testRemoteServiceAdminRegistered() throws Exception {
//        testServiceRegistered(RemoteServiceAdmin.class);
//    } 
//    
//    
//    private void testServiceRegistered(Class serviceClass) throws Exception {
//        IMocksControl control = EasyMock.createNiceControl();
//        
//        Bundle b = control.createMock(Bundle.class);
//        Hashtable<String, String> ht = new Hashtable<String, String>();
//        EasyMock.expect(b.getHeaders()).andReturn(ht).anyTimes();
//
//        final Map<Object, Dictionary> services = new HashMap<Object, Dictionary>();
//        BundleContext bc = control.createMock(BundleContext.class);
//        EasyMock.expect(bc.registerService(
//            (String) EasyMock.anyObject(),
//            EasyMock.anyObject(), 
//            (Dictionary) EasyMock.anyObject())).andAnswer(new IAnswer<ServiceRegistration>() {
//                public ServiceRegistration answer() throws Throwable {
//                    services.put(EasyMock.getCurrentArguments()[1],
//                        (Dictionary) EasyMock.getCurrentArguments()[2]);
//                    return null;
//                }                
//            }).anyTimes();            
//        EasyMock.expect(bc.registerService(
//            (String []) EasyMock.anyObject(),
//            EasyMock.anyObject(), 
//            (Dictionary) EasyMock.anyObject())).andAnswer(new IAnswer<ServiceRegistration>() {
//                public ServiceRegistration answer() throws Throwable {
//                    services.put(EasyMock.getCurrentArguments()[1],
//                        (Dictionary) EasyMock.getCurrentArguments()[2]);
//                    return null;
//                }                
//            }).anyTimes();            
//
//        EasyMock.expect(b.getBundleContext()).andReturn(bc).anyTimes();
//        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
//        control.replay();
//
//        Activator a = new Activator();
//        a.start(bc);
//        
//        for (Object svc : services.keySet()) {
//            if (serviceClass.isAssignableFrom(svc.getClass())) {
//                return;
//            }
//        }
//        fail("Should have a service registered of type: " + serviceClass);
//    }
//
//    private static class TestServiceImpl implements TestService {}    
}
