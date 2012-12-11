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
package org.apache.cxf.dosgi.discovery.zookeeper.server;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.discovery.zookeeper.server.ZookeeperStarter.MyZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.osgi.framework.BundleContext;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;


public class ZookeeperStarterTest extends TestCase {
    public void testUpdateConfig() throws Exception {
        final File tempDir = new File("target");
        IMocksControl control = EasyMock.createControl();
        BundleContext bc = control.createMock(BundleContext.class);
        expect(bc.getDataFile("")).andReturn(tempDir);
        final MyZooKeeperServerMain mockServer = control.createMock(MyZooKeeperServerMain.class);
        control.replay();
        
        ZookeeperStarter starter = new ZookeeperStarter(bc) {

            @Override
            protected void startFromConfig(QuorumPeerConfig config) throws IOException, InterruptedException {
                assertEquals(1234, config.getClientPortAddress().getPort());
                assertTrue(config.getDataDir().contains(tempDir + File.separator + "zkdata"));
                assertEquals(2000, config.getTickTime());
                assertEquals(10, config.getInitLimit());
                assertEquals(5, config.getSyncLimit());
                this.main = mockServer;
            }

        };
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("clientPort", "1234");
        starter.updated(props);
        assertNotNull(starter.main);

        control.verify();
    }
    
    public void testRemoveConfiguration() throws Exception {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        MyZooKeeperServerMain zkServer = EasyMock.createMock(MyZooKeeperServerMain.class);
        zkServer.shutdown();
        EasyMock.expectLastCall();
        
        replay(zkServer);

        ZookeeperStarter starter = new ZookeeperStarter(bc);
        starter.main = zkServer;
        starter.updated(null);
        
        verify(zkServer);
        assertNull("main should be null", starter.main);
    }

}
