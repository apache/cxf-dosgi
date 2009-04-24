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
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.zookeeper.ZooKeeper;
import org.easymock.classextension.EasyMock;
import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServiceEndpointDescription;

public class InterfaceDataMonitorListenerImplTest extends TestCase {
    public void testChange() throws Exception {
        final List<DiscoveredServiceNotification> dsnCallbacks = new ArrayList<DiscoveredServiceNotification>();
        DiscoveredServiceTracker dst = new DiscoveredServiceTracker() {
            public void serviceChanged(DiscoveredServiceNotification dsn) {
                dsnCallbacks.add(dsn);
            }            
        };
        
        Properties initial = new Properties();
        initial.put("a", "b");     
        ByteArrayOutputStream propBytes = new ByteArrayOutputStream();
        initial.store(propBytes, "");
        
        ZooKeeper zk = EasyMock.createMock(ZooKeeper.class);
        EasyMock.expect(zk.getChildren(Util.getZooKeeperPath(String.class.getName()), false)).
            andReturn(Arrays.asList("x#y#z"));
        EasyMock.expect(zk.getData(Util.getZooKeeperPath(String.class.getName()) + "/x#y#z", false, null)).
            andReturn(propBytes.toByteArray());
        EasyMock.replay(zk);

        InterfaceDataMonitorListenerImpl dml = new InterfaceDataMonitorListenerImpl(zk, String.class.getName(), dst);
        
        assertEquals("Precondition failed", 0, dsnCallbacks.size());
        dml.change();
        assertEquals(1, dsnCallbacks.size());
        DiscoveredServiceNotification dsn = dsnCallbacks.iterator().next();
        assertEquals(Collections.singleton(String.class.getName()), dsn.getInterfaces());
        assertEquals(DiscoveredServiceNotification.AVAILABLE, dsn.getType());
        assertEquals(0, dsn.getFilters().size());
        ServiceEndpointDescription sed = dsn.getServiceEndpointDescription();
        assertEquals(Collections.singleton(String.class.getName()), sed.getProvidedInterfaces());        
        assertEquals(initial, sed.getProperties());
    }
}
