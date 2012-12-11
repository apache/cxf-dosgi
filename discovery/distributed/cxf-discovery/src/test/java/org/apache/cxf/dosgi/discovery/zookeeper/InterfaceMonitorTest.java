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

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointListener;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

public class InterfaceMonitorTest extends TestCase {

    public void testInterfaceMonitor() throws KeeperException, InterruptedException {

        IMocksControl c = EasyMock.createControl();

        BundleContext ctx = c.createMock(BundleContext.class);
        ZooKeeper zk = c.createMock(ZooKeeper.class);
        expect(zk.getState()).andReturn(ZooKeeper.States.CONNECTED).anyTimes();

        String scope = "(myProp=test)";
        String interf = "es.schaaf.test";
        String node = Util.getZooKeeperPath(interf);

        EndpointListener epListener = c.createMock(EndpointListener.class);
        InterfaceMonitor im = new InterfaceMonitor(zk, interf, epListener, scope, ctx);
        zk.exists(eq(node), eq(im), eq(im), EasyMock.anyObject());
        EasyMock.expectLastCall().once();

        expect(zk.exists(eq(node), eq(false))).andReturn(new Stat()).anyTimes();
        expect(zk.getChildren(eq(node), eq(false))).andReturn(Collections.<String> emptyList()).once();
        expect(zk.getChildren(eq(node), eq(im))).andReturn(Collections.<String> emptyList()).once();

        c.replay();
        im.start();
        // simulate a zk callback
        WatchedEvent we = new WatchedEvent(EventType.NodeCreated, KeeperState.SyncConnected, node);
        im.process(we);
        c.verify();
    }
}
