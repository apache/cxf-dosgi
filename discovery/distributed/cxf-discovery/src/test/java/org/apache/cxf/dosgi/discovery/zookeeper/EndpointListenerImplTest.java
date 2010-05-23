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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class EndpointListenerImplTest extends TestCase {

    public void testEndpointRemovalAdding() throws KeeperException, InterruptedException {

        IMocksControl c = EasyMock.createNiceControl();

        BundleContext ctx = c.createMock(BundleContext.class);
        ZooKeeperDiscovery zkd = c.createMock(ZooKeeperDiscovery.class);

        ZooKeeper zk = c.createMock(ZooKeeper.class);

        EasyMock.expect(zkd.getZookeeper()).andReturn(zk).anyTimes();

        
        String path = "/osgi/service_registry/myClass/google.de#80##test";
        EasyMock.expect(
                        zk.create(EasyMock.eq(path),
                                  (byte[])EasyMock.anyObject(), EasyMock.eq(Ids.OPEN_ACL_UNSAFE), EasyMock
                                      .eq(CreateMode.EPHEMERAL))).andReturn("").once();
        
        zk.delete(EasyMock.eq("/osgi/service_registry/myClass/google.de#80##test"), EasyMock.eq(-1));
        EasyMock.expectLastCall().once();

        c.replay();

        EndpointListenerImpl eli = new EndpointListenerImpl(zkd, ctx);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.OBJECTCLASS, new String[] {
            "myClass"
        });
        props.put(RemoteConstants.ENDPOINT_ID, "http://google.de:80/test");
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "myConfig");

        EndpointDescription ed = new EndpointDescription(props);

        eli.endpointAdded(ed, null);
        eli.endpointAdded(ed, null);  // should do nothing

        eli.endpointRemoved(ed, null);
        eli.endpointRemoved(ed, null); // should do nothing
        
        c.verify();

    }
    
    
    public void testClose() throws KeeperException, InterruptedException{
        
        
        IMocksControl c = EasyMock.createNiceControl();

        BundleContext ctx = c.createMock(BundleContext.class);
        ZooKeeperDiscovery zkd = c.createMock(ZooKeeperDiscovery.class);

        ZooKeeper zk = c.createMock(ZooKeeper.class);

        EasyMock.expect(zkd.getZookeeper()).andReturn(zk).anyTimes();

        
        String path = "/osgi/service_registry/myClass/google.de#80##test";
        EasyMock.expect(
                        zk.create(EasyMock.eq(path),
                                  (byte[])EasyMock.anyObject(), EasyMock.eq(Ids.OPEN_ACL_UNSAFE), EasyMock
                                      .eq(CreateMode.EPHEMERAL))).andReturn("").once();
        
        zk.delete(EasyMock.eq("/osgi/service_registry/myClass/google.de#80##test"), EasyMock.eq(-1));
        EasyMock.expectLastCall().once();

        c.replay();

        EndpointListenerImpl eli = new EndpointListenerImpl(zkd, ctx);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.OBJECTCLASS, new String[] {
            "myClass"
        });
        props.put(RemoteConstants.ENDPOINT_ID, "http://google.de:80/test");
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "myConfig");

        EndpointDescription ed = new EndpointDescription(props);

        eli.endpointAdded(ed, null);

        eli.close(); // should result in zk.delete(...)
        
        c.verify();
        
    }
    
    
    public void testGetKey() throws Exception {
      assertEquals("somehost#9090##org#example#TestEndpoint", 
          EndpointListenerImpl.getKey("http://somehost:9090/org/example/TestEndpoint"));
  }

}
