/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cxf.dosgi.discovery.zookeeper.server;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.discovery.zookeeper.server.ManagedService.MyZooKeeperServerMain;
import org.apache.zookeeper.ZooKeeperMain;
import org.apache.zookeeper.server.ServerConfig;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;

public class ManagedServiceTest extends TestCase {
    public void testManagedService() throws Exception {
        final File tempDir = new File(System.getProperty("java.io.tmpdir"));

        final ManagedService.MyZooKeeperServerMain zkMain = 
            EasyMock.createMock(ManagedService.MyZooKeeperServerMain.class);
        zkMain.runFromConfig((ServerConfig) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                ServerConfig sc = (ServerConfig) EasyMock.getCurrentArguments()[0];
                assertEquals(new File(tempDir, "zkdata").getCanonicalFile().toString(), 
                    sc.getDataDir());
                assertEquals(1234, sc.getClientPort());
                assertEquals(2000, sc.getTickTime());
                return null;
            }
        });
        EasyMock.replay(zkMain);       
        
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.getDataFile("")).andReturn(tempDir);
        EasyMock.replay(bc);
        
        final StringBuilder threadStatus = new StringBuilder();
        ManagedService ms = new ManagedService(bc) {
            @Override
            ManagedService.MyZooKeeperServerMain getZooKeeperMain() {
                return zkMain;
            }

            @Override
            void startThread() {
                threadStatus.append("started");
            }                        
        };
        
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        final Hashtable<String, Object> expected = new Hashtable<String, Object>();        
        expected.put("tickTime", "2000");
        expected.put("initLimit", "10");
        expected.put("syncLimit", "5");
        expected.put("dataDir", new File(tempDir, "zkdata").getCanonicalFile().toString());
        expected.put(Constants.SERVICE_PID, "org.apache.cxf.dosgi.discovery.zookeeper.server");
        expected.put("clientPort", "1234");
        sreg.setProperties(expected);
        EasyMock.expectLastCall();
        EasyMock.replay(sreg);
        ms.setRegistration(sreg);
        
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "org.apache.cxf.dosgi.discovery.zookeeper.server");
        props.put("clientPort", "1234");
        
        ms.updated(props);
        EasyMock.verify(sreg);

        assertEquals("started", threadStatus.toString());
        ms.zkMainThread.run();
        
        EasyMock.verify(zkMain);
        EasyMock.verify(bc);   
        
        EasyMock.reset(zkMain);
        zkMain.shutdown();
        EasyMock.expectLastCall();
        EasyMock.verify();
        
        assertNotNull(ms.main);
        ms.shutdown();
        assertNull(ms.main);
        assertNull(ms.zkMainThread);
    }
    
    public void testRemoveConfiguration() throws Exception {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        
        final StringBuilder shutDownTracker = new StringBuilder();
        ManagedService ms = new ManagedService(bc) {
            @Override
            public synchronized void shutdown() {
                shutDownTracker.append("called");
            }            

            @Override
            void startThread() {}                        
        };
        
        assertEquals("Precondition failed", 0, shutDownTracker.length());
        ms.updated(null);
        assertEquals("called", shutDownTracker.toString());
        // check that it didn't get reinitialized TODO
    }

    public void testNewConfiguration() throws Exception {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        
        final StringBuilder shutDownTracker = new StringBuilder();
        ManagedService ms = new ManagedService(bc) {
            @Override
            public synchronized void shutdown() {
                shutDownTracker.append("called");
            }            

            @Override
            void startThread() {}                        
        };
        
        assertEquals("Precondition failed", 0, shutDownTracker.length());
        assertNull("Precondition failed", ms.main);
        assertNull("Precondition failed", ms.zkMainThread);
        
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("clientPort", "9911");
        ms.updated(props);
        assertEquals("Shutdown should not have been called", 0, shutDownTracker.length());
        assertNotNull(ms.main);
        assertNotNull(ms.zkMainThread);
    }

    public void testChangeConfiguration() throws Exception {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        
        final StringBuilder shutDownTracker = new StringBuilder();
        ManagedService ms = new ManagedService(bc) {
            @Override
            public synchronized void shutdown() {
                shutDownTracker.append("called");
            }            

            @Override
            void startThread() {}                        
        };
        
        MyZooKeeperServerMain initialMsMain = 
            EasyMock.createMock(ManagedService.MyZooKeeperServerMain.class);
        ms.main = initialMsMain;
        Thread initialZkThread = new Thread();
        ms.zkMainThread = initialZkThread;
        
        assertEquals("Precondition failed", 0, shutDownTracker.length());
        assertNotNull("Precondition failed", ms.main);
        assertNotNull("Precondition failed", ms.zkMainThread);
        
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("clientPort", "9911");
        ms.updated(props);
        assertEquals("We are reconfiguring, so shutdown should be called", 
                "called", shutDownTracker.toString());
        assertNotNull(ms.main);
        assertNotNull(ms.zkMainThread);
        assertNotSame(ms.main, initialMsMain);
        assertNotSame(ms.zkMainThread, initialZkThread);
    }
}
