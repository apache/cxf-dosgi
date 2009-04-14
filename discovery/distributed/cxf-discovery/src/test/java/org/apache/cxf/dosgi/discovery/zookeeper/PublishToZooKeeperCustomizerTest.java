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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class PublishToZooKeeperCustomizerTest extends TestCase {
    public void testAddingService() throws Exception {
        Hashtable<String, Object> srProps = new Hashtable<String, Object>();
        srProps.put("osgi.remote.interfaces", "*");
        
        final Properties expected = new Properties();
        expected.put("osgi.remote.interfaces", "*");
        ByteArrayOutputStream expectedBytes = new ByteArrayOutputStream();
        expected.store(expectedBytes, "");

        ServiceReference sr = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sr.getProperty("service.interface")).andReturn(Arrays.asList("java.lang.String", "org.example.interface.AnInterface"));
        EasyMock.expect(sr.getProperty("osgi.remote.endpoint.location")).andReturn("http://somehost.someorg:80/abc/def");
        EasyMock.expect(sr.getProperty("service.properties")).andReturn(srProps).anyTimes();
        EasyMock.replay(sr);

        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.getService(sr)).andReturn("something");
        EasyMock.replay(bc);
        
        ZooKeeper zk = EasyMock.createNiceMock(ZooKeeper.class);
        // Expect create calls for both interfaces
        EasyMock.expect(zk.create(EasyMock.eq(Util.PATH_PREFIX + "java/lang/String"),
                (byte[]) EasyMock.aryEq(new byte[0]), 
                (List<ACL>) EasyMock.eq(Ids.OPEN_ACL_UNSAFE), 
                (CreateMode) EasyMock.eq(CreateMode.PERSISTENT))).andReturn(null);
        EasyMock.expect(zk.create(EasyMock.eq(Util.PATH_PREFIX + "java/lang/String/somehost.someorg#80##abc#def"),
                (byte[]) EasyMock.anyObject(), 
                (List<ACL>) EasyMock.eq(Ids.OPEN_ACL_UNSAFE), 
                (CreateMode) EasyMock.eq(CreateMode.EPHEMERAL))).andAnswer(new IAnswer<String>() {
                    public String answer() throws Throwable {
                        byte [] b = (byte[]) EasyMock.getCurrentArguments()[1];
                        Properties actual = new Properties();
                        actual.load(new ByteArrayInputStream(b));
                        assertEquals(expected, actual);
                        
                        return null;
                    }                    
                });

        EasyMock.expect(zk.create(EasyMock.eq(Util.PATH_PREFIX + "org/example/interface/AnInterface"),
                (byte[]) EasyMock.aryEq(new byte[0]), 
                (List<ACL>) EasyMock.eq(Ids.OPEN_ACL_UNSAFE), 
                (CreateMode) EasyMock.eq(CreateMode.PERSISTENT))).andReturn(null);
        EasyMock.expect(zk.create(EasyMock.eq(Util.PATH_PREFIX + "org/example/interface/AnInterface/somehost.someorg#80##abc#def"),
                (byte[]) EasyMock.anyObject(), 
                (List<ACL>) EasyMock.eq(Ids.OPEN_ACL_UNSAFE), 
                (CreateMode) EasyMock.eq(CreateMode.EPHEMERAL))).andAnswer(new IAnswer<String>() {
                    public String answer() throws Throwable {
                        byte [] b = (byte[]) EasyMock.getCurrentArguments()[1];
                        Properties actual = new Properties();
                        actual.load(new ByteArrayInputStream(b));
                        assertEquals(expected, actual);
                        
                        return null;
                    }                    
                });
        EasyMock.replay(zk);
        
        PublishToZooKeeperCustomizer pc = new PublishToZooKeeperCustomizer(bc, zk);                
        pc.addingService(sr);
        
        EasyMock.verify(sr);
        EasyMock.verify(bc);
        EasyMock.verify(zk);
    }
    
    public void testEnsurePath() throws Exception {
        ZooKeeper zk = EasyMock.createStrictMock(ZooKeeper.class);
        EasyMock.expect(zk.exists((String) EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(null);
        expectCreateCall(zk, "/a");
        EasyMock.expect(zk.exists((String) EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(null);
        expectCreateCall(zk, "/a/b");
        EasyMock.expect(zk.exists((String) EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(null);
        expectCreateCall(zk, "/a/b/c");
        EasyMock.expect(zk.exists((String) EasyMock.anyObject(), EasyMock.anyBoolean())).andReturn(null);
        expectCreateCall(zk, "/a/b/c/D");
        EasyMock.replay(zk);
        
        PublishToZooKeeperCustomizer pc = new PublishToZooKeeperCustomizer(null, zk);
        pc.ensurePath("/a/b/c/D");
        EasyMock.verify(zk);
    }
    
    private static void expectCreateCall(ZooKeeper zk, String path) throws Exception {
        EasyMock.expect(zk.create(EasyMock.eq(path),
                (byte[]) EasyMock.aryEq(new byte[0]), 
                (List<ACL>) EasyMock.eq(Ids.OPEN_ACL_UNSAFE), 
                (CreateMode) EasyMock.eq(CreateMode.PERSISTENT))).andReturn(path);
    }

    public void testGetData() throws Exception {
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("osgi.remote.interfaces", "*");
        expected.put("osgi.remote.configuration.type", "pojo");
        expected.put("osgi.remote.configuration.pojo.address", "http://localhost:9090/ps");
                
        ServiceReference sr = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sr.getProperty("service.properties")).andReturn(expected);
        EasyMock.replay(sr);
        
        byte[] data = PublishToZooKeeperCustomizer.getData(sr);
        
        Properties actual = new Properties();
        actual.load(new ByteArrayInputStream(data));
        
        assertEquals(expected, actual);
        EasyMock.verify(sr);
    }
    
    public void testGetKey() throws Exception {
        assertEquals("somehost#9090##org#example#TestEndpoint", 
            PublishToZooKeeperCustomizer.getKey("http://somehost:9090/org/example/TestEndpoint"));
    }
    
    public void testGetKeyLocalhost() throws Exception {
        String hostAddr = InetAddress.getLocalHost().getHostAddress();
        assertEquals(hostAddr + "#8000##ps",
            PublishToZooKeeperCustomizer.getKey("http://localhost:8000/ps"));
    }
}
