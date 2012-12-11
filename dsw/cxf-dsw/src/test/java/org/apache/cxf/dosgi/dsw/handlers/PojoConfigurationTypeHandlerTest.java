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
package org.apache.cxf.dosgi.dsw.handlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.handlers.jaxws.MyJaxWsEchoService;
import org.apache.cxf.dosgi.dsw.handlers.jaxws.MyJaxWsEchoServiceImpl;
import org.apache.cxf.dosgi.dsw.handlers.simple.MySimpleEchoService;
import org.apache.cxf.dosgi.dsw.handlers.simple.MySimpleEchoServiceImpl;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentManagerImpl;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;


public class PojoConfigurationTypeHandlerTest extends TestCase {
    public void testGetPojoAddressEndpointURI() {
        IntentManager intentManager = new IntentManagerImpl(new IntentMap());
        PojoConfigurationTypeHandler handler = new PojoConfigurationTypeHandler(null, 
                                                                                intentManager, 
                                                                                dummyHttpServiceManager());
        Map<String, Object> sd = new HashMap<String, Object>();
        String url = "http://somewhere:1234/blah";
        sd.put(RemoteConstants.ENDPOINT_ID, url);
        assertEquals(url, handler.getServerAddress(sd, String.class));
    }
    
    private HttpServiceManager dummyHttpServiceManager() {
        return new HttpServiceManager(null, null, null, null);
    }

    public void testGetPojoAddressEndpointCxf() {
        IntentManager intentManager = new IntentManagerImpl(new IntentMap());
        PojoConfigurationTypeHandler handler = new PojoConfigurationTypeHandler(null,
                                                                                intentManager,
                                                                                dummyHttpServiceManager());
        Map<String, Object> sd = new HashMap<String, Object>();
        String url = "http://somewhere:29/boo";
        sd.put("org.apache.cxf.ws.address", url);
        assertEquals(url, handler.getServerAddress(sd, String.class));
    }

    public void testGetPojoAddressEndpointPojo() {
        IntentManager intentManager = new IntentManagerImpl(new IntentMap());
        PojoConfigurationTypeHandler handler = new PojoConfigurationTypeHandler(null, 
                                                                                intentManager,
                                                                                dummyHttpServiceManager());
        Map<String, Object> sd = new HashMap<String, Object>();
        String url = "http://somewhere:32768/foo";
        sd.put("osgi.remote.configuration.pojo.address", url);
        assertEquals(url, handler.getServerAddress(sd, String.class));
    }
    
    public void testGetDefaultPojoAddress() {
        IntentManager intentManager = new IntentManagerImpl(new IntentMap());
        PojoConfigurationTypeHandler handler = new PojoConfigurationTypeHandler(null, 
                                                                                intentManager , 
                                                                                dummyHttpServiceManager());
        Map<String, Object> sd = new HashMap<String, Object>(); 
        assertEquals("/java/lang/String", handler.getServerAddress(sd, String.class));
    }

    //  todo: add test for data bindings
    public void testCreateProxy() {
        IMocksControl c = EasyMock.createNiceControl();
        BundleContext bc1 = c.createMock(BundleContext.class);
        BundleContext bc2 = c.createMock(BundleContext.class);
        
        ServiceReference sref = c.createMock(ServiceReference.class);
        
        final ClientProxyFactoryBean cpfb = c.createMock(ClientProxyFactoryBean.class);
        ReflectionServiceFactoryBean sf = c.createMock(ReflectionServiceFactoryBean.class);
        EasyMock.expect(cpfb.getServiceFactory()).andReturn(sf).anyTimes();
        IntentManager intentManager = new IntentManagerImpl(new IntentMap()) {
            @Override
            public String[] applyIntents(List<Feature> features, 
                                         AbstractEndpointFactory factory, 
                                         Map<String, Object> sd) {
                return new String[0];
            }
        };
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(bc1,
                                                                          intentManager,
                                                                          dummyHttpServiceManager()) {
            @Override
            protected ClientProxyFactoryBean createClientProxyFactoryBean(Map<String, Object> sd, Class<?> iClass) {
                return cpfb;
            }
        };
        
        Map<String, Object> props = new HashMap<String, Object>();
        
        props.put(RemoteConstants.ENDPOINT_ID, "http://google.de/");
        props.put(org.osgi.framework.Constants.OBJECTCLASS, new String[]{"my.class"});
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[]{"my.config"});
        EndpointDescription endpoint = new EndpointDescription(props);
        
        
        cpfb.setAddress((String)EasyMock.eq(props.get(RemoteConstants.ENDPOINT_ID)));
        EasyMock.expectLastCall().atLeastOnce();
        
        cpfb.setServiceClass(EasyMock.eq(CharSequence.class));
        EasyMock.expectLastCall().atLeastOnce();
        
        c.replay();
        Object proxy = p.createProxy(sref, bc1, bc2, CharSequence.class, endpoint);
        assertNotNull(proxy);
        assertTrue("Proxy is not of the requested type! ", proxy instanceof CharSequence);
        c.verify();
    }

    public void testCreateServerWithAddressProprety() {
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(dswContext);

        String myService = "Hi";                
        final ServerFactoryBean sfb = createMockServerFactoryBean();
        
        IntentMap intentMap = new IntentMap();
        IntentManager intentManager = new IntentManagerImpl(intentMap) {
            @Override
            public String[] applyIntents(List<Feature> features, AbstractEndpointFactory factory, 
                                         Map<String, Object> sd) {
                return new String []{};
            }
        };
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, intentManager, 
                                                                          dummyHttpServiceManager()) {
            @Override
            protected ServerFactoryBean createServerFactoryBean(Map<String, Object> sd, Class<?> iClass) {
                return sfb;
            }
        };
        
        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(sr);
        EasyMock.replay(callingContext);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.WS_ADDRESS_PROPERTY, "http://alternate_host:80/myString");

        ExportResult exportResult = p.createServer(sr, dswContext, callingContext, props, String.class, myService);
        
        
        Map<String, Object> edProps = exportResult.getEndpointProps();

        assertNotNull(edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS));
        assertEquals(1, ((String[])edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS)).length);
        assertEquals(Constants.WS_CONFIG_TYPE, ((String[])edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS))[0]);
        assertEquals("http://alternate_host:80/myString", edProps.get(RemoteConstants.ENDPOINT_ID));
        
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
        
        IntentManager intentManager = new IntentManagerImpl(new IntentMap());
        PojoConfigurationTypeHandler pch = new PojoConfigurationTypeHandler(bc, 
                                                                            intentManager,
                                                                            dummyHttpServiceManager());
        
        Map<String, Object> sd = new HashMap<String, Object>();
        sd.put(org.osgi.framework.Constants.SERVICE_ID, 42);
        Map<String, Object> props = pch.createEndpointProps(sd, String.class, new String [] {"org.apache.cxf.ws"}, 
                "http://localhost:12345", new String [] {"my_intent", "your_intent"});
        
        assertFalse(props.containsKey(org.osgi.framework.Constants.SERVICE_ID));
        assertEquals(42, props.get(RemoteConstants.ENDPOINT_SERVICE_ID));
        assertEquals("some_uuid1", props.get(RemoteConstants.ENDPOINT_FRAMEWORK_UUID));
        assertEquals("http://localhost:12345", props.get(RemoteConstants.ENDPOINT_ID));
        assertEquals(Arrays.asList("java.lang.String"), 
                     Arrays.asList((Object []) props.get(org.osgi.framework.Constants.OBJECTCLASS)));
        assertEquals(Arrays.asList("org.apache.cxf.ws"),
                     Arrays.asList((Object []) props.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS)));
        assertEquals(Arrays.asList("my_intent", "your_intent"),
                     Arrays.asList((Object []) props.get(RemoteConstants.SERVICE_INTENTS)));
        assertEquals("0.0.0", props.get("endpoint.package.version.java.lang"));
    }
    
    public void testCreateJaxWsEndpointWithoutIntents() {
        IMocksControl c = EasyMock.createNiceControl();
        BundleContext dswBC = c.createMock(BundleContext.class);
        IntentManager intentManager = new DummyIntentManager();
        PojoConfigurationTypeHandler handler = new PojoConfigurationTypeHandler(dswBC, 
                                                                                intentManager, 
                                                                                dummyHttpServiceManager());
        Object serviceBean = new MyJaxWsEchoServiceImpl();
        ServiceReference sref = c.createMock(ServiceReference.class);

        Map<String, Object> sd = new HashMap<String, Object>();

        c.replay();
        ExportResult exportResult = handler.createServer(sref, dswBC, null, sd, MyJaxWsEchoService.class, serviceBean);
        c.verify();

        Server server = exportResult.getServer();
        Endpoint ep = server.getEndpoint();
        QName bindingName = ep.getEndpointInfo().getBinding().getName();
        Assert.assertEquals(JaxWsEndpointImpl.class, ep.getClass());
        Assert.assertEquals(new QName("http://jaxws.handlers.dsw.dosgi.cxf.apache.org/",
                                      "MyJaxWsEchoServiceServiceSoapBinding"),
                            bindingName);
        
    }
    
    public void testCreateSimpleEndpointWithoutIntents() {
        IMocksControl c = EasyMock.createNiceControl();
        BundleContext dswBC = c.createMock(BundleContext.class);
        IntentManager intentManager = new DummyIntentManager();
        PojoConfigurationTypeHandler handler 
            = new PojoConfigurationTypeHandler(dswBC, intentManager, dummyHttpServiceManager());
        Object serviceBean = new MySimpleEchoServiceImpl();
        ServiceReference sref = c.createMock(ServiceReference.class);
        Map<String, Object> sd = new HashMap<String, Object>();

        c.replay();
        ExportResult exportResult = handler.createServer(sref, dswBC, null, sd, MySimpleEchoService.class, serviceBean);
        Server server = exportResult.getServer();
        c.verify();
        
        Endpoint ep = server.getEndpoint();
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
}
