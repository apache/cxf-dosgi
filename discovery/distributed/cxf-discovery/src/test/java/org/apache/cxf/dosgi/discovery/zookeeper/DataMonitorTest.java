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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.easymock.classextension.EasyMock;
import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServiceEndpointDescription;

public class DataMonitorTest extends TestCase {
    public void testCreateListener() {
        ZooKeeper zk = EasyMock.createMock(ZooKeeper.class);
        DiscoveredServiceTracker dst = EasyMock.createMock(DiscoveredServiceTracker.class);        
        
        DataMonitor dm = new DataMonitor(zk, String.class.getName(), dst);
        DataMonitorListenerImpl listener = (DataMonitorListenerImpl) dm.listener;
        assertSame(zk, listener.zookeeper);
        assertEquals(Util.getZooKeeperPath(String.class.getName()), listener.znode);
        assertEquals(String.class.getName(), listener.interFace);
        assertSame(dst, listener.discoveredServiceTracker);
    }
    
    public void testDataMonitor() throws Exception {
        Properties s1Props = new Properties();
        s1Props.put("a", "b");
        ByteArrayOutputStream s1Bytes = new ByteArrayOutputStream();
        s1Props.store(s1Bytes, "");
        
        Properties s2Props = new Properties();
        s2Props.put("d", "e");
        ByteArrayOutputStream s2Bytes = new ByteArrayOutputStream();
        s2Props.store(s2Bytes, "");
        
        ZooKeeper zk = EasyMock.createNiceMock(ZooKeeper.class);
        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()), false, null))
            .andReturn(new byte[0]).anyTimes();
        EasyMock.expect(zk.getChildren(Util.getZooKeeperPath(String.class.getName()), false))
            .andReturn(Arrays.asList("a#90#r", "b#90#r")).anyTimes();
        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()) + "/a#90#r", false, null))
            .andReturn(s1Bytes.toByteArray()).anyTimes();
        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()) + "/b#90#r", false, null))
            .andReturn(s2Bytes.toByteArray()).anyTimes();
        EasyMock.replay(zk);
        
        final List<DiscoveredServiceNotification> notifications = new ArrayList<DiscoveredServiceNotification>();
        DiscoveredServiceTracker dst = new DiscoveredServiceTracker() {
            public void serviceChanged(DiscoveredServiceNotification dsn) {
                notifications.add(dsn);
            }            
        };
        
        DataMonitor dm = new DataMonitor(zk, String.class.getName(), dst);
        assertEquals("Precondition failed", 0, notifications.size());
        dm.processResult(Code.Ok, null, null, null);
        assertEquals(2, notifications.size());
        
        boolean s1Found = false, s2Found = false;
        for (DiscoveredServiceNotification dsn : notifications) {
            assertEquals(DiscoveredServiceNotification.AVAILABLE, dsn.getType());
            assertEquals(Collections.emptyList(), dsn.getFilters());
            assertEquals(Collections.singleton(String.class.getName()), dsn.getInterfaces());
            ServiceEndpointDescription sed = dsn.getServiceEndpointDescription();
            assertEquals(Collections.singleton(String.class.getName()), sed.getProvidedInterfaces());
            Map<?, ?> m = sed.getProperties();
            if (s1Props.equals(m)) {
                s1Found = true;
            }
            if (s2Props.equals(m)) {
                s2Found = true;
            }            
        }
        assertTrue(s1Found);
        assertTrue(s2Found);
        
        // Second time around, with same data
        notifications.clear();
        dm.processResult(Code.Ok, null, null, null);
        assertEquals("No changes, so should not get any new notifications", 0, notifications.size());
        
        // Third time around, with different data
        EasyMock.reset(zk);
        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()), false, null))
            .andReturn(new byte[] {123}).anyTimes();
        EasyMock.expect(zk.getChildren(Util.getZooKeeperPath(String.class.getName()), false))
            .andReturn(Arrays.asList("a#90#r")).anyTimes();
        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()) + "/a#90#r", false, null))
            .andReturn(s1Bytes.toByteArray()).anyTimes();
        EasyMock.replay(zk);
        notifications.clear();
        dm.processResult(Code.Ok, null, null, null);
        DiscoveredServiceNotification dsn = notifications.iterator().next();
        assertEquals(1, notifications.size());
        assertEquals(DiscoveredServiceNotification.AVAILABLE, dsn.getType());
        assertEquals(Collections.emptyList(), dsn.getFilters());
        assertEquals(Collections.singleton(String.class.getName()), dsn.getInterfaces());
        ServiceEndpointDescription sed = dsn.getServiceEndpointDescription();
        assertEquals(Collections.singleton(String.class.getName()), sed.getProvidedInterfaces());
        assertEquals(s1Props, sed.getProperties());
        
        // Fourth time around, now with null
        EasyMock.reset(zk);
        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()), false, null))
            .andReturn(null).anyTimes();
        EasyMock.expect(zk.getChildren(Util.getZooKeeperPath(String.class.getName()), false))
            .andReturn(Arrays.asList("b#90#r")).anyTimes();
        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()) + "/b#90#r", false, null))
            .andReturn(s2Bytes.toByteArray()).anyTimes();
        EasyMock.replay(zk);
        notifications.clear();
        dm.processResult(Code.Ok, null, null, null);
        DiscoveredServiceNotification dsn2 = notifications.iterator().next();
        assertEquals(1, notifications.size());
        assertEquals(DiscoveredServiceNotification.AVAILABLE, dsn2.getType());
        assertEquals(Collections.emptyList(), dsn2.getFilters());
        assertEquals(Collections.singleton(String.class.getName()), dsn2.getInterfaces());
        ServiceEndpointDescription sed2 = dsn2.getServiceEndpointDescription();
        assertEquals(Collections.singleton(String.class.getName()), sed2.getProvidedInterfaces());
        assertEquals(s2Props, sed2.getProperties());

        // Fifth time around, with null again
        notifications.clear();
        dm.processResult(Code.Ok, null, null, null);
        assertEquals("No changes, so should not get any new notifications", 0, notifications.size());
    }
    
    public void testDataMonitorNoExist() throws Exception {
        ZooKeeper zk = EasyMock.createMock(ZooKeeper.class);
        zk.exists(
            EasyMock.eq(Util.getZooKeeperPath(String.class.getName())),
            EasyMock.eq(true),
            (StatCallback) EasyMock.anyObject(), 
            EasyMock.isNull());
        EasyMock.expectLastCall();
        EasyMock.replay(zk);

        DataMonitor dm = new DataMonitor(zk, String.class.getName(), null);        
        dm.processResult(Code.NoNode, null, null, null);

        EasyMock.verify(zk);
    }
    
    public void testDataMonitorDefault() {
        ZooKeeper zk = EasyMock.createMock(ZooKeeper.class);
        zk.exists(
            EasyMock.eq(Util.getZooKeeperPath(String.class.getName())),
            EasyMock.eq(true),
            (StatCallback) EasyMock.anyObject(), 
            EasyMock.isNull());
        EasyMock.expectLastCall();
        EasyMock.replay(zk);

        DataMonitor dm = new DataMonitor(zk, String.class.getName(), null);
        EasyMock.verify(zk);        
        
        EasyMock.reset(zk);
        zk.exists(Util.getZooKeeperPath(String.class.getName()), true, dm, null);
        EasyMock.expectLastCall();
        EasyMock.replay(zk);
        // This should trigger a call to zookeeper.exists() as defined above
        dm.processResult(12345, null, null, null);

        EasyMock.verify(zk);        
    }
}
