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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;

public class LocalDiscoveryTest extends TestCase {
    public void testLocalDiscovery() throws Exception {
        Filter filter = EasyMock.createMock(Filter.class);
        EasyMock.replay(filter);
        
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.createFilter("(objectClass=org.osgi.service.remoteserviceadmin.EndpointListener)")).andReturn(filter);
        bc.addServiceListener((ServiceListener) EasyMock.anyObject(), 
            EasyMock.eq("(objectClass=org.osgi.service.remoteserviceadmin.EndpointListener)"));
        EasyMock.expectLastCall();
        EasyMock.expect(bc.getServiceReferences("org.osgi.service.remoteserviceadmin.EndpointListener", null)).andReturn(null);
        bc.addBundleListener((BundleListener) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {            
            public Object answer() throws Throwable {
                assertEquals(LocalDiscovery.class, EasyMock.getCurrentArguments()[0].getClass());
                return null;
            }
        });
        EasyMock.expect(bc.getBundles()).andReturn(null);
        EasyMock.replay(bc);
                
        LocalDiscovery ld = new LocalDiscovery(bc);
        assertSame(bc, ld.bundleContext);
        assertNotNull(ld.listenerTracker);        
        
        EasyMock.verify(bc);
        
        EasyMock.reset(bc);
        bc.removeBundleListener(ld);        
        EasyMock.expectLastCall();
        bc.removeServiceListener((ServiceListener) EasyMock.anyObject());
        EasyMock.expectLastCall();
        EasyMock.replay(bc);

        ld.shutDown();
        EasyMock.verify(bc);
    }
    
    public void testPreExistingBundles() throws Exception {
        Filter filter = EasyMock.createMock(Filter.class);
        EasyMock.replay(filter);
        
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.createFilter("(objectClass=org.osgi.service.remoteserviceadmin.EndpointListener)")).andReturn(filter);
        bc.addServiceListener((ServiceListener) EasyMock.anyObject(), 
            EasyMock.eq("(objectClass=org.osgi.service.remoteserviceadmin.EndpointListener)"));
        EasyMock.expectLastCall();
        EasyMock.expect(bc.getServiceReferences("org.osgi.service.remoteserviceadmin.EndpointListener", null)).andReturn(null);
        bc.addBundleListener((BundleListener) EasyMock.anyObject());
        EasyMock.expectLastCall();
        
        Bundle b1 = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b1.getState()).andReturn(Bundle.RESOLVED);
        EasyMock.replay(b1);
        Bundle b2 = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b2.getState()).andReturn(Bundle.ACTIVE);
        Dictionary<String, Object> headers = new Hashtable<String, Object>();
        headers.put("Remote-Service", "OSGI-INF/remote-service/");
        EasyMock.expect(b2.getHeaders()).andReturn(headers);
        
        URL rs3URL = getClass().getResource("/ed3.xml");
        URL rs4URL = getClass().getResource("/ed4.xml");        
        List<URL> urls = Arrays.asList(rs3URL, rs4URL);
        EasyMock.expect(b2.findEntries("OSGI-INF/remote-service", "*.xml", false))
            .andReturn(Collections.enumeration(urls));
        EasyMock.replay(b2);
        
        EasyMock.expect(bc.getBundles()).andReturn(new Bundle [] {b1, b2});
        EasyMock.replay(bc);
                
        LocalDiscovery ld = new LocalDiscovery(bc);
                
        assertEquals(3, ld.endpointDescriptions.size());
        Set<String> expected = new HashSet<String>(
                Arrays.asList("http://somewhere:12345", "http://somewhere:1", "http://somewhere"));
        Set<String> actual = new HashSet<String>();
        for (Map.Entry<EndpointDescription, Bundle> entry : ld.endpointDescriptions.entrySet()) {
            assertSame(b2, entry.getValue());
            actual.add(entry.getKey().getRemoteURI());
        }
        assertEquals(expected, actual);
    }
    
    public void testBundleChanged() throws Exception {
        LocalDiscovery ld = getLocalDiscovery();

        Bundle bundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(bundle.getState()).andReturn(Bundle.ACTIVE);
        Dictionary<String, Object> headers = new Hashtable<String, Object>();
        headers.put("Remote-Service", "OSGI-INF/rsa/");
        EasyMock.expect(bundle.getHeaders()).andReturn(headers);
        EasyMock.expect(bundle.findEntries("OSGI-INF/rsa", "*.xml", false))
            .andReturn(Collections.enumeration(
                Collections.singleton(getClass().getResource("/ed3.xml"))));
        EasyMock.replay(bundle);
                
        BundleEvent be0 = new BundleEvent(BundleEvent.INSTALLED, bundle);
        ld.bundleChanged(be0);
        assertEquals(0, ld.endpointDescriptions.size());
        
        BundleEvent be = new BundleEvent(BundleEvent.STARTED, bundle);
        ld.bundleChanged(be);
        assertEquals(1, ld.endpointDescriptions.size());
        EndpointDescription ed = ld.endpointDescriptions.keySet().iterator().next();
        assertEquals("http://somewhere:12345", ed.getRemoteURI());
        assertSame(bundle, ld.endpointDescriptions.get(ed));
    }
    
    public void testAddingService() throws Exception {
        LocalDiscovery ld = getLocalDiscovery();
        ///
        Bundle bundle = EasyMock.createMock(Bundle.class);
        EasyMock.expect(bundle.getState()).andReturn(Bundle.ACTIVE);
        Dictionary<String, Object> headers = new Hashtable<String, Object>();
        headers.put("Remote-Service", "OSGI-INF/rsa/ed4.xml");
        EasyMock.expect(bundle.getHeaders()).andReturn(headers);
        EasyMock.expect(bundle.findEntries("OSGI-INF/rsa", "ed4.xml", false))
            .andReturn(Collections.enumeration(
                Collections.singleton(getClass().getResource("/ed4.xml"))));
        EasyMock.replay(bundle);
                        
        BundleEvent be = new BundleEvent(BundleEvent.STARTED, bundle);
        ld.bundleChanged(be);
        assertEquals(2, ld.endpointDescriptions.size());        
        ///
        final Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(EndpointListener.ENDPOINT_LISTENER_SCOPE, "(objectClass=org.example.ClassA)");
        ServiceReference sr = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sr.getPropertyKeys()).andReturn(props.keySet().toArray(new String [] {})).anyTimes();
        EasyMock.expect(sr.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                return props.get(EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
                
        EasyMock.replay(sr);
        
        EasyMock.reset(ld.bundleContext);
        EndpointListener el = EasyMock.createMock(EndpointListener.class);
        EasyMock.expect(ld.bundleContext.getService(sr)).andReturn(el);
        EasyMock.expect(ld.bundleContext.createFilter((String) EasyMock.anyObject())).andAnswer(new IAnswer<Filter>() {
            public Filter answer() throws Throwable {
                return FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(ld.bundleContext);
        
        el.endpointAdded((EndpointDescription) EasyMock.anyObject(), 
                EasyMock.eq("(objectClass=org.example.ClassA)"));
        EasyMock.expectLastCall();
        EasyMock.replay(el);
        
        assertEquals("Precondition failed", 0, ld.listenerToFilters.size());
        assertEquals("Precondition failed", 0, ld.filterToListeners.size());        
        assertSame(el, ld.listenerTracker.addingService(sr));
        
        assertEquals(1, ld.listenerToFilters.size());
        assertEquals(Collections.singletonList("(objectClass=org.example.ClassA)"), ld.listenerToFilters.get(el));
        assertEquals(1, ld.filterToListeners.size()); 
        assertEquals(Collections.singletonList(el), ld.filterToListeners.get("(objectClass=org.example.ClassA)"));

        EasyMock.verify(el);
    }

    private LocalDiscovery getLocalDiscovery() throws InvalidSyntaxException {
        Filter filter = EasyMock.createMock(Filter.class);
        EasyMock.replay(filter);
        
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.createFilter("(objectClass=org.osgi.service.remoteserviceadmin.EndpointListener)")).andReturn(filter);
        bc.addServiceListener((ServiceListener) EasyMock.anyObject(), 
            EasyMock.eq("(objectClass=org.osgi.service.remoteserviceadmin.EndpointListener)"));
        EasyMock.expectLastCall();
        EasyMock.expect(bc.getServiceReferences("org.osgi.service.remoteserviceadmin.EndpointListener", null)).andReturn(null);
        bc.addBundleListener((BundleListener) EasyMock.anyObject());
        EasyMock.expectLastCall();
        EasyMock.expect(bc.getBundles()).andReturn(null);
        EasyMock.replay(bc);
                
        LocalDiscovery ld = new LocalDiscovery(bc);
        return ld;
    }
}
