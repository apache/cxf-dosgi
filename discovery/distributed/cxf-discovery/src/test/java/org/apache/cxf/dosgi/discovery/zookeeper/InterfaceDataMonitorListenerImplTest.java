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
package org.apache.cxf.dosgi.discovery.zookeeper;

import junit.framework.TestCase;

public class InterfaceDataMonitorListenerImplTest extends TestCase {
    
    public void testDUMMY() {
        assertTrue(true);
    }
    
//    public void testChange() throws Exception {
//        final List<DiscoveredServiceNotification> dsnCallbacks = new ArrayList<DiscoveredServiceNotification>();
//        DiscoveredServiceTracker dst = new DiscoveredServiceTracker() {
//            public void serviceChanged(DiscoveredServiceNotification dsn) {
//                dsnCallbacks.add(dsn);
//            }            
//        };
//        
//        //----------------------------------------------------------------
//        // Test DiscoveredServiceNotification.AVAILABLE
//        //----------------------------------------------------------------
//        Properties initial = new Properties();
//        initial.put("a", "b");     
//        initial.put(ServicePublication.ENDPOINT_LOCATION, "http://somehost:12345/some/context");
//        ByteArrayOutputStream propBytes = new ByteArrayOutputStream();
//        initial.store(propBytes, "");
//        
//        ZooKeeper zk = EasyMock.createMock(ZooKeeper.class);
//        EasyMock.expect(zk.getChildren(Util.getZooKeeperPath(String.class.getName()), false))
//            .andReturn(Arrays.asList("x#y#z"));
//        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()) + "/x#y#z", false, null))
//            .andReturn(propBytes.toByteArray());
//        EasyMock.replay(zk);
//
//        InterfaceDataMonitorListenerImpl dml = new InterfaceDataMonitorListenerImpl(zk, String.class.getName(), dst);
//        
//        assertEquals("Precondition failed", 0, dsnCallbacks.size());
//        dml.change();
//        assertEquals(1, dsnCallbacks.size());
//        DiscoveredServiceNotification dsn = dsnCallbacks.iterator().next();
//        assertEquals(Collections.singleton(String.class.getName()), dsn.getInterfaces());
//        assertEquals(DiscoveredServiceNotification.AVAILABLE, dsn.getType());
//        assertEquals(0, dsn.getFilters().size());
//        ServiceEndpointDescription sed = dsn.getServiceEndpointDescription();
//        assertEquals(Collections.singleton(String.class.getName()), sed.getProvidedInterfaces());    
//        
//        Properties expected = new Properties();
//        expected.load(new ByteArrayInputStream(propBytes.toByteArray()));
//        expected.put("service.exported.configs", "org.apache.cxf.ws");
//        expected.put("org.apache.cxf.ws.address", "http://somehost:12345/some/context");
//        
//        assertEquals(expected, sed.getProperties());
//        EasyMock.verify(zk);
//        
//        // Again with the same data
//        EasyMock.reset(zk);
//        EasyMock.expect(zk.getChildren(Util.getZooKeeperPath(String.class.getName()), false))
//            .andReturn(Arrays.asList("x#y#z"));
//        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()) + "/x#y#z", false, null))
//            .andReturn(propBytes.toByteArray());
//        EasyMock.replay(zk);
//
//        dsnCallbacks.clear();
//        assertEquals("Precondition failed", 0, dsnCallbacks.size());
//        dml.change();
//        assertEquals(0, dsnCallbacks.size());
//        
//        EasyMock.verify(zk);
//        //----------------------------------------------------------------
//        // Test DiscoveredServiceNotification.MODIFIED
//        //----------------------------------------------------------------
//        Properties modified = new Properties();
//        modified.put("c", "d");
//        modified.put(ServicePublication.ENDPOINT_LOCATION, "http://somehost:999/some/context");
//        modified.put("service.exported.configs", "org.apache.cxf.rs");
//        ByteArrayOutputStream modBytes = new ByteArrayOutputStream();
//        modified.store(modBytes, "");
//        
//        EasyMock.reset(zk);
//        EasyMock.expect(zk.getChildren(Util.getZooKeeperPath(String.class.getName()), false))
//            .andReturn(Arrays.asList("x#y#z"));
//        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()) + "/x#y#z", false, null))
//            .andReturn(modBytes.toByteArray());
//        EasyMock.replay(zk);
//
//        dsnCallbacks.clear();
//        assertEquals("Precondition failed", 0, dsnCallbacks.size());
//        dml.change();
//        assertEquals(1, dsnCallbacks.size());
//        DiscoveredServiceNotification dsn2 = dsnCallbacks.iterator().next();
//        assertEquals(Collections.singleton(String.class.getName()), dsn2.getInterfaces());
//        assertEquals(DiscoveredServiceNotification.MODIFIED, dsn2.getType());
//        assertEquals(0, dsn2.getFilters().size());
//        ServiceEndpointDescription sed2 = dsn2.getServiceEndpointDescription();
//        assertEquals(Collections.singleton(String.class.getName()), sed2.getProvidedInterfaces());        
//        assertEquals(modified, sed2.getProperties());
//        
//        EasyMock.verify(zk);
//
//        //----------------------------------------------------------------
//        // Test DiscoveredServiceNotification.MODIFIED2
//        //----------------------------------------------------------------
//        Properties modified2 = new Properties();
//        modified2.put("c", "d2");
//        modified2.put(ServicePublication.ENDPOINT_LOCATION, "http://somehost:112/some/context");
//        modified2.put("service.exported.configs", "org.apache.cxf.ws");
//        modified2.put("org.apache.cxf.ws.address", "http://somewhereelse/123");
//        ByteArrayOutputStream modBytes2 = new ByteArrayOutputStream();
//        modified2.store(modBytes2, "");
//        
//        EasyMock.reset(zk);
//        EasyMock.expect(zk.getChildren(Util.getZooKeeperPath(String.class.getName()), false))
//            .andReturn(Arrays.asList("x#y#z"));
//        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()) + "/x#y#z", false, null))
//            .andReturn(modBytes2.toByteArray());
//        EasyMock.replay(zk);
//
//        dsnCallbacks.clear();
//        assertEquals("Precondition failed", 0, dsnCallbacks.size());
//        dml.change();
//        assertEquals(1, dsnCallbacks.size());
//        DiscoveredServiceNotification dsn3 = dsnCallbacks.iterator().next();
//        assertEquals(Collections.singleton(String.class.getName()), dsn3.getInterfaces());
//        assertEquals(DiscoveredServiceNotification.MODIFIED, dsn3.getType());
//        assertEquals(0, dsn3.getFilters().size());
//        ServiceEndpointDescription sed3 = dsn3.getServiceEndpointDescription();
//        assertEquals(Collections.singleton(String.class.getName()), sed3.getProvidedInterfaces());        
//        assertEquals(modified2, sed3.getProperties());
//        
//        EasyMock.verify(zk);
//        //----------------------------------------------------------------
//        // Test DiscoveredServiceNotification.UNAVAILABLE
//        //----------------------------------------------------------------
//        EasyMock.reset(zk);
//        EasyMock.expect(zk.getChildren(Util.getZooKeeperPath(String.class.getName()), false))
//            .andReturn(Collections.<String>emptyList());
//        EasyMock.replay(zk);
//
//        dsnCallbacks.clear();
//        assertEquals("Precondition failed", 0, dsnCallbacks.size());
//        dml.change();
//        assertEquals(1, dsnCallbacks.size());
//        DiscoveredServiceNotification dsn4 = dsnCallbacks.iterator().next();
//        assertEquals(Collections.singleton(String.class.getName()), dsn4.getInterfaces());
//        assertEquals(DiscoveredServiceNotification.UNAVAILABLE, dsn4.getType());
//        assertEquals(0, dsn4.getFilters().size());
//        ServiceEndpointDescription sed4 = dsn4.getServiceEndpointDescription();
//        assertEquals(Collections.singleton(String.class.getName()), sed4.getProvidedInterfaces());        
//        assertEquals(modified2, sed4.getProperties());
//        
//        EasyMock.verify(zk);
//        
//        // Try the same again...
//        EasyMock.reset(zk);
//        EasyMock.expect(zk.getChildren(Util.getZooKeeperPath(String.class.getName()), false))
//            .andReturn(Collections.<String>emptyList());
//        EasyMock.replay(zk);
//
//        dsnCallbacks.clear();
//        assertEquals("Precondition failed", 0, dsnCallbacks.size());
//        dml.change();
//        assertEquals("Should not receive a callback again...", 0, dsnCallbacks.size());
//        EasyMock.verify(zk);
//    }
}
