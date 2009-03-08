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
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
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
        EasyMock.expect(sr.getProperty("osgi.discovery.interest.interfaces")).
                andReturn(Collections.singleton("org.example.SomeService")).anyTimes();
        
        EasyMock.expect(bc.getService(sr)).andReturn(dst).anyTimes();
        control.replay();

        // create the local discovery service
        LocalDiscoveryService lds = new LocalDiscoveryService(bc);
        
        // it should be prepopulated with the info from bundle b0
        assertEquals(2, lds.servicesInfo.size());
        Map<String, Object> sed1Props = new HashMap<String, Object>();
        sed1Props.put("osgi.remote.requires.intents", "confidentiality");
        ServiceEndpointDescription sed1 = new ServiceEndpointDescriptionImpl(
                Arrays.asList("org.example.SomeService"), sed1Props);
        ServiceEndpointDescription sed2 = new ServiceEndpointDescriptionImpl(
                Arrays.asList("SomeOtherService", "WithSomeSecondInterface"));
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
        
        ServiceEndpointDescription sed3 = new ServiceEndpointDescriptionImpl(
                Arrays.asList("org.example.SomeRelatedService", "org.example.SomeService"));
        assertEquals(3, lds.servicesInfo.size());
        assertTrue(lds.servicesInfo.containsKey(sed1));
        assertTrue(lds.servicesInfo.containsKey(sed2));
        assertTrue(lds.servicesInfo.containsKey(sed3));
        
        assertEquals("We should have been notified of the new bundle", 
                2, dst.notifications.size());
        assertEquals(DiscoveredServiceNotification.AVAILABLE, 
                dst.notifications.get(1).getType());
        assertEquals(sed3, dst.notifications.get(1).getServiceEndpointDescription());
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

        // now remove the tracker itself...
        assertEquals("Precondition failed", 1, lds.interfacesToTrackers.size());
        assertEquals("Precondition failed", 1, lds.trackersToInterfaces.size());
        assertEquals("Precondition failed", 1, lds.interfacesToTrackers.values().iterator().next().size());
        assertEquals("Precondition failed", 1, lds.trackersToInterfaces.values().iterator().next().size());
        lds.trackerTracker.removedService(sr, dst);
        assertEquals("Tracker should have been removed", 0, lds.interfacesToTrackers.values().iterator().next().size());
        assertEquals("Tracker should have been removed", 0, lds.trackersToInterfaces.size());
    }
    
    public void testLocalDiscoveryServiceDSTFilter() throws Exception {
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
        EasyMock.expect(sr.getProperty("osgi.discovery.interest.filters")).
                andReturn(Collections.singleton("(blah <= 5)")).anyTimes();
        
        EasyMock.expect(bc.getService(sr)).andReturn(dst).anyTimes();
        
        // set up the mock filter behaviour
        Dictionary<String, Object> d1 = new Hashtable<String, Object>();
        d1.put("blah", "5");
        EasyMock.expect(mockFilter.match(d1)).andReturn(true).anyTimes();
        Dictionary<String, Object> d2 = new Hashtable<String, Object>();
        d2.put("blah", "3");
        d2.put("boo", "hello");
        EasyMock.expect(mockFilter.match(d2)).andReturn(true).anyTimes();
        
        control.replay();

        // create the local discovery service
        LocalDiscoveryService lds = new LocalDiscoveryService(bc);
        
        // it should be prepopulated with the info from bundle b0
        assertEquals(2, lds.servicesInfo.size());
        Map<String, Object> sed1Props = new HashMap<String, Object>();
        sed1Props.put("osgi.remote.requires.intents", "confidentiality");
        ServiceEndpointDescription sed1 = new ServiceEndpointDescriptionImpl(
                Arrays.asList("org.example.SomeService"), sed1Props);
        Map<String, Object> sed2Props = new HashMap<String, Object>();
        sed2Props.put("blah", "5");
        ServiceEndpointDescription sed2 = new ServiceEndpointDescriptionImpl(
                Arrays.asList("SomeOtherService", "WithSomeSecondInterface"), sed2Props);
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
        
        Map<String, Object> sed3Props = new HashMap<String, Object>();
        sed3Props.put("blah", "3");
        sed3Props.put("boo", "hello");
        ServiceEndpointDescription sed3 = new ServiceEndpointDescriptionImpl(
                Arrays.asList("org.example.SomeRelatedService", "org.example.SomeService"), sed3Props);
        assertEquals(3, lds.servicesInfo.size());
        assertTrue(lds.servicesInfo.containsKey(sed1));
        assertTrue(lds.servicesInfo.containsKey(sed2));
        assertTrue(lds.servicesInfo.containsKey(sed3));
        
        assertEquals("We should have been notified of the new bundle", 
                2, dst.notifications.size());
        assertEquals(DiscoveredServiceNotification.AVAILABLE, 
                dst.notifications.get(1).getType());
        assertEquals(sed3, dst.notifications.get(1).getServiceEndpointDescription());
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
        
        // assert that we get notified about the removal
    }

    public void testUpdateTracker() {
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
        EasyMock.expect(sr.getProperty("osgi.discovery.interest.interfaces")).
                andReturn(Collections.singleton("org.example.SomeService")).anyTimes();
        EasyMock.replay(sr);
        
        EasyMock.expect(bc.getService(sr)).andReturn(dst).anyTimes();
        control.replay();

        // create the local discovery service
        LocalDiscoveryService lds = new LocalDiscoveryService(bc);
        
        // it should be prepopulated with the info from bundle b0
        assertEquals(2, lds.servicesInfo.size());
        Map<String, Object> sed1Props = new HashMap<String, Object>();
        sed1Props.put("osgi.remote.requires.intents", "confidentiality");
        ServiceEndpointDescription sed1 = new ServiceEndpointDescriptionImpl(
                Arrays.asList("org.example.SomeService"), sed1Props);
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
        
        EasyMock.reset(sr);
        EasyMock.expect(sr.getProperty("osgi.discovery.interest.interfaces")).
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
    }
    
    public void testLocalDiscoveryServiceExistingBundles() {
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
        EasyMock.expect(sr.getProperty("osgi.discovery.interest.interfaces")).
                andReturn(Collections.singleton("org.example.SomeService")).anyTimes();
        
        EasyMock.expect(bc.getService(sr)).andReturn(dst).anyTimes();
        control.replay();

        // create the local discovery service
        LocalDiscoveryService lds = new LocalDiscoveryService(bc);
        
        ServiceEndpointDescription sed3 = new ServiceEndpointDescriptionImpl(
                Arrays.asList("org.example.SomeRelatedService", "org.example.SomeService"));
        assertEquals(1, lds.servicesInfo.size());
        assertEquals(sed3, lds.servicesInfo.keySet().iterator().next());
    
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

    private static class TestDiscoveredServiceTracker implements DiscoveredServiceTracker {
        private List<DiscoveredServiceNotification> notifications = new ArrayList<DiscoveredServiceNotification>();

        public void serviceChanged(DiscoveredServiceNotification notification) {
            notifications.add(notification);
        }        
    }
}
