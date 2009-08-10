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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import junit.framework.TestCase;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.ServicePublication;

public class PublishToZooKeeperCustomizerTest extends TestCase {
    public void testAddingService() throws Exception {
        Hashtable<String, Object> srProps = new Hashtable<String, Object>();
        srProps.put("osgi.remote.interfaces", "*");
        
        String location = "http://somehost.someorg:80/abc/def";
        String eid = UUID.randomUUID().toString();
        ServiceReference sr = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sr.getProperty(ServicePublication.SERVICE_INTERFACE_NAME)).andReturn(Arrays.asList("java.lang.String", "org.example.interface.AnInterface"));
        EasyMock.expect(sr.getProperty(ServicePublication.ENDPOINT_LOCATION)).andReturn(location).atLeastOnce();
        EasyMock.expect(sr.getProperty(ServicePublication.ENDPOINT_ID)).andReturn(eid).atLeastOnce();
        EasyMock.expect(sr.getProperty(ServicePublication.SERVICE_PROPERTIES)).andReturn(srProps).anyTimes();
        EasyMock.replay(sr);

        final Properties expected = new Properties();
        expected.put("osgi.remote.interfaces", "*");
        expected.put("osgi.remote.endpoint.location", location);
        expected.put("osgi.remote.endpoint.id", eid);
        ByteArrayOutputStream expectedBytes = new ByteArrayOutputStream();
        expected.store(expectedBytes, "");

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
    
    public void testRemovedService() throws Exception {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.replay(bc);
        
        String endpoint = "http://localhost:2991/123";
        ServiceReference sr = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sr.getProperty(ServicePublication.SERVICE_INTERFACE_NAME)).andReturn(Runnable.class.getName());
        EasyMock.expect(sr.getProperty(ServicePublication.ENDPOINT_LOCATION)).andReturn(endpoint);        
        EasyMock.replay(sr);
        
        ZooKeeper zk = EasyMock.createMock(ZooKeeper.class);
        zk.delete(Util.getZooKeeperPath(Runnable.class.getName()) + "/" +
                PublishToZooKeeperCustomizer.getKey(endpoint), -1);
        EasyMock.replay(zk);
        
        PublishToZooKeeperCustomizer pc = new PublishToZooKeeperCustomizer(null, zk);
        pc.removedService(sr, null);
        
        EasyMock.verify(bc);
        EasyMock.verify(sr);
        EasyMock.verify(zk);
    }
    
    public void testModifiedService() {
        final List<Object> actual = new ArrayList<Object>();
        PublishToZooKeeperCustomizer pc = new PublishToZooKeeperCustomizer(null, null) {
            @Override
            public Object addingService(ServiceReference sr) {
                actual.add("add");
                actual.add(sr);
                return null;
            }

            @Override
            public void removedService(ServiceReference sr, Object obj) {
                actual.add("remove");
                actual.add(sr);
                actual.add(obj);
            }            
        };

        ServiceReference sr = EasyMock.createMock(ServiceReference.class);
        Object obj = new Object();
        
        assertEquals("Precondition failed", 0, actual.size());
        pc.modifiedService(sr, obj);
        
        List<Object> expected = Arrays.asList("remove", sr, obj, "add", sr);
        assertEquals(expected, actual);
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
        Map<String, Object> initial = new HashMap<String, Object>();
        initial.put("osgi.remote.interfaces", "*");
        initial.put("osgi.remote.configuration.type", "pojo");
        initial.put("osgi.remote.configuration.pojo.address", "http://localhost:9090/ps");
        
        String eid = UUID.randomUUID().toString();
        String epLoc = "http://localhost:9090/ps";
        HashMap<String, Object> expected = new HashMap<String, Object>(initial);
        expected.put(ServicePublication.ENDPOINT_ID, eid);
        expected.put(ServicePublication.ENDPOINT_LOCATION, 
            "http://" + InetAddress.getLocalHost().getHostAddress() + ":9090/ps");
        expected.put("osgi.remote.configuration.pojo.address", "http://" + 
            InetAddress.getLocalHost().getHostAddress() + ":9090/ps");
                
        ServiceReference sr = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sr.getProperty(ServicePublication.SERVICE_PROPERTIES)).andReturn(initial);
        EasyMock.expect(sr.getProperty(ServicePublication.ENDPOINT_ID)).andReturn(eid);
        EasyMock.expect(sr.getProperty(ServicePublication.ENDPOINT_LOCATION)).andReturn(epLoc);
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
    
    public void testLocalHostTranslation() {
        assertEquals("http://somehost:9090", 
            PublishToZooKeeperCustomizer.filterLocalHost("http://localhost:9090", "somehost"));
        assertEquals("http://somehost:9090/myPath", 
            PublishToZooKeeperCustomizer.filterLocalHost("http://127.0.0.1:9090/myPath", "somehost"));

        // a few negative tests too
        assertEquals("http://localhostt:9090", 
            PublishToZooKeeperCustomizer.filterLocalHost("http://localhostt:9090", "somehost"));
        assertEquals("There is a localhost on the planet.", 
            PublishToZooKeeperCustomizer.filterLocalHost("There is a localhost on the planet.", "somehost"));
    }
}
