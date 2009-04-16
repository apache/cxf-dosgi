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

import java.util.Collections;

import junit.framework.TestCase;

import org.apache.zookeeper.ZooKeeper;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.DiscoveredServiceTracker;

public class FindInZooKeeperCustomizerTest extends TestCase {
    public void testAddingService() {
        DiscoveredServiceTracker dst = new DiscoveredServiceTracker() {
            public void serviceChanged(DiscoveredServiceNotification dsn) {
            }            
        };
        
        ServiceReference sr = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sr.getProperty(DiscoveredServiceTracker.PROP_KEY_MATCH_CRITERIA_INTERFACES)).
            andReturn(Collections.singleton(String.class.getName()));
        EasyMock.replay(sr);
        
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.getService(sr)).andReturn(dst);
        EasyMock.replay(bc);
        
        ZooKeeper zk = EasyMock.createNiceMock(ZooKeeper.class);
        EasyMock.replay(zk);
        
        FindInZooKeeperCustomizer fc = new FindInZooKeeperCustomizer(bc, zk);
        
        assertEquals("Precondition failed", 0, fc.watchers.size());
        fc.addingService(sr);
        assertEquals(1, fc.watchers.size());
        
        String key = fc.watchers.keySet().iterator().next();
        assertEquals(String.class.getName(), key);
        DataMonitor dm = fc.watchers.get(key);
        assertNotNull(dm.listener);
        assertSame(zk, dm.zookeeper);
        assertEquals(Util.getZooKeeperPath(String.class.getName()), dm.znode);
        
    }
}
