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
package org.apache.cxf.dosgi.dsw.hooks;

import org.junit.Assert;
import org.junit.Test;

public class CxfFindListenerHookTest extends Assert {

    @Test
    public void testDUMMY() throws Exception {
        
    }
    
//    private IMocksControl control;
//    
//    @Before
//    public void setUp() {
//        control = EasyMock.createNiceControl();
//    }
    
    /* Todo this test doesn't apply at the moment since the ListenerHook doesn't
     * have a serviceReferencesRequested() API (yet).
    @Test
    public void testSyncListenerHook() throws Exception {
        
        Bundle bundle = control.createMock(Bundle.class);
        bundle.findEntries(EasyMock.eq("OSGI-INF/remote-service"), 
            EasyMock.eq("*.xml"), EasyMock.anyBoolean());
        EasyMock.expectLastCall().andReturn(Collections.enumeration(
            Arrays.asList(getClass().getResource("/OSGI-INF/remote-service/remote-services.xml"))));
        Dictionary<String, String> bundleHeaders = new Hashtable<String, String>();
        bundleHeaders.put(org.osgi.framework.Constants.BUNDLE_NAME, 
                          "Test Bundle");
        bundleHeaders.put(org.osgi.framework.Constants.BUNDLE_VERSION, 
                          "1.0.0");
        bundle.getHeaders();
        EasyMock.expectLastCall().andReturn(bundleHeaders).anyTimes();
        bundle.loadClass(TestService.class.getName());
        EasyMock.expectLastCall().andReturn(TestService.class).anyTimes();
        final BundleContext requestingContext = control.createMock(BundleContext.class);
        requestingContext.getBundle();
        EasyMock.expectLastCall().andReturn(bundle).anyTimes();
        
        BundleTestContext dswContext = new BundleTestContext(bundle);
        dswContext.addServiceReference(TestService.class.getName(), 
                                       control.createMock(ServiceReference.class));
        control.replay();
     
        CxfListenerHook hook = new CxfListenerHook(dswContext, null);
        
        // TODO : if the next call ends up being executed in a thread of its own then
        // update the test accordingly, use Futures for ex
        
        hook.serviceReferencesRequested(requestingContext, 
                                       TestService.class.getName(), null, true);
        
        List<ServiceReference> registeredRefs = dswContext.getRegisteredReferences();
        assertNotNull(registeredRefs);
        assertEquals(1, registeredRefs.size());        
    } */
    
//    @Test
//    public void testTrackerPropertiesOnlyClassInFilterWithMatchingInterface() throws Exception {
//        String filter = "(objectClass=" + TestService.class.getName() + ")";
//        doTestTrackerPropertiesSet(filter,
//                                   "osgi.remote.discovery.interest.interfaces",
//                                   TestService.class.getName(),
//                                   asList(TestService.class.getName()),
//                                   Collections.EMPTY_SET);
//    }
//
//    @Test
//    public void testTrackerPropertiesGenericFilterWithMatchingInterface() throws Exception {
//        String filter = "(&(objectClass=" + TestService.class.getName() 
//                        + ")(colour=blue))";
//        doTestTrackerPropertiesSet(filter,
//                                   "osgi.remote.discovery.interest.filters",
//                                   replacePredicate(filter),
//                                   asList(TestService.class.getName()),
//                                   Collections.EMPTY_SET);
//    }
//
//    @Test
//    public void testTrackerPropertiesOnlyClassInFilterWithMatchingFilter() throws Exception {
//        String filter = "(objectClass=" + TestService.class.getName() + ")";
//        doTestTrackerPropertiesSet(filter,
//                                   "osgi.remote.discovery.interest.interfaces",
//                                   TestService.class.getName(),
//                                   Collections.EMPTY_SET,
//                                   asList(replacePredicate(filter)));
//    }
//
//    @Test
//    public void testTrackerPropertiesGenericFilterWithMatchingFilter() throws Exception {
//        String filter = "(&(objectClass=" + TestService.class.getName() 
//                        + ")(colour=blue))";
//        doTestTrackerPropertiesSet(filter,
//                                   "osgi.remote.discovery.interest.filters",
//                                   replacePredicate(filter),
//                                   Collections.EMPTY_SET,
//                                   asList(replacePredicate(filter)));
//    }
//
//    @Test
//    public void testTrackerPropertiesOnlyClassInFilterWithMatchingBoth() throws Exception {
//        String filter = "(objectClass=" + TestService.class.getName() + ")";
//        doTestTrackerPropertiesSet(filter,
//                                   "osgi.remote.discovery.interest.interfaces",
//                                   TestService.class.getName(),
//                                   asList(TestService.class.getName()),
//                                   asList(replacePredicate(filter)));
//    }
//
//    @Test
//    public void testTrackerPropertiesGenericFilterWithMatchingBoth() throws Exception {
//        String filter = "(&(objectClass=" + TestService.class.getName() 
//                        + ")(colour=blue))";
//        doTestTrackerPropertiesSet(filter,
//                                   "osgi.remote.discovery.interest.filters",
//                                   replacePredicate(filter),
//                                   Collections.EMPTY_SET,
//                                   asList(replacePredicate(filter)));
//    }
//
//    private void doTestTrackerPropertiesSet(final String filter,
//                                            String propKey,
//                                            String propValue,
//                                            Collection matchingInterfaces,
//                                            Collection matchingFilters) throws Exception {
//        Bundle bundle = control.createMock(Bundle.class);
//        Dictionary<String, String> bundleHeaders = 
//            new Hashtable<String, String>();
//        bundleHeaders.put(org.osgi.framework.Constants.BUNDLE_NAME, 
//                          "Test Bundle");
//        bundleHeaders.put(org.osgi.framework.Constants.BUNDLE_VERSION, 
//                          "1.0.0");
//        bundle.getHeaders();
//        EasyMock.expectLastCall().andReturn(bundleHeaders).times(2);
//        final String serviceClass = TestService.class.getName();
//        bundle.loadClass(serviceClass);
//        EasyMock.expectLastCall().andReturn(TestService.class).times(2);
//        final BundleContext requestingContext = 
//            control.createMock(BundleContext.class);
//        
//        BundleTestContext dswContext = new BundleTestContext(bundle);
//        ServiceRegistration serviceRegistration =
//            control.createMock(ServiceRegistration.class);
//        dswContext.addServiceRegistration(serviceClass, serviceRegistration);
//        serviceRegistration.unregister();
//        EasyMock.expectLastCall().times(1);
//        ServiceReference serviceReference = 
//            control.createMock(ServiceReference.class);
//        dswContext.addServiceReference(serviceClass, serviceReference);
//
//        final String trackerClass = DiscoveredServiceTracker.class.getName();
//        ServiceRegistration trackerRegistration =
//            control.createMock(ServiceRegistration.class);
//        dswContext.addServiceRegistration(trackerClass, trackerRegistration);
//        ServiceReference trackerReference = 
//            control.createMock(ServiceReference.class);
//        dswContext.addServiceReference(trackerClass, trackerReference);
//
//        List property = asList(propValue);
//        Dictionary properties = new Hashtable();
//        properties.put(propKey, property);
//        trackerRegistration.setProperties(properties);
//        EasyMock.expectLastCall();
//
//        if (matchingInterfaces.size() == 0 && matchingFilters.size() > 0) {
//            Iterator filters = matchingFilters.iterator();
//            while (filters.hasNext()) {
//                Filter f = control.createMock(Filter.class);
//                dswContext.addFilter((String)filters.next(), f);
//                f.match(EasyMock.isA(Dictionary.class));
//                EasyMock.expectLastCall().andReturn(true);
//            }
//        } 
//
//        control.replay();
//     
//        CxfFindListenerHook hook = new CxfFindListenerHook(dswContext, null);
//
//        ListenerHook.ListenerInfo info = new ListenerHook.ListenerInfo() {
//            public BundleContext getBundleContext() {
//                return requestingContext;
//            }
//
//            public String getFilter() {
//                return filter;
//            }
//
//            public boolean isRemoved() {
//                return false;
//            }            
//        };
//        hook.added(Collections.singleton(info));
//
//        DiscoveredServiceTracker tracker = (DiscoveredServiceTracker)
//            dswContext.getService(trackerReference);
//        assertNotNull(tracker);
//
//        Collection interfaces = asList(serviceClass);
//
//        notifyAvailable(tracker, matchingInterfaces, matchingFilters, "1234");
//        notifyAvailable(tracker, matchingInterfaces, matchingFilters, "5678");
//        notifyAvailable(tracker, matchingInterfaces, matchingFilters, "1234");
//        
//        notifyUnAvailable(tracker, "1234");
//        notifyUnAvailable(tracker, "5678");
//
//        notifyAvailable(tracker, matchingInterfaces, matchingFilters , "1234");
//
//        control.verify();
//
//        Map<String, ServiceReference> registeredRefs = 
//            dswContext.getRegisteredReferences();
//        assertNotNull(registeredRefs);
//        assertEquals(2, registeredRefs.size());
//        assertNotNull(registeredRefs.get(serviceClass));
//        assertSame(serviceReference, registeredRefs.get(serviceClass));
//
//        Map<String, ServiceRegistration> registeredRegs = 
//            dswContext.getRegisteredRegistrations();
//        assertNotNull(registeredRegs);
//        assertEquals(2, registeredRegs.size());
//        assertNotNull(registeredRegs.get(trackerClass));
//        assertSame(trackerRegistration, registeredRegs.get(trackerClass));
//
//        List<Object> registeredServices = dswContext.getRegisteredServices();
//        assertNotNull(registeredServices);
//        assertEquals(2, registeredServices.size());
//    } 
//
//    @Test
//    public void testConstructorAndGetters() {
//        BundleContext bc = control.createMock(BundleContext.class);
//        CxfRemoteServiceAdmin dp = control.createMock(CxfRemoteServiceAdmin.class);
//        control.replay();
//        
//        CxfFindListenerHook clh = new CxfFindListenerHook(bc, dp);
//        assertSame(bc, clh.getContext());
//        assertSame(dp, clh.getDistributionProvider());
//    }
//
//    @Test
//    public void testFindHook() {
//        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
//        
//        final List<String> lookupCalls = new ArrayList<String>();        
//        CxfFindListenerHook fh = new CxfFindListenerHook(bc, null) {
//            @Override
//            protected synchronized void lookupDiscoveryService(
//                    String interfaceName, String filterValue) {
//                lookupCalls.add(interfaceName);
//                lookupCalls.add(filterValue);
//            }            
//        };
//        
//        String clazz = "my.app.Class";
//        String filter = "&(A=B)(C=D)";
//        fh.find(null, clazz, filter, true, null);
//        
//        assertEquals(Arrays.asList(clazz, filter), lookupCalls);
//    }
//    
//    private void notifyAvailable(DiscoveredServiceTracker tracker,
//                                 Collection interfaces,
//                                 Collection filters, 
//                                 String endpointId) {
//        Map<String, Object> props = new Hashtable<String, Object>();
//        props.put("osgi.remote.interfaces", "*");
//        props.put("osgi.remote.endpoint.id", endpointId);
//        tracker.serviceChanged(new Notification(AVAILABLE,
//                                                TestService.class.getName(),
//                                                interfaces,
//                                                filters, 
//                                                props));
//    }
//
//    private void notifyUnAvailable(DiscoveredServiceTracker tracker, 
//                                   String endpointId) {
//        Map<String, Object> props = new Hashtable<String, Object>();
//        props.put("osgi.remote.endpoint.id", endpointId);
//        tracker.serviceChanged(new Notification(UNAVAILABLE,
//                                                TestService.class.getName(),
//                                                Collections.EMPTY_SET,
//                                                Collections.EMPTY_SET,
//                                                props));
//    }
//
//    private List<String> asList(String s) {
//        List l = new ArrayList<String>();
//        l.add(s);
//        return l;
//    }
//
//    private String replacePredicate(String filter) {
//        return filter.replace("objectClass", ServicePublication.SERVICE_INTERFACE_NAME);
//    }
//
//    private class Notification implements DiscoveredServiceNotification {
//        private int type;
//        private ServiceEndpointDescription sed;
//        private Collection interfaces;
//        private Collection filters;
//
//        Notification(int type, 
//                     String interfaceName,
//                     Collection interfaces,
//                     Collection filters, 
//                     Map<String, Object> props) {
//            this.type = type;
//            this.sed = 
//                new ServiceEndpointDescriptionImpl(interfaceName, props);
//            this.interfaces = interfaces;
//            this.filters = filters;
//        }
//
//        public int getType() {
//            return type;
//        }
//
//        public ServiceEndpointDescription getServiceEndpointDescription() {
//            return sed;
//        }
//
//        public Collection getInterfaces() {
//            return interfaces; 
//        }
//
//        public Collection getFilters() {
//            return filters; 
//        }
//    }

}
