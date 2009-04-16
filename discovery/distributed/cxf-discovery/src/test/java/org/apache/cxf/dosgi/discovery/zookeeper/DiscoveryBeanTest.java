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

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.zookeeper.ZooKeeper;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServicePublication;
import org.osgi.util.tracker.ServiceTracker;

public class DiscoveryBeanTest extends TestCase {
    public void testDiscoveryBean() throws Exception {
        final StringBuilder sb = new StringBuilder();
        DiscoveryBean db = new DiscoveryBean() {
            @Override
            ZooKeeper createZooKeeper(String hostPort) throws IOException {
                sb.append(hostPort);
                ZooKeeper zk = EasyMock.createMock(ZooKeeper.class);
                EasyMock.replay(zk);
                return zk;
            }            
        };
        
        DiscoveryServiceImpl ds = new DiscoveryServiceImpl();
        ds.setZooKeeperHost("myhost.mymachine.mytld");
        ds.setZooKeeperPort(1234);
        db.setDiscoveryServiceBean(ds);
        
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        expectServiceTrackerCalls(bc, ServicePublication.class.getName());
        expectServiceTrackerCalls(bc, DiscoveredServiceTracker.class.getName());
        EasyMock.replay(bc);
        db.setBundleContext(bc);
        
        assertNull("Precondition failed", db.lookupTracker);
        assertNull("Precondition failed", db.publicationTracker);
        db.afterPropertiesSet();
        assertNotNull(db.lookupTracker);
        assertNotNull(db.publicationTracker);
        EasyMock.verify(bc);
        
        EasyMock.verify(db.zooKeeper);
        EasyMock.reset(db.zooKeeper);
        db.zooKeeper.close();
        EasyMock.expectLastCall();
        EasyMock.replay(db.zooKeeper);
        
        ServiceTracker st1 = EasyMock.createMock(ServiceTracker.class);
        st1.close();
        EasyMock.expectLastCall();
        EasyMock.replay(st1);
        ServiceTracker st2 = EasyMock.createMock(ServiceTracker.class);
        st2.close();
        EasyMock.expectLastCall();
        EasyMock.replay(st2);
        
        db.lookupTracker = st1;
        db.publicationTracker = st2;
        
        db.destroy();        
    }

    private void expectServiceTrackerCalls(BundleContext bc, String objectClass)
            throws InvalidSyntaxException {
        Filter filter = EasyMock.createNiceMock(Filter.class);
        EasyMock.replay(filter);
        
        EasyMock.expect(bc.createFilter("(objectClass=" + objectClass + ")"))
            .andReturn(filter).anyTimes();
        bc.addServiceListener((ServiceListener) EasyMock.anyObject(), 
            EasyMock.eq("(objectClass=" + objectClass + ")"));
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(bc.getServiceReferences(objectClass, null))
            .andReturn(new ServiceReference [0]).anyTimes();
    }
}
