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
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

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
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {            
            public Object answer() throws Throwable {
                assertEquals(LocalDiscovery.class, EasyMock.getCurrentArguments()[0].getClass());
                return null;
            }
        });
        
        Bundle b1 = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b1.getState()).andReturn(Bundle.RESOLVED);
        EasyMock.replay(b1);
        Bundle b2 = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b2.getState()).andReturn(Bundle.ACTIVE);
        Dictionary<String, Object> headers = new Hashtable<String, Object>();
        headers.put("Remote-Service", "OSGI-INF/remote-service");
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
}
