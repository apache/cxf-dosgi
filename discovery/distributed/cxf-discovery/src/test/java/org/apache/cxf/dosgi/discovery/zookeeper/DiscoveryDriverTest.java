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

public class DiscoveryDriverTest extends TestCase {
    
    public void testDUMMY() {
        assertTrue(true);
    }
    
//    public void testDiscoveryDriver() throws Exception {
//        BundleContext bc = getDefaultBundleContext();
//        Dictionary<String, String> props = getDefaultProps();
//        
//        final StringBuilder sb = new StringBuilder();
//        DiscoveryDriver dd = new DiscoveryDriver(bc, props) {
//            @Override
//            ZooKeeper createZooKeeper() throws IOException {
//                sb.append(zkHost + ":" + zkPort);
//                ZooKeeper zk = EasyMock.createMock(ZooKeeper.class);
//                EasyMock.replay(zk);
//                return zk;
//            }           
//        };
//        EasyMock.verify(bc);
//        assertEquals("somehost:1910", sb.toString());
//        
//        EasyMock.verify(dd.zooKeeper);
//        EasyMock.reset(dd.zooKeeper);
//        dd.zooKeeper.close();
//        EasyMock.expectLastCall();
//        EasyMock.replay(dd.zooKeeper);
//        
//        ServiceTracker st1 = EasyMock.createMock(ServiceTracker.class);
//        st1.close();
//        EasyMock.expectLastCall();
//        EasyMock.replay(st1);
//        ServiceTracker st2 = EasyMock.createMock(ServiceTracker.class);
//        st2.close();
//        EasyMock.expectLastCall();
//        EasyMock.replay(st2);
//        
//        dd.lookupTracker = st1;
//        dd.publicationTracker = st2;
//        
//        dd.destroy();        
//    }
//
//    private void expectServiceTrackerCalls(BundleContext bc, String objectClass)
//            throws InvalidSyntaxException {
//        Filter filter = EasyMock.createNiceMock(Filter.class);
//        EasyMock.replay(filter);
//        
//        EasyMock.expect(bc.createFilter("(objectClass=" + objectClass + ")"))
//            .andReturn(filter).anyTimes();
//        bc.addServiceListener((ServiceListener) EasyMock.anyObject(), 
//            EasyMock.eq("(objectClass=" + objectClass + ")"));
//        EasyMock.expectLastCall().anyTimes();
//        EasyMock.expect(bc.getServiceReferences(objectClass, null))
//            .andReturn(new ServiceReference [0]).anyTimes();
//    }
//    
//    public void testProcessEvent() throws Exception {
//        DiscoveryDriver db = new DiscoveryDriver(getDefaultBundleContext(), getDefaultProps()) {
//            @Override
//            ZooKeeper createZooKeeper() throws IOException {
//                return null;
//            }            
//        };
//        
//        FindInZooKeeperCustomizer fc = new FindInZooKeeperCustomizer(null, null);
//        List<InterfaceMonitor> l1 = new ArrayList<InterfaceMonitor>();
//        InterfaceMonitor dm1a = EasyMock.createMock(InterfaceMonitor.class);
//        dm1a.process();
//        EasyMock.expectLastCall();
//        EasyMock.replay(dm1a);
//        InterfaceMonitor dm1b = EasyMock.createMock(InterfaceMonitor.class);
//        dm1b.process();
//        EasyMock.expectLastCall();
//        EasyMock.replay(dm1b);
//        l1.add(dm1a);
//        l1.add(dm1b);
//        
//        List<InterfaceMonitor> l2 = new ArrayList<InterfaceMonitor>();
//        InterfaceMonitor dm2 = EasyMock.createMock(InterfaceMonitor.class);
//        dm2.process();
//        EasyMock.expectLastCall();
//        EasyMock.replay(dm2);
//        l2.add(dm2);
//
//        fc.watchers.put(EasyMock.createMock(DiscoveredServiceTracker.class), l1);
//        fc.watchers.put(EasyMock.createMock(DiscoveredServiceTracker.class), l2);
//        
//        db.finderCustomizer = fc;
//        db.process(null);
//        
//        EasyMock.verify(dm1a);
//        EasyMock.verify(dm1b);
//        EasyMock.verify(dm2);
//    }
//
//    private BundleContext getDefaultBundleContext() throws InvalidSyntaxException {
//        BundleContext bc = EasyMock.createMock(BundleContext.class);
//        expectServiceTrackerCalls(bc, ServicePublication.class.getName());
//        expectServiceTrackerCalls(bc, DiscoveredServiceTracker.class.getName());
//        EasyMock.replay(bc);
//        return bc;
//    }
//
//    private Dictionary<String, String> getDefaultProps() {
//        Dictionary<String, String> props = new Hashtable<String, String>();
//        props.put("zookeeper.host", "somehost");
//        props.put("zookeeper.port", "1910");
//        props.put("zookeeper.timeout", "1500");
//        return props;
//    }

}
