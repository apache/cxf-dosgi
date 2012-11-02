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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class EndpointListenerImplTest extends TestCase {

    public void testEndpointRemovalAdding() throws KeeperException, InterruptedException {

        IMocksControl c = EasyMock.createNiceControl();

        BundleContext ctx = c.createMock(BundleContext.class);
        ZooKeeper zk = c.createMock(ZooKeeper.class);

        String path = "/osgi/service_registry/myClass/google.de#80##test";
        EasyMock.expect(
                        zk.create(EasyMock.eq(path),
                                  (byte[])EasyMock.anyObject(), EasyMock.eq(Ids.OPEN_ACL_UNSAFE), EasyMock
                                      .eq(CreateMode.EPHEMERAL))).andReturn("").once();

        zk.delete(EasyMock.eq("/osgi/service_registry/myClass/google.de#80##test"), EasyMock.eq(-1));
        EasyMock.expectLastCall().once();

        c.replay();

        PublishingEndpointListener eli = new PublishingEndpointListener(zk, ctx);

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

    public void testDiscoveryPlugin() throws Exception {
        DiscoveryPlugin plugin1 = new DiscoveryPlugin() {
            public String process(Map<String, Object> mutableProperties, String endpointKey) {
                String eid = (String) mutableProperties.get("endpoint.id");
                mutableProperties.put("endpoint.id", eid + "/appended");
                return endpointKey;
            }
        };
        ServiceReference sr1 = EasyMock.createMock(ServiceReference.class);

        DiscoveryPlugin plugin2 = new DiscoveryPlugin() {
            public String process(Map<String, Object> mutableProperties, String endpointKey) {
                mutableProperties.put("foo", "bar");
                return endpointKey.replaceAll("localhost", "some.machine");
            }
        };
        ServiceReference sr2 = EasyMock.createMock(ServiceReference.class);

        BundleContext ctx = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(ctx.createFilter(EasyMock.isA(String.class))).andAnswer(new IAnswer<Filter>() {
            public Filter answer() throws Throwable {
                return FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        ctx.addServiceListener(EasyMock.isA(ServiceListener.class),
                EasyMock.eq("(objectClass=org.apache.cxf.dosgi.discovery.zookeeper.DiscoveryPlugin)"));
        EasyMock.expect(ctx.getService(sr1)).andReturn(plugin1).anyTimes();
        EasyMock.expect(ctx.getService(sr2)).andReturn(plugin2).anyTimes();
        EasyMock.expect(ctx.getServiceReferences(DiscoveryPlugin.class.getName(), null)).andReturn(
                new ServiceReference[] {sr1, sr2}).anyTimes();
        EasyMock.replay(ctx);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.OBJECTCLASS, new String[] { "org.foo.myClass" });
        props.put(RemoteConstants.ENDPOINT_ID, "http://localhost:9876/test");
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "myConfig");
        EndpointDescription ep = new EndpointDescription(props);

        Map<String, Object> expectedProps = new HashMap<String, Object>(props);
        expectedProps.put("endpoint.id", "http://localhost:9876/test/appended");
        expectedProps.put("foo", "bar");
        expectedProps.put("service.imported", "true");

        final ZooKeeper zk = EasyMock.createNiceMock(ZooKeeper.class);
        String expectedFullPath = "/osgi/service_registry/org/foo/myClass/some.machine#9876##test";
        EasyMock.expect(zk.create(
                EasyMock.eq(expectedFullPath),
                EasyMock.aryEq(PublishingEndpointListener.getData(expectedProps)),
                EasyMock.eq(Ids.OPEN_ACL_UNSAFE),
                EasyMock.eq(CreateMode.EPHEMERAL))).andReturn("");
        EasyMock.replay(zk);

        PublishingEndpointListener eli = new PublishingEndpointListener(zk, ctx);

        List<EndpointDescription> endpointList = getEndpointsList(eli);
        assertEquals("Precondition", 0, endpointList.size());
        eli.endpointAdded(ep, null);
        assertEquals(1, endpointList.size());

        EasyMock.verify(zk);
    }


    @SuppressWarnings("unchecked")
    private List<EndpointDescription> getEndpointsList(PublishingEndpointListener eli) throws Exception {
        Field field = eli.getClass().getDeclaredField("endpoints");
        field.setAccessible(true);
        return (List<EndpointDescription>) field.get(eli);
    }

    public void testClose() throws KeeperException, InterruptedException{

        IMocksControl c = EasyMock.createNiceControl();

        BundleContext ctx = c.createMock(BundleContext.class);
        ZooKeeper zk = c.createMock(ZooKeeper.class);

        String path = "/osgi/service_registry/myClass/google.de#80##test";
        EasyMock.expect(
                        zk.create(EasyMock.eq(path),
                                  (byte[])EasyMock.anyObject(), EasyMock.eq(Ids.OPEN_ACL_UNSAFE), EasyMock
                                      .eq(CreateMode.EPHEMERAL))).andReturn("").once();

        zk.delete(EasyMock.eq("/osgi/service_registry/myClass/google.de#80##test"), EasyMock.eq(-1));
        EasyMock.expectLastCall().once();

        c.replay();

        PublishingEndpointListener eli = new PublishingEndpointListener(zk, ctx);

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
            PublishingEndpointListener.getKey("http://somehost:9090/org/example/TestEndpoint"));
    }

}
