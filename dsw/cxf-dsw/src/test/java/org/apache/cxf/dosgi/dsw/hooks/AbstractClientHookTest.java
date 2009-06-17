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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServiceEndpointDescription;

public class AbstractClientHookTest extends TestCase {
    public void testOSGiRemoteProperty() throws Exception {
        IMocksControl control = EasyMock.createNiceControl();
        BundleContext bc = control.createMock(BundleContext.class);
        CxfDistributionProvider dp = control.createMock(CxfDistributionProvider.class);
        ServiceEndpointDescription sed = control.createMock(ServiceEndpointDescription.class);
        EasyMock.expect(sed.getProperties()).andReturn(new HashMap<String, Object>()).anyTimes();
        ConfigurationTypeHandler handler = control.createMock(ConfigurationTypeHandler.class);
        EasyMock.expect(handler.getType()).andReturn("test").anyTimes();
        control.replay();
                
        
        AbstractClientHook ch = new AbstractClientHook(bc, dp) {
            @Override
            protected String getIdentificationProperty() {
                return "ID";
            }            
        };
        Map<String, Object> props = ch.getProperties(sed, handler);
        assertTrue(Boolean.valueOf((String) props.get("service.imported")));
        assertEquals("test", props.get("service.imported.configs"));
    }
    
    public void testLookupDiscoveryServiceInterface() {
        ServiceRegistration sr = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.replay(sr);

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        AbstractClientHook ch = new AbstractClientHook(bc, null) {};
        // Put a mock service registration in there so we can check how its called
        ch.trackerRegistration = sr;

        ch.lookupDiscoveryService(null, null);
        EasyMock.verify(sr);
        
        // pass in an interface
        EasyMock.reset(sr);
        Dictionary d = new Hashtable();
        d.put(DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA, Arrays.asList("a"));
        sr.setProperties(d);
        EasyMock.replay(sr);
        
        ch.lookupDiscoveryService("a", null);
        EasyMock.verify(sr);

        // try again with the same value, should not trigger a callback since it's already there
        EasyMock.reset(sr);
        EasyMock.replay(sr);

        ch.lookupDiscoveryService("a", null);
        EasyMock.verify(sr);

        // pass in another interface
        EasyMock.reset(sr);
        d = new Hashtable();
        d.put(DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA, Arrays.asList("a", "b"));
        sr.setProperties(d);
        EasyMock.replay(sr);

        ch.lookupDiscoveryService("b", null);
        EasyMock.verify(sr);

        // try again with the same value, should not trigger a callback since it's already there
        EasyMock.reset(sr);
        EasyMock.replay(sr);

        ch.lookupDiscoveryService("a", null);
        ch.lookupDiscoveryService("b", null);
        EasyMock.verify(sr);
    }
    
    public void testLookupDiscoveryServiceFilter() {
        ServiceRegistration sr = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.replay(sr);

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        AbstractClientHook ch = new AbstractClientHook(bc, null) {};
        // Put a mock service registration in there so we can check how its called
        ch.trackerRegistration = sr;

        ch.lookupDiscoveryService(null, null);
        EasyMock.verify(sr);
        
        // pass in an interface
        EasyMock.reset(sr);
        Dictionary d = new Hashtable();
        d.put(DiscoveredServiceTracker.FILTER_MATCH_CRITERIA, Arrays.asList("a"));
        sr.setProperties(d);
        EasyMock.replay(sr);
        
        ch.lookupDiscoveryService(null, "a");
        EasyMock.verify(sr);

        // try again with the same value, should not trigger a callback since it's already there
        EasyMock.reset(sr);
        EasyMock.replay(sr);

        ch.lookupDiscoveryService(null, "a");
        EasyMock.verify(sr);

        // pass in another interface
        EasyMock.reset(sr);
        d = new Hashtable();
        d.put(DiscoveredServiceTracker.FILTER_MATCH_CRITERIA, Arrays.asList("a", "b"));
        sr.setProperties(d);
        EasyMock.replay(sr);

        ch.lookupDiscoveryService(null, "b");
        EasyMock.verify(sr);

        // try again with the same value, should not trigger a callback since it's already there
        EasyMock.reset(sr);
        EasyMock.replay(sr);

        ch.lookupDiscoveryService(null, "a");
        ch.lookupDiscoveryService(null, "b");
        EasyMock.verify(sr);
    }
    
    public void testLookupDiscoveryServiceBoth() {
        ServiceRegistration sr = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.replay(sr);

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        AbstractClientHook ch = new AbstractClientHook(bc, null) {};
        // Put a mock service registration in there so we can check how its called
        ch.trackerRegistration = sr;

        ch.lookupDiscoveryService(null, null);
        EasyMock.verify(sr);
        
        // pass in an interface
        EasyMock.reset(sr);
        Dictionary d = new Hashtable();
        d.put(DiscoveredServiceTracker.FILTER_MATCH_CRITERIA, Arrays.asList("a"));
        d.put(DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA, Arrays.asList("x"));
        sr.setProperties(d);
        EasyMock.replay(sr);
        
        ch.lookupDiscoveryService("x", "a");
        EasyMock.verify(sr);

        // try again with the same value, should not trigger a callback since it's already there
        EasyMock.reset(sr);
        EasyMock.replay(sr);

        ch.lookupDiscoveryService(null, "a");
        ch.lookupDiscoveryService("x", "a");
        EasyMock.verify(sr);
    }        
}
