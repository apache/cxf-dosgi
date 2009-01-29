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

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;

import org.osgi.service.discovery.DiscoveredServiceTracker;

public class CxfListenerHookTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }
    
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
    
    //@Test
    public void testListenerHook() throws Exception {
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
        final String serviceClass = TestService.class.getName();
        bundle.loadClass(serviceClass);
        EasyMock.expectLastCall().andReturn(TestService.class).anyTimes();
        final BundleContext requestingContext = control.createMock(BundleContext.class);
        requestingContext.getBundle();
        EasyMock.expectLastCall().andReturn(bundle).anyTimes();
        
        BundleTestContext dswContext = new BundleTestContext(bundle);
	ServiceReference reference = control.createMock(ServiceReference.class);
        dswContext.addServiceReference(serviceClass, reference);

        final String trackerClass = DiscoveredServiceTracker.class.getName();
        ServiceRegistration registration =
            control.createMock(ServiceRegistration.class);
        dswContext.addServiceRegistration(trackerClass, registration);
        registration.setProperties(EasyMock.isA(Dictionary.class));
        EasyMock.expectLastCall();

        control.replay();
     
        CxfListenerHook hook = new CxfListenerHook(dswContext, null);

        ListenerHook.ListenerInfo info = new ListenerHook.ListenerInfo() {
            public BundleContext getBundleContext() {
                return requestingContext;
            }

            public String getFilter() {
                return "(objectClass=" + serviceClass + ")";
            }            
        };
        hook.added(Collections.singleton(info));
        
        Map<String, ServiceReference> registeredRefs = 
            dswContext.getRegisteredReferences();
        assertNotNull(registeredRefs);
        assertEquals(1, registeredRefs.size());
        assertNotNull(registeredRefs.get(serviceClass));
        assertSame(reference, registeredRefs.get(serviceClass));

        Map<String, ServiceRegistration> registeredRegs = 
            dswContext.getRegisteredRegistrations();
        assertNotNull(registeredRegs);
        assertEquals(1, registeredRegs.size());
        assertNotNull(registeredRegs.get(trackerClass));
        assertSame(registration, registeredRegs.get(trackerClass));
    } 

    @Test
    public void testConstructorAndGetters() {
        BundleContext bc = control.createMock(BundleContext.class);
        CxfDistributionProvider dp = control.createMock(CxfDistributionProvider.class);
        control.replay();
        
        CxfListenerHook clh = new CxfListenerHook(bc, dp);
        assertSame(bc, clh.getContext());
        assertSame(dp, clh.getDistributionProvider());
    }

}
