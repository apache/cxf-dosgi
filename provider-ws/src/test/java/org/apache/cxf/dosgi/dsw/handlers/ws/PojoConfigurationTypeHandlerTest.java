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
package org.apache.cxf.dosgi.dsw.handlers.ws;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.aries.rsa.spi.Endpoint;
import org.apache.aries.rsa.util.EndpointHelper;
import org.apache.cxf.dosgi.common.httpservice.HttpServiceManager;
import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.dosgi.common.intent.IntentManagerImpl;
import org.apache.cxf.dosgi.common.util.ServerEndpoint;
import org.apache.cxf.dosgi.dsw.handlers.jaxws.MyJaxWsEchoService;
import org.apache.cxf.dosgi.dsw.handlers.simple.MySimpleEchoService;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import junit.framework.TestCase;

public class PojoConfigurationTypeHandlerTest extends TestCase {

    public void testGetPojoAddressEndpointURI() {
        IntentManager intentManager = new IntentManagerImpl();
        WsProvider handler = new WsProvider();
        handler.setIntentManager(intentManager);
        handler.setHttpServiceManager(dummyHttpServiceManager());
        Map<String, Object> sd = new HashMap<String, Object>();
        String url = "http://somewhere:1234/blah";
        sd.put(RemoteConstants.ENDPOINT_ID, url);
        assertEquals(url, handler.getServerAddress(sd, String.class));
    }

    private HttpServiceManager dummyHttpServiceManager() {
        return new HttpServiceManager();
    }

    public void testGetPojoAddressEndpointCxf() {
        IntentManager intentManager = new IntentManagerImpl();
        WsProvider handler = new WsProvider();
        handler.setIntentManager(intentManager);
        handler.setHttpServiceManager(dummyHttpServiceManager());
        Map<String, Object> sd = new HashMap<String, Object>();
        String url = "http://somewhere:29/boo";
        sd.put("org.apache.cxf.ws.address", url);
        assertEquals(url, handler.getServerAddress(sd, String.class));
    }

    public void testGetDefaultPojoAddress() {
        IntentManager intentManager = new IntentManagerImpl();
        WsProvider handler = new WsProvider();
        handler.setIntentManager(intentManager);
        handler.setHttpServiceManager(dummyHttpServiceManager());
        Map<String, Object> sd = new HashMap<String, Object>();
        assertEquals("/java/lang/String", handler.getServerAddress(sd, String.class));
    }

    // todo: add test for data bindings
    public void testCreateProxy() {
        IMocksControl c = EasyMock.createNiceControl();
        BundleContext bc1 = c.createMock(BundleContext.class);
        
        BundleContext requestingContext = c.createMock(BundleContext.class);

        final ClientProxyFactoryBean cpfb = c.createMock(ClientProxyFactoryBean.class);
        ReflectionServiceFactoryBean sf = c.createMock(ReflectionServiceFactoryBean.class);
        EasyMock.expect(cpfb.getServiceFactory()).andReturn(sf).anyTimes();
        IntentManager intentManager = new IntentManagerImpl() {
            @Override
            public String[] applyIntents(List<Feature> features,
                                         AbstractEndpointFactory factory,
                                         Map<String, Object> sd) {
                return new String[0];
            }
        };
        WsProvider p = new WsProvider() {
            @Override
            protected ClientProxyFactoryBean createClientProxyFactoryBean(Map<String, Object> sd, Class<?> iClass) {
                return cpfb;
            }
        };
        p.setIntentManager(intentManager);
        p.setHttpServiceManager(dummyHttpServiceManager());
        p.activate(bc1);

        Class<?>[] exportedInterfaces = new Class[]{Runnable.class};
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.ENDPOINT_ID, "http://google.de/");
        EndpointHelper.addObjectClass(props, exportedInterfaces);
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[]{"my.config"});
        EndpointDescription endpoint = new EndpointDescription(props);

        cpfb.setAddress((String)EasyMock.eq(props.get(RemoteConstants.ENDPOINT_ID)));
        EasyMock.expectLastCall().atLeastOnce();

        cpfb.setServiceClass(EasyMock.eq(Runnable.class));
        EasyMock.expectLastCall().atLeastOnce();

        c.replay();
        ClassLoader cl = null;
        Object proxy = p.importEndpoint(cl, requestingContext, exportedInterfaces, endpoint);
        assertNotNull(proxy);
        assertTrue("Proxy is not of the requested type! ", proxy instanceof Runnable);
        c.verify();
    }

    public void testCreateServerWithAddressProperty() throws Exception {
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(dswContext);

        String myService = "Hi";
        final ServerFactoryBean sfb = createMockServerFactoryBean();

        IntentManager intentManager = new IntentManagerImpl() {
            @Override
            public String[] applyIntents(List<Feature> features, AbstractEndpointFactory factory,
                                         Map<String, Object> sd) {
                return new String[]{};
            }
        };
        WsProvider p = new WsProvider() {
            @Override
            protected ServerFactoryBean createServerFactoryBean(Map<String, Object> sd, Class<?> iClass) {
                return sfb;
            }
        };
        p.activate(dswContext);
        p.setIntentManager(intentManager);
        p.setHttpServiceManager(dummyHttpServiceManager());
        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bundleContext);
        
        Class<?>[] exportedInterface = new Class[]{String.class};
        Map<String, Object> props = new HashMap<String, Object>();
        EndpointHelper.addObjectClass(props, exportedInterface);
        props.put(WsConstants.WS_ADDRESS_PROPERTY, "http://alternate_host:80/myString");

        Endpoint exportResult = p.exportService(myService, bundleContext, props, exportedInterface);
        Map<String, Object> edProps = exportResult.description().getProperties();

        assertNotNull(edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS));
        assertEquals(1, ((String[])edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS)).length);
        assertEquals(WsConstants.WS_CONFIG_TYPE, ((String[])edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS))[0]);
        assertEquals("http://alternate_host:80/myString", edProps.get(RemoteConstants.ENDPOINT_ID));
    }

    public void testAddressing() throws Exception {
        runAddressingTest(new HashMap<String, Object>(), "http://localhost:9000/java/lang/Runnable");

        Map<String, Object> p1 = new HashMap<String, Object>();
        p1.put("org.apache.cxf.ws.address", "http://somewhere");
        runAddressingTest(p1, "http://somewhere");

        Map<String, Object> p3 = new HashMap<String, Object>();
        p3.put("org.apache.cxf.ws.port", 65535);
        runAddressingTest(p3, "http://localhost:65535/java/lang/Runnable");

        Map<String, Object> p4 = new HashMap<String, Object>();
        p4.put("org.apache.cxf.ws.port", "8181");
        runAddressingTest(p4, "http://localhost:8181/java/lang/Runnable");
    }

    private void runAddressingTest(Map<String, Object> properties, String expectedAddress) throws Exception {
        Class<?>[] exportedInterface = new Class[]{Runnable.class};
        EndpointHelper.addObjectClass(properties, exportedInterface);
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        String expectedUUID = UUID.randomUUID().toString();
        EasyMock.expect(dswContext.getProperty(org.osgi.framework.Constants.FRAMEWORK_UUID)).andReturn(expectedUUID);
        EasyMock.replay(dswContext);

        IntentManager intentManager = EasyMock.createNiceMock(IntentManager.class);
        EasyMock.replay(intentManager);

        WsProvider handler = new WsProvider() {
            @Override
            protected Endpoint createServerFromFactory(ServerFactoryBean factory,
                                                       EndpointDescription epd) {
                return new ServerEndpoint(epd, null);
            }
        };
        handler.setIntentManager(intentManager);
        handler.setHttpServiceManager(dummyHttpServiceManager());
        handler.activate(dswContext);
        Runnable myService = EasyMock.createMock(Runnable.class);
        EasyMock.replay(myService);
        
        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bundleContext);

        Endpoint result = handler.exportService(myService, bundleContext, properties, exportedInterface);
        Map<String, Object> props = result.description().getProperties();
        assertEquals(expectedAddress, props.get("org.apache.cxf.ws.address"));
        Assert.assertArrayEquals(new String[] {"org.apache.cxf.ws"}, 
                     (String[]) props.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS));
        Assert.assertArrayEquals(new String[] {"java.lang.Runnable"}, 
                     (String[]) props.get(org.osgi.framework.Constants.OBJECTCLASS));
    }

    public void t2estCreateServerException() {
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(dswContext);

        IntentManager intentManager = EasyMock.createNiceMock(IntentManager.class);
        EasyMock.replay(intentManager);

        WsProvider handler = new WsProvider() {
            @Override
            protected Endpoint createServerFromFactory(ServerFactoryBean factory,
                                                       EndpointDescription epd) {
                throw new TestException();
            }
        };
        handler.setIntentManager(intentManager);
        handler.setHttpServiceManager(dummyHttpServiceManager());
        handler.activate(dswContext);

        Map<String, Object> props = new HashMap<String, Object>();

        Runnable myService = EasyMock.createMock(Runnable.class);
        EasyMock.replay(myService);
        try {
            handler.exportService(myService, null, props, new Class[]{Runnable.class});
            fail("Expected TestException");
        } catch (TestException e) {
            // Expected
        }
    }

    private ServerFactoryBean createMockServerFactoryBean() {
        ReflectionServiceFactoryBean sf = EasyMock.createNiceMock(ReflectionServiceFactoryBean.class);
        EasyMock.replay(sf);

        final StringBuilder serverURI = new StringBuilder();

        ServerFactoryBean sfb = EasyMock.createNiceMock(ServerFactoryBean.class);
        Server server = createMockServer(sfb);

        EasyMock.expect(sfb.getServiceFactory()).andReturn(sf).anyTimes();
        EasyMock.expect(sfb.create()).andReturn(server);
        sfb.setAddress((String) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                serverURI.setLength(0);
                serverURI.append(EasyMock.getCurrentArguments()[0]);
                return null;
            }
        });
        EasyMock.expect(sfb.getAddress()).andAnswer(new IAnswer<String>() {
            public String answer() throws Throwable {
                return serverURI.toString();
            }
        });
        EasyMock.replay(sfb);
        return sfb;
    }

    private Server createMockServer(final ServerFactoryBean sfb) {
        AttributedURIType addr = EasyMock.createMock(AttributedURIType.class);
        EasyMock.expect(addr.getValue()).andAnswer(new IAnswer<String>() {
            public String answer() throws Throwable {
                return sfb.getAddress();
            }
        });
        EasyMock.replay(addr);

        EndpointReferenceType er = EasyMock.createMock(EndpointReferenceType.class);
        EasyMock.expect(er.getAddress()).andReturn(addr);
        EasyMock.replay(er);

        Destination destination = EasyMock.createMock(Destination.class);
        EasyMock.expect(destination.getAddress()).andReturn(er);
        EasyMock.replay(destination);

        Server server = EasyMock.createNiceMock(Server.class);
        EasyMock.expect(server.getDestination()).andReturn(destination);
        EasyMock.replay(server);
        return server;
    }

    public void testCreateEndpointProps() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getProperty("org.osgi.framework.uuid")).andReturn("some_uuid1");
        EasyMock.replay(bc);

        IntentManager intentManager = new IntentManagerImpl();
        WsProvider pch = new WsProvider();
        pch.setIntentManager(intentManager);
        pch.setHttpServiceManager(dummyHttpServiceManager());
        pch.activate(bc);
        Class<?>[] exportedInterfaces = new Class[] {String.class};
        Map<String, Object> sd = new HashMap<String, Object>();
        sd.put(org.osgi.framework.Constants.SERVICE_ID, 42);
        EndpointHelper.addObjectClass(sd, exportedInterfaces);
        EndpointDescription epd = pch.createEndpointDesc(sd, new String[] {"org.apache.cxf.ws"},
                "http://localhost:12345", new String[] {"my_intent", "your_intent"});

        assertEquals("http://localhost:12345", epd.getId());
        assertEquals(Arrays.asList("java.lang.String"), epd.getInterfaces());
        assertEquals(Arrays.asList("org.apache.cxf.ws"), epd.getConfigurationTypes());
        assertEquals(Arrays.asList("my_intent", "your_intent"), epd.getIntents());
        assertEquals(new Version("0.0.0"), epd.getPackageVersion("java.lang"));
    }

    public void t2estCreateJaxWsEndpointWithoutIntents() {
        IMocksControl c = EasyMock.createNiceControl();
        BundleContext dswBC = c.createMock(BundleContext.class);
        
        IntentManager intentManager = new DummyIntentManager();
        WsProvider handler = new WsProvider();
        handler.setIntentManager(intentManager);
        handler.setHttpServiceManager(dummyHttpServiceManager());
        handler.activate(dswBC);

        Map<String, Object> sd = new HashMap<String, Object>();
        sd.put(WsConstants.WS_ADDRESS_PROPERTY, "/somewhere");
        BundleContext serviceBC = c.createMock(BundleContext.class);
        Object myService = null;
        c.replay();

        ServerEndpoint serverWrapper = (ServerEndpoint)handler.exportService(myService,
                                                                           serviceBC, 
                                                                           sd, 
                                                                           new Class[]{MyJaxWsEchoService.class});
        c.verify();

        org.apache.cxf.endpoint.Endpoint ep = serverWrapper.getServer().getEndpoint();
        QName bindingName = ep.getEndpointInfo().getBinding().getName();
        Assert.assertEquals(JaxWsEndpointImpl.class, ep.getClass());
        Assert.assertEquals(new QName("http://jaxws.handlers.dsw.dosgi.cxf.apache.org/",
                                      "MyJaxWsEchoServiceServiceSoapBinding"),
                            bindingName);
    }

    public void t2estCreateSimpleEndpointWithoutIntents() {
        IMocksControl c = EasyMock.createNiceControl();
        BundleContext dswBC = c.createMock(BundleContext.class);

        IntentManager intentManager = new DummyIntentManager();
        WsProvider handler = new WsProvider();
        handler.setIntentManager(intentManager);
        handler.setHttpServiceManager(dummyHttpServiceManager());
        handler.activate(dswBC);
        Map<String, Object> sd = new HashMap<String, Object>();
        sd.put(WsConstants.WS_ADDRESS_PROPERTY, "/somewhere_else");
        BundleContext serviceBC = c.createMock(BundleContext.class);
        c.replay();
        ServerEndpoint serverWrapper = (ServerEndpoint)handler.exportService(null, serviceBC, sd, 
                                                                          new Class[]{MySimpleEchoService.class});
        c.verify();

        org.apache.cxf.endpoint.Endpoint ep = serverWrapper.getServer().getEndpoint();
        QName bindingName = ep.getEndpointInfo().getBinding().getName();
        Assert.assertEquals(EndpointImpl.class, ep.getClass());
        Assert.assertEquals(new QName("http://simple.handlers.dsw.dosgi.cxf.apache.org/",
                                      "MySimpleEchoServiceSoapBinding"),
                            bindingName);
    }

    public static class DummyIntentManager implements IntentManager {

        @Override
        public String[] applyIntents(List<Feature> features,
                                     AbstractEndpointFactory factory,
                                     Map<String, Object> props) {
            return new String[]{};
        }

        @Override
        public void assertAllIntentsSupported(Map<String, Object> serviceProperties) {
        }
    }

    @SuppressWarnings("serial")
    public static class TestException extends RuntimeException {
    }
}
