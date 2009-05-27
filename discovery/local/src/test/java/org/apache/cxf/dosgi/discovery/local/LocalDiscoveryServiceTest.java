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
package org.apache.cxf.dosgi.discovery.local;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.discovery.ServicePublication;

public class LocalDiscoveryServiceTest extends TestCase {
    public void testLocalDiscoveryServiceDSTInterface() {
        URL rsA = getClass().getResource("/rs_a.xml");
        Enumeration<URL> rsAEnum = Collections.enumeration(Arrays.asList(rsA));
        URL rsB = getClass().getResource("/rs_b.xml");
        Enumeration<URL> rsBEnum = Collections.enumeration(Arrays.asList(rsB));
        URL rsC = getClass().getResource("/rs_c.xml");
        Enumeration<URL> rsCEnum = Collections.enumeration(Arrays.asList(rsC));
        
        // Set up some mock objects
        IMocksControl control = EasyMock.createNiceControl();
        Bundle b0 = control.createMock(Bundle.class);
        EasyMock.expect(b0.getState()).andReturn(Bundle.ACTIVE).anyTimes();
        EasyMock.expect(b0.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                andReturn(rsAEnum).anyTimes();
        
        BundleContext bc = control.createMock(BundleContext.class);
        ServiceReference sr = control.createMock(ServiceReference.class);
        TestDiscoveredServiceTracker dst = new TestDiscoveredServiceTracker();

        EasyMock.expect(bc.getBundles()).andReturn(new Bundle[] {b0}).anyTimes();
        EasyMock.expect(sr.getProperty(DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA)).
                andReturn(Collections.singleton("org.example.SomeService")).anyTimes();
        
        EasyMock.expect(bc.getService(sr)).andReturn(dst).anyTimes();
        control.replay();

        // create the local discovery service
        LocalDiscoveryService lds = new LocalDiscoveryService(bc);
        
        // it should be prepopulated with the info from bundle b0
        assertEquals(2, lds.servicesInfo.size());
        Map<Collection<String>, String> eids = getEndpointIDs(lds.servicesInfo.keySet());

        List<String> intf1 = Arrays.asList("org.example.SomeService");
        Map<String, Object> sed1Props = new HashMap<String, Object>();
        sed1Props.put("osgi.remote.requires.intents", "confidentiality");
        sed1Props.put(ServicePublication.SERVICE_INTERFACE_NAME, intf1);
        setEndpointID(eids, sed1Props, "org.example.SomeService");
        ServiceEndpointDescription sed1 = new ServiceEndpointDescriptionImpl(intf1, sed1Props);
        List<String> intf2 = Arrays.asList("SomeOtherService", "WithSomeSecondInterface");
        Map<String, Object> sed2Props = new HashMap<String, Object>();
        sed2Props.put(ServicePublication.SERVICE_INTERFACE_NAME, intf2);
        setEndpointID(eids, sed2Props, "SomeOtherService", "WithSomeSecondInterface");       
        ServiceEndpointDescription sed2 = new ServiceEndpointDescriptionImpl(intf2, sed2Props);
        assertTrue(lds.servicesInfo.containsKey(sed1));
        assertTrue(lds.servicesInfo.containsKey(sed2));
        
        // should be pre-populated by now...
        // register a tracker, it should get called back instantly with sed1
        assertEquals("Precondition failed", 0, dst.notifications.size());
        lds.trackerTracker.addingService(sr);
        assertEquals(1, dst.notifications.size());
        DiscoveredServiceNotification dsn = dst.notifications.iterator().next();
        assertEquals(DiscoveredServiceNotification.AVAILABLE, dsn.getType());
        assertEquals(sed1, dsn.getServiceEndpointDescription());
        verifyNotification(dsn, 0, 1, "org.example.SomeService");
        
        // add a new bundle that also contains a someservice 
        // we should get notified again...
        Bundle b1 = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b1.getState()).andReturn(Bundle.ACTIVE).anyTimes();
        EasyMock.expect(b1.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                andReturn(rsBEnum).anyTimes();
        EasyMock.replay(b1);

        // Call back on the LDS just like what would have happened with the BundleListener 
        BundleEvent be = new BundleEvent(BundleEvent.STARTED, b1);
        lds.bundleChanged(be);
        Map<Collection<String>, String> eids2 = getEndpointIDs(lds.servicesInfo.keySet());
        
        List<String> intf3 = Arrays.asList("org.example.SomeRelatedService", "org.example.SomeService");
        Map<String, Object> sed3Props = new HashMap<String, Object>();
        sed3Props.put(ServicePublication.SERVICE_INTERFACE_NAME, intf3);
        setEndpointID(eids2, sed3Props, "org.example.SomeRelatedService", "org.example.SomeService");       
        ServiceEndpointDescription sed3 = new ServiceEndpointDescriptionImpl(intf3, sed3Props);
        assertEquals(3, lds.servicesInfo.size());
        assertTrue(lds.servicesInfo.containsKey(sed1));
        assertTrue(lds.servicesInfo.containsKey(sed2));
        assertTrue(lds.servicesInfo.containsKey(sed3));
        
        assertEquals("We should have been notified of the new bundle", 
                2, dst.notifications.size());
        assertEquals(DiscoveredServiceNotification.AVAILABLE, 
                dst.notifications.get(1).getType());
        assertEquals(sed3, dst.notifications.get(1).getServiceEndpointDescription());
        verifyNotification(dst.notifications.get(1), 0, 1, "org.example.SomeService");

        ArrayList<DiscoveredServiceNotification> copiedNotifications = 
                new ArrayList<DiscoveredServiceNotification>(dst.notifications);
        
        // add an unrelated bundle - no notification...
        Bundle b2 = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b2.getState()).andReturn(Bundle.ACTIVE).anyTimes();
        EasyMock.expect(b2.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                andReturn(rsCEnum).anyTimes();
        EasyMock.replay(b2);
        
        BundleEvent be2 = new BundleEvent(BundleEvent.STARTED, b1);
        lds.bundleChanged(be2);
        assertEquals("There should not have been any extra notification", 
                copiedNotifications, dst.notifications);
        
        // Send an irrelevant bundle event
        BundleEvent be3 = new BundleEvent(BundleEvent.LAZY_ACTIVATION, b0);        
        lds.bundleChanged(be3);
        assertEquals("There should not have been any changes to the registered serviceInfos",
                3, lds.servicesInfo.size());
        assertEquals("There should not have been any extra notification", 
                copiedNotifications, dst.notifications);
               
        // remove bundle b0
        BundleEvent be4 = new BundleEvent(BundleEvent.STOPPING, b0);        
        lds.bundleChanged(be4);
        
        assertEquals(1, lds.servicesInfo.size());
        assertEquals(sed3, lds.servicesInfo.keySet().iterator().next());
        
        assertEquals(3, dst.notifications.size()); 
        assertEquals(DiscoveredServiceNotification.UNAVAILABLE, 
                dst.notifications.get(2).getType());
        assertEquals(sed1, dst.notifications.get(2).getServiceEndpointDescription());
        verifyNotification(dst.notifications.get(2), 0, 1, "org.example.SomeService");

        // now remove the tracker itself...
        assertEquals("Precondition failed", 1, lds.interfacesToTrackers.size());
        assertEquals("Precondition failed", 1, lds.trackersToInterfaces.size());
        assertEquals("Precondition failed", 1, lds.interfacesToTrackers.values().iterator().next().size());
        assertEquals("Precondition failed", 1, lds.trackersToInterfaces.values().iterator().next().size());
        lds.trackerTracker.removedService(sr, dst);
        assertEquals("Tracker should have been removed", 0, lds.interfacesToTrackers.values().iterator().next().size());
        assertEquals("Tracker should have been removed", 0, lds.trackersToInterfaces.size());
    }

    private void setEndpointID(Map<Collection<String>, String> eids,
            Map<String, Object> props, String ... interfaces) {
        props.put(ServicePublication.ENDPOINT_ID, eids.get(
                new HashSet<String>(Arrays.asList(interfaces))));
    }
    
    public void testLocalDiscoveryServiceDSTFilter() throws Exception {
        LocalDiscoveryUtils.addEndpointID = false;
        
        try {
            URL rsA = getClass().getResource("/rs_d.xml");
            Enumeration<URL> rsAEnum = Collections.enumeration(Arrays.asList(rsA));
            URL rsB = getClass().getResource("/rs_e.xml");
            Enumeration<URL> rsBEnum = Collections.enumeration(Arrays.asList(rsB));
            URL rsC = getClass().getResource("/rs_f.xml");
            Enumeration<URL> rsCEnum = Collections.enumeration(Arrays.asList(rsC));
            
            // Set up some mock objects
            IMocksControl control = EasyMock.createNiceControl();
            Bundle b0 = control.createMock(Bundle.class);
            EasyMock.expect(b0.getState()).andReturn(Bundle.ACTIVE).anyTimes();
            EasyMock.expect(b0.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                    andReturn(rsAEnum).anyTimes();
            
            BundleContext bc = control.createMock(BundleContext.class);
            Filter mockFilter = control.createMock(Filter.class);
            ServiceReference sr = control.createMock(ServiceReference.class);
            TestDiscoveredServiceTracker dst = new TestDiscoveredServiceTracker();
    
            EasyMock.expect(bc.getBundles()).andReturn(new Bundle[] {b0}).anyTimes();
            EasyMock.expect(bc.createFilter("(blah <= 5)")).andReturn(mockFilter).anyTimes();
            EasyMock.expect(sr.getProperty(DiscoveredServiceTracker.FILTER_MATCH_CRITERIA)).
                    andReturn(Collections.singleton("(blah <= 5)")).anyTimes();
            
            EasyMock.expect(bc.getService(sr)).andReturn(dst).anyTimes();
            
            // set up the mock filter behaviour
            Dictionary<String, Object> d1 = new Hashtable<String, Object>();
            d1.put("blah", "5");
            d1.put("osgi.remote.service.interfaces", 
                    Arrays.asList("SomeOtherService", "WithSomeSecondInterface"));
            EasyMock.expect(mockFilter.match(d1)).andReturn(true).anyTimes();
            Dictionary<String, Object> d2 = new Hashtable<String, Object>();
            d2.put("blah", "3");
            d2.put("boo", "hello");
            d2.put("osgi.remote.service.interfaces", 
                    Arrays.asList("org.example.SomeRelatedService", "org.example.SomeService"));
            EasyMock.expect(mockFilter.match(d2)).andReturn(true).anyTimes();
            
            control.replay();
    
            // create the local discovery service
            LocalDiscoveryService lds = new LocalDiscoveryService(bc);
            
            // it should be prepopulated with the info from bundle b0
            assertEquals(2, lds.servicesInfo.size());
    
            List<String> intf1 = Arrays.asList("org.example.SomeService");
            Map<String, Object> sed1Props = new HashMap<String, Object>();
            sed1Props.put("osgi.remote.requires.intents", "confidentiality");
            sed1Props.put(ServicePublication.SERVICE_INTERFACE_NAME, intf1);
            ServiceEndpointDescription sed1 = new ServiceEndpointDescriptionImpl(intf1, sed1Props);
            List<String> intf2 = Arrays.asList("SomeOtherService", "WithSomeSecondInterface");
            Map<String, Object> sed2Props = new HashMap<String, Object>();
            sed2Props.put("blah", "5");
            sed2Props.put(ServicePublication.SERVICE_INTERFACE_NAME, intf2);
            ServiceEndpointDescription sed2 = new ServiceEndpointDescriptionImpl(intf2, sed2Props);
            assertTrue(lds.servicesInfo.containsKey(sed1));
            assertTrue(lds.servicesInfo.containsKey(sed2));
            
            // should be pre-populated by now...
            // register a tracker, it should get called back instantly with sed1
            assertEquals("Precondition failed", 0, dst.notifications.size());
            lds.trackerTracker.addingService(sr);
            assertEquals(1, dst.notifications.size());
            DiscoveredServiceNotification dsn = dst.notifications.iterator().next();
            assertEquals(DiscoveredServiceNotification.AVAILABLE, dsn.getType());
            assertEquals(sed2, dsn.getServiceEndpointDescription());
            verifyNotification(dsn, 1, 0, "(blah <= 5)");
            
            // add a new bundle that also contains a someservice 
            // we should get notified again...
            Bundle b1 = EasyMock.createNiceMock(Bundle.class);
            EasyMock.expect(b1.getState()).andReturn(Bundle.ACTIVE).anyTimes();
            EasyMock.expect(b1.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                    andReturn(rsBEnum).anyTimes();
            EasyMock.replay(b1);
    
            // Call back on the LDS just like what would have happened with the BundleListener 
            BundleEvent be = new BundleEvent(BundleEvent.STARTED, b1);
            lds.bundleChanged(be);
            
            List<String> intf3 = Arrays.asList("org.example.SomeRelatedService", "org.example.SomeService");
            Map<String, Object> sed3Props = new HashMap<String, Object>();
            sed3Props.put("blah", "3");
            sed3Props.put("boo", "hello");
            sed3Props.put(ServicePublication.SERVICE_INTERFACE_NAME, intf3);
            ServiceEndpointDescription sed3 = new ServiceEndpointDescriptionImpl(intf3, sed3Props);
            assertEquals(3, lds.servicesInfo.size());
            assertTrue(lds.servicesInfo.containsKey(sed1));
            assertTrue(lds.servicesInfo.containsKey(sed2));
            assertTrue(lds.servicesInfo.containsKey(sed3));
            
            assertEquals("We should have been notified of the new bundle", 
                    2, dst.notifications.size());
            assertEquals(DiscoveredServiceNotification.AVAILABLE, 
                    dst.notifications.get(1).getType());
            assertEquals(sed3, dst.notifications.get(1).getServiceEndpointDescription());
            verifyNotification(dsn, 1, 0, "(blah <= 5)");
            
            ArrayList<DiscoveredServiceNotification> copiedNotifications = 
                    new ArrayList<DiscoveredServiceNotification>(dst.notifications);
            
            // add an unrelated bundle - no notification...
            Bundle b2 = EasyMock.createNiceMock(Bundle.class);
            EasyMock.expect(b2.getState()).andReturn(Bundle.ACTIVE).anyTimes();
            EasyMock.expect(b2.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                    andReturn(rsCEnum).anyTimes();
            EasyMock.replay(b2);
            
            BundleEvent be2 = new BundleEvent(BundleEvent.STARTED, b1);
            lds.bundleChanged(be2);
            assertEquals("There should not have been any extra notification", 
                    copiedNotifications, dst.notifications);
        } finally {
            LocalDiscoveryUtils.addEndpointID = true;
        }
    }

    public void testUpdateTracker() {
        LocalDiscoveryUtils.addEndpointID = false;
        
        try {
            URL rsA = getClass().getResource("/rs_a.xml");
            Enumeration<URL> rsAEnum = Collections.enumeration(Arrays.asList(rsA));
            
            // Set up some mock objects
            IMocksControl control = EasyMock.createNiceControl();
            Bundle b0 = control.createMock(Bundle.class);
            EasyMock.expect(b0.getState()).andReturn(Bundle.ACTIVE).anyTimes();
            EasyMock.expect(b0.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                    andReturn(rsAEnum).anyTimes();
            
            BundleContext bc = control.createMock(BundleContext.class);
            EasyMock.expect(bc.getBundles()).andReturn(new Bundle[] {b0}).anyTimes();
    
            TestDiscoveredServiceTracker dst = new TestDiscoveredServiceTracker();
    
            ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
            EasyMock.expect(sr.getProperty(DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA)).
                    andReturn(Collections.singleton("org.example.SomeService")).anyTimes();
            EasyMock.replay(sr);
            
            EasyMock.expect(bc.getService(sr)).andReturn(dst).anyTimes();
            control.replay();
    
            // create the local discovery service
            LocalDiscoveryService lds = new LocalDiscoveryService(bc);
            
            // it should be prepopulated with the info from bundle b0
            assertEquals(2, lds.servicesInfo.size());
            List<String> intf1 = Arrays.asList("org.example.SomeService");
            Map<String, Object> sed1Props = new HashMap<String, Object>();
            sed1Props.put(ServicePublication.SERVICE_INTERFACE_NAME, intf1);
            sed1Props.put("osgi.remote.requires.intents", "confidentiality");
            ServiceEndpointDescription sed1 = new ServiceEndpointDescriptionImpl(intf1, sed1Props);
            ServiceEndpointDescription sed2 = new ServiceEndpointDescriptionImpl(
                    Arrays.asList("SomeOtherService", "WithSomeSecondInterface"));
            assertTrue(lds.servicesInfo.containsKey(sed1));
            assertTrue(lds.servicesInfo.containsKey(sed2));
            
            // should be prepopulated by now...
            // register a tracker, it should get called back instantly with sed1
            assertEquals("Precondition failed", 0, dst.notifications.size());
            lds.trackerTracker.addingService(sr);
            assertEquals(1, dst.notifications.size());
            DiscoveredServiceNotification dsn = dst.notifications.iterator().next();
            assertEquals(DiscoveredServiceNotification.AVAILABLE, dsn.getType());
            assertEquals(sed1, dsn.getServiceEndpointDescription());
            verifyNotification(dsn, 0, 1, "org.example.SomeService");
            
            EasyMock.reset(sr);
            EasyMock.expect(sr.getProperty(DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA)).
                    andReturn(Arrays.asList("org.example.SomeService", "SomeOtherService")).anyTimes();
            EasyMock.replay(sr);
            
            lds.trackerTracker.modifiedService(sr, dst);
            assertEquals(2, dst.notifications.size());
            DiscoveredServiceNotification dsn0 = dst.notifications.get(0);
            assertEquals(DiscoveredServiceNotification.AVAILABLE, dsn0.getType());
            assertEquals(sed1, dsn0.getServiceEndpointDescription());
            DiscoveredServiceNotification dsn1 = dst.notifications.get(1);
            assertEquals(DiscoveredServiceNotification.AVAILABLE, dsn1.getType());
            assertEquals(sed2, dsn1.getServiceEndpointDescription());
            verifyNotification(dsn1, 0, 1, "SomeOtherService");
        } finally {
            LocalDiscoveryUtils.addEndpointID = true;
        }
    }
    
    public void testLocalDiscoveryServiceExistingBundles() {
        LocalDiscoveryUtils.addEndpointID = false;
        try {
            URL rsA = getClass().getResource("/rs_a.xml");
            Enumeration<URL> rsAEnum = Collections.enumeration(Arrays.asList(rsA));
            URL rsB = getClass().getResource("/rs_b.xml");
            Enumeration<URL> rsBEnum = Collections.enumeration(Arrays.asList(rsB));
            
            // Set up some mock objects
            IMocksControl control = EasyMock.createNiceControl();
            Bundle b0 = control.createMock(Bundle.class);
            EasyMock.expect(b0.getState()).andReturn(Bundle.INSTALLED).anyTimes();
            EasyMock.expect(b0.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                    andReturn(rsAEnum).anyTimes();
    
            Bundle b1 = control.createMock(Bundle.class);
            EasyMock.expect(b1.getState()).andReturn(Bundle.ACTIVE).anyTimes();
            EasyMock.expect(b1.findEntries("OSGI-INF/remote-service", "*.xml", false)).
                    andReturn(rsBEnum).anyTimes();
            
            BundleContext bc = control.createMock(BundleContext.class);
            ServiceReference sr = control.createMock(ServiceReference.class);
            TestDiscoveredServiceTracker dst = new TestDiscoveredServiceTracker();
    
            EasyMock.expect(bc.getBundles()).andReturn(new Bundle[] {b0, b1}).anyTimes();
            EasyMock.expect(sr.getProperty(DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA)).
                    andReturn(Collections.singleton("org.example.SomeService")).anyTimes();
            
            EasyMock.expect(bc.getService(sr)).andReturn(dst).anyTimes();
            control.replay();
    
            // create the local discovery service
            LocalDiscoveryService lds = new LocalDiscoveryService(bc);
            
            ServiceEndpointDescription sed3 = new ServiceEndpointDescriptionImpl(
                    Arrays.asList("org.example.SomeRelatedService", "org.example.SomeService"));
            assertEquals(1, lds.servicesInfo.size());
            assertEquals(sed3, lds.servicesInfo.keySet().iterator().next());
        } finally {
            LocalDiscoveryUtils.addEndpointID = true;
        }
    }
    
    public void testCombinationInterfaceAndFilter() {
        // TODO write this one
    }
    
    public void testCreatThenShutdown() {
        final List<String> bundleListenerRegs = new ArrayList<String>();
        
        IMocksControl control = EasyMock.createNiceControl();
        BundleContext bc = control.createMock(BundleContext.class);
        bc.addBundleListener((BundleListener) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                assertTrue("Should be a LocalDiscoveryService", 
                        EasyMock.getCurrentArguments()[0] instanceof LocalDiscoveryService);
                bundleListenerRegs.add("addBundleListener");
                return null;
            }            
        });
        
        bc.removeBundleListener((BundleListener) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                assertTrue("Should be a LocalDiscoveryService", 
                        EasyMock.getCurrentArguments()[0] instanceof LocalDiscoveryService);
                bundleListenerRegs.add("removeBundleListener");
                return null;
            }            
        });
        
        control.replay();
        
        assertEquals("Precondition failed", 0, bundleListenerRegs.size());
        LocalDiscoveryService lds = new LocalDiscoveryService(bc);
        assertEquals(1, bundleListenerRegs.size());
        assertEquals("addBundleListener", bundleListenerRegs.get(0));
        
        lds.shutdown();
        assertEquals(2, bundleListenerRegs.size());
        assertEquals("addBundleListener", bundleListenerRegs.get(0));
        assertEquals("removeBundleListener", bundleListenerRegs.get(1));
    }
    
    public void testAddTracker() {
        String prop = DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA;

        DiscoveredServiceTracker dst = EasyMock.createMock(DiscoveredServiceTracker.class);
        EasyMock.replay(dst);
        ServiceReference ref = EasyMock.createMock(ServiceReference.class);
        List<String> interfaces = new ArrayList<String>(Arrays.asList("A", "B"));
        EasyMock.expect(ref.getProperty(prop)).andReturn(interfaces);
        EasyMock.replay(ref);
        
        Map<String, List<DiscoveredServiceTracker>> forwardMap = 
            new HashMap<String, List<DiscoveredServiceTracker>>();
        Map<DiscoveredServiceTracker, Collection<String>> reverseMap = 
            new HashMap<DiscoveredServiceTracker, Collection<String>>();
        
        assertEquals("Precondition failed", 0, forwardMap.size());
        assertEquals("Precondition failed", 0, reverseMap.size());
        Collection<String> result = LocalDiscoveryService.addTracker(ref, dst, prop, 
            forwardMap, reverseMap);
        assertEquals(interfaces, result);
        
        assertEquals(interfaces, reverseMap.get(dst));
        // check that the data in the reversemap is backed by a different array
        reverseMap.get(dst).clear();
        assertEquals("The data in the reverseMap should be backed by a different object, not the DST property",
            Arrays.asList("A", "B"), interfaces);
        
        assertEquals(2, forwardMap.size());
        assertEquals(Collections.singletonList(dst), forwardMap.get("A"));
        assertEquals(Collections.singletonList(dst), forwardMap.get("B"));

        EasyMock.verify(ref);
    }
    
    public void testRemoveTracker() {
        DiscoveredServiceTracker dst = new DiscoveredServiceTracker(){
            public void serviceChanged(DiscoveredServiceNotification notification) {
            }
        };
        
        Map<String, List<DiscoveredServiceTracker>> forwardMap = 
            new HashMap<String, List<DiscoveredServiceTracker>>();
        forwardMap.put("A", new ArrayList<DiscoveredServiceTracker>(Arrays.asList(dst)));

        Map<DiscoveredServiceTracker, Collection<String>> reverseMap = 
            new HashMap<DiscoveredServiceTracker, Collection<String>>();
        reverseMap.put(dst, new ArrayList<String>(Arrays.asList("A", "B")));
        
        assertEquals("Precondition failed", 1, reverseMap.size());
        assertEquals("Precondition failed", 1, forwardMap.get("A").size());
        Collection<String> old = LocalDiscoveryService.removeTracker(dst, forwardMap, reverseMap);
        assertEquals(1, old.size());
        assertEquals("A", old.iterator().next());
        
        assertEquals(0, forwardMap.get("A").size());
        assertEquals(0, reverseMap.size());
    }

    public void testRemoveTrackerNull() {
        DiscoveredServiceTracker dst = new DiscoveredServiceTracker(){
            public void serviceChanged(DiscoveredServiceNotification notification) {
            }
        };
        
        Map<String, List<DiscoveredServiceTracker>> forwardMap = 
            new HashMap<String, List<DiscoveredServiceTracker>>();
        Map<DiscoveredServiceTracker, Collection<String>> reverseMap = 
            new HashMap<DiscoveredServiceTracker, Collection<String>>();
        
        assertNull(LocalDiscoveryService.removeTracker(dst, forwardMap, reverseMap));
    }
    
    public void testTriggerCallbacksWithFilter() throws Exception {
        String filter = "(|(osgi.remote.service.interfaces=org.acme.A)(osgi.remote.service.interfaces=org.acme.B))";

        Filter mockFilter = EasyMock.createMock(Filter.class);
        Dictionary<String, Object> map = new Hashtable<String, Object>();
        map.put("osgi.remote.service.interfaces", Arrays.asList("org.acme.B"));
        EasyMock.expect(mockFilter.match(map)).andReturn(true).anyTimes();
        EasyMock.replay(mockFilter);

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.createFilter(filter)).andReturn(mockFilter).anyTimes();
        EasyMock.replay(bc);
        
        final List<DiscoveredServiceNotification> notifications = 
            new ArrayList<DiscoveredServiceNotification>();
        DiscoveredServiceTracker dst = new DiscoveredServiceTracker(){        
            public void serviceChanged(DiscoveredServiceNotification notification) {
                notifications.add(notification);
            }
        };
        
        LocalDiscoveryService lds = new LocalDiscoveryService(bc);
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl("org.acme.B");
        
        assertEquals("Precondition failed", 0, notifications.size());
        lds.triggerCallbacks(dst, filter, true, sd, DiscoveredServiceNotification.AVAILABLE);
        assertEquals(1, notifications.size());
    }
 
    private void verifyNotification(DiscoveredServiceNotification dsn,
                               int filterCount,
                               int interfaceCount,
                               String expected) {
        assertNotNull(dsn.getFilters());
        assertNotNull(dsn.getInterfaces());
        assertEquals(filterCount, dsn.getFilters().size());
        assertEquals(interfaceCount, dsn.getInterfaces().size());
        Iterator<?> i = filterCount > 0 
                     ? dsn.getFilters().iterator()
                     : dsn.getInterfaces().iterator();
        assertEquals(expected, i.next()); 
    }

    @SuppressWarnings("unchecked")
    private Map<Collection<String>, String> getEndpointIDs(
            Collection<ServiceEndpointDescription> seds) {
        Map<Collection<String>, String> map = new HashMap<Collection<String>, String>();
        
        for (ServiceEndpointDescription sed : seds) {
            map.put((Collection<String>) sed.getProvidedInterfaces(), sed.getEndpointID());
        }
        
        return map;
    }

    private static class TestDiscoveredServiceTracker implements DiscoveredServiceTracker {
        private List<DiscoveredServiceNotification> notifications = new ArrayList<DiscoveredServiceNotification>();

        public void serviceChanged(DiscoveredServiceNotification notification) {
            notifications.add(notification);
        }        
    }    
}
