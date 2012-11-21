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

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentManagerImpl;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
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
        Map<String, Object> hp = new HashMap<String, Object>();
        IntentManager intentManager = new IntentManagerImpl(new IntentMap());
        PojoConfigurationTypeHandler handler = new PojoConfigurationTypeHandler(null, intentManager , hp);
        Map<String, Object> sd = new HashMap<String, Object>();
        String url = "http://somewhere:1234/blah";
        sd.put(RemoteConstants.ENDPOINT_ID, url);
        assertEquals(url, handler.getPojoAddress(sd, String.class));
    }
    
    public void testGetPojoAddressEndpointCxf() {
        Map<String, Object> hp = new HashMap<String, Object>();
        IntentManager intentManager = new IntentManagerImpl(new IntentMap());
        PojoConfigurationTypeHandler handler = new PojoConfigurationTypeHandler(null, intentManager , hp);
        Map<String, Object> sd = new HashMap<String, Object>();
        String url = "http://somewhere:29/boo";
        sd.put("org.apache.cxf.ws.address", url);
        assertEquals(url, handler.getPojoAddress(sd, String.class));
    }

    public void testGetPojoAddressEndpointPojo() {
        Map<String, Object> hp = new HashMap<String, Object>();
        IntentManager intentManager = new IntentManagerImpl(new IntentMap());
        PojoConfigurationTypeHandler handler = new PojoConfigurationTypeHandler(null, intentManager , hp);
        Map<String, Object> sd = new HashMap<String, Object>();
        String url = "http://somewhere:32768/foo";
        sd.put("osgi.remote.configuration.pojo.address", url);
        assertEquals(url, handler.getPojoAddress(sd, String.class));
    }
    
    public void testGetPojoAddressDefaultWithAlternatePort() {
        Map<String, Object> hp = new HashMap<String, Object>();
        IntentManager intentManager = new IntentManagerImpl(new IntentMap());
        PojoConfigurationTypeHandler handler = new PojoConfigurationTypeHandler(null, intentManager , hp);
        Map<String, Object> sd = new HashMap<String, Object>();
        String localIP = getLocalIp();
        String url = "http://"+localIP+":1234/java/lang/String";
        sd.put("org.apache.cxf.ws.port", "1234");
        assertEquals(url, handler.getPojoAddress(sd, String.class));        
    }

    public void testGetDefaultPojoAddress() {
        Map<String, Object> hp = new HashMap<String, Object>();
        IntentManager intentManager = new IntentManagerImpl(new IntentMap());
        PojoConfigurationTypeHandler handler = new PojoConfigurationTypeHandler(null, intentManager , hp);
        Map<String, Object> sd = new HashMap<String, Object>(); 
        String localIP = getLocalIp();
        assertEquals("http://"+localIP+":9000/java/lang/String", handler.getPojoAddress(sd, String.class));
    }

    private String getLocalIp() {
        String localIP;
        try {
            localIP = LocalHostUtil.getLocalHost().getHostAddress();
        } catch (Exception e) {
            localIP = "localhost";
        }
        return localIP;
    }

    private Map<String, Object> handlerProps;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        handlerProps = new HashMap<String, Object>();
        handlerProps.put(Constants.DEFAULT_HOST_CONFIG, "somehost");
        handlerProps.put(Constants.DEFAULT_PORT_CONFIG, "54321");
    }
    
    //  todo: add test for data bindings
    public void testCreateProxy(){
        IMocksControl c = EasyMock.createNiceControl();
        BundleContext bc1 = c.createMock(BundleContext.class);
        BundleContext bc2 = c.createMock(BundleContext.class);
        
        ServiceReference sref = c.createMock(ServiceReference.class);
        
        final ClientProxyFactoryBean cpfb = c.createMock(ClientProxyFactoryBean.class);
        ReflectionServiceFactoryBean sf = c.createMock(ReflectionServiceFactoryBean.class);
        EasyMock.expect(cpfb.getServiceFactory()).andReturn(sf).anyTimes();
        IntentManager intentManager = new IntentManagerImpl(new IntentMap()) {
            @Override
            public String[] applyIntents(List<Feature> features, AbstractEndpointFactory factory, Map<String, Object> sd) {
                return new String[0];
            }
        };
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(bc1, intentManager, handlerProps) {
            @Override
            ClientProxyFactoryBean createClientProxyFactoryBean(String frontend) {
                return cpfb;
            }
        };
        
        Map props = new HashMap();
        
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
        
        if (proxy instanceof CharSequence) {
            CharSequence cs = (CharSequence)proxy;
            
        }else{
            assertTrue("Proxy is not of the requested type! ", false);
        }
        
        
        
        c.verify();
        
    }

//    public void testCreateProxyPopulatesDistributionProvider() {
//        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
//        EasyMock.replay(bc);
//        ReflectionServiceFactoryBean sf = EasyMock.createNiceMock(ReflectionServiceFactoryBean.class);
//        EasyMock.replay(sf);
//        
//        final ClientProxyFactoryBean cpfb = EasyMock.createNiceMock(ClientProxyFactoryBean.class);
//        EasyMock.expect(cpfb.getServiceFactory()).andReturn(sf).anyTimes();
//        EasyMock.replay(cpfb);
//        
//        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(bc);
//        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(bc, handlerProps) {
//            @Override
//            ClientProxyFactoryBean createClientProxyFactoryBean(String frontend) {
//                return cpfb;
//            }
//            
//            @Override
//            String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
//                List<AbstractFeature> features, AbstractEndpointFactory factory, Map sd) {
//                return new String[0];
//            }
//        };
//        
//        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
//        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
//        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
//        ServiceEndpointDescription sd = TestUtils.mockServiceDescription("Foo");
//        EasyMock.replay(sr);
//        EasyMock.replay(dswContext);
//        EasyMock.replay(callingContext);
//        EasyMock.replay(sd);
//        
//        assertEquals("Precondition failed", 0, dp.getRemoteServices().size());
//        p.createProxy(sr, dswContext, callingContext, CharSequence.class, sd);
//        assertEquals(1, dp.getRemoteServices().size());
//        assertSame(sr, dp.getRemoteServices().iterator().next());
//    }
//    
//    public void testCreateServerPopulatesDistributionProvider() {
//        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
//        EasyMock.replay(dswContext);
//
//        String myService = "Hi";                
//        final ServerFactoryBean sfb = createMockServerFactoryBean();
//        
//        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(dswContext);
//        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
//            @Override
//            ServerFactoryBean createServerFactoryBean(String frontend) {
//                return sfb;
//            }
//
//            @Override
//            String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
//                List<AbstractFeature> features, AbstractEndpointFactory factory, ServiceEndpointDescription sd) {
//                return new String []{"A", "B"};
//            }
//        };
//        
//        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
//        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
//        ServiceEndpointDescription sd = TestUtils.mockServiceDescription("Foo");
//        EasyMock.replay(sr);
//        EasyMock.replay(callingContext);
//        EasyMock.replay(sd);
//        
//        assertEquals("Precondition failed", 0, dp.getExposedServices().size());
//        p.createServer(sr, dswContext, callingContext, sd, String.class, myService);
//        assertEquals(1, dp.getExposedServices().size());
//        assertSame(sr, dp.getExposedServices().iterator().next());
//        
//        Map<String, String> expected = new HashMap<String, String>();
//        expected.put("service.exported.configs", "org.apache.cxf.ws");
//        expected.put("org.apache.cxf.ws.address", "http://somehost:54321/java/lang/String");
//        expected.put("service.intents", "A B");
//        assertEquals(expected, dp.getExposedProperties(sr));
//    }
//    
    public void testCreateServerWithAddressProprety() {
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(dswContext);

        String myService = "Hi";                
        final ServerFactoryBean sfb = createMockServerFactoryBean();
        
        IntentMap intentMap = new IntentMap();
        IntentManager intentManager = new IntentManagerImpl(intentMap ) {
            @Override
            public String[] applyIntents(List<Feature> features, AbstractEndpointFactory factory, Map<String, Object> sd) {
                return new String []{};
            }
        };
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, intentManager, handlerProps) {
            @Override
            ServerFactoryBean createServerFactoryBean(String frontend) {
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
        
        
        Map edProps = exportResult.getEndpointProps();

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
        PojoConfigurationTypeHandler pch = new PojoConfigurationTypeHandler(bc, intentManager , null);
        
        Map<String, Object> sd = new HashMap<String, Object>();
        sd.put(org.osgi.framework.Constants.SERVICE_ID, 42);
        Map<String, Object> props = pch.createEndpointProps(sd, String.class, new String [] {"org.apache.cxf.ws"}, 
                "http://localhost:12345", new String [] {"my_intent", "your_intent"});
        
        assertFalse(props.containsKey(org.osgi.framework.Constants.SERVICE_ID));
        assertEquals(42, props.get(RemoteConstants.ENDPOINT_SERVICE_ID));
        assertEquals("some_uuid1", props.get(RemoteConstants.ENDPOINT_FRAMEWORK_UUID));
        assertEquals("http://localhost:12345", props.get(RemoteConstants.ENDPOINT_ID));
        assertEquals(Arrays.asList("java.lang.String"), Arrays.asList((Object []) props.get(org.osgi.framework.Constants.OBJECTCLASS)));
        assertEquals(Arrays.asList("org.apache.cxf.ws"), Arrays.asList((Object []) props.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS)));
        assertEquals(Arrays.asList("my_intent", "your_intent"), Arrays.asList((Object []) props.get(RemoteConstants.SERVICE_INTENTS)));
        assertEquals("0.0.0", props.get("endpoint.package.version.java.lang"));
    }
//    
//    public void testServiceExposedAdminEvent() throws Exception {
//        EventAdmin ea = EasyMock.createMock(EventAdmin.class);
//        ea.postEvent((Event) EasyMock.anyObject());
//        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
//            public Object answer() throws Throwable {
//                Event e = (Event) EasyMock.getCurrentArguments()[0];
//                assertEquals("org/osgi/service/distribution/DistributionProvider/service/exposed", e.getTopic());                
//                List<String> keys = Arrays.asList(e.getPropertyNames());
//                assertTrue(keys.contains(EventConstants.SERVICE));
//                assertNotNull(e.getProperty(EventConstants.SERVICE));
//                assertTrue(keys.contains(EventConstants.SERVICE_ID));
//                assertEquals(17L, e.getProperty(EventConstants.SERVICE_ID));
//                assertTrue(keys.contains(EventConstants.SERVICE_OBJECTCLASS));
//                assertEquals(Arrays.asList(String.class.getName()), 
//                             Arrays.asList((Object []) e.getProperty(EventConstants.SERVICE_OBJECTCLASS)));
//                return null;
//            }
//        });            
//        EasyMock.replay(ea);
//        
//        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
//        EasyMock.replay(dswContext);
//
//        String myService = "Hi";                
//        final ServerFactoryBean sfb = createMockServerFactoryBean();
//        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(dswContext);
//        dp.addEventAdmin(ea);
//        
//        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
//            @Override
//            ServerFactoryBean createServerFactoryBean(String frontend) {
//                return sfb;
//            }
//
//            @Override
//            String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
//                List<AbstractFeature> features, AbstractEndpointFactory factory, ServiceEndpointDescription sd) {
//                return new String []{};
//            }
//        };
//        
//        final Map<String, Object> props = new HashMap<String, Object>();
//        props.put(org.osgi.framework.Constants.SERVICE_ID, 17L);
//        props.put(org.osgi.framework.Constants.OBJECTCLASS, new String [] {String.class.getName()});
//        props.put("boo", "baa");
//        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
//        EasyMock.expect(sr.getPropertyKeys()).andReturn(props.keySet().toArray(new String [] {})).anyTimes();
//        EasyMock.expect(sr.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {
//            public Object answer() throws Throwable {
//                return props.get(EasyMock.getCurrentArguments()[0]);
//            }            
//        }).anyTimes();
//        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
//        ServiceEndpointDescription sd = TestUtils.mockServiceDescription("Foo");
//        EasyMock.replay(sr);
//        EasyMock.replay(callingContext);
//        EasyMock.replay(sd);
//
//        p.createServer(sr, dswContext, callingContext, sd, String.class, myService);
//        EasyMock.verify(ea);
//    }
//
//    public void testServiceUnsatisfiedAdminEvent() throws Exception {
//        EventAdmin ea = EasyMock.createMock(EventAdmin.class);
//        ea.postEvent((Event) EasyMock.anyObject());
//        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
//            public Object answer() throws Throwable {
//                Event e = (Event) EasyMock.getCurrentArguments()[0];
//                assertEquals("org/osgi/service/distribution/DistributionProvider/service/unsatisfied", e.getTopic());                
//                return null;
//            }
//        });            
//        EasyMock.replay(ea);
//        
//        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
//        EasyMock.replay(dswContext);
//
//        String myService = "Hi";                
//        final ServerFactoryBean sfb = createMockServerFactoryBean();
//        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(dswContext);
//        dp.addEventAdmin(ea);
//        
//        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
//            @Override
//            ServerFactoryBean createServerFactoryBean(String frontend) {
//                return sfb;
//            }
//
//            @Override
//            String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
//                List<AbstractFeature> features, AbstractEndpointFactory factory, ServiceEndpointDescription sd) {
//                throw new IntentUnsatifiedException("XYZ");
//            }
//        };
//        
//        final Map<String, Object> props = new HashMap<String, Object>();
//        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
//        EasyMock.expect(sr.getPropertyKeys()).andReturn(props.keySet().toArray(new String [] {})).anyTimes();
//        EasyMock.expect(sr.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {
//            public Object answer() throws Throwable {
//                return props.get(EasyMock.getCurrentArguments()[0]);
//            }            
//        }).anyTimes();
//        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
//        ServiceEndpointDescription sd = TestUtils.mockServiceDescription("Foo");
//        EasyMock.replay(sr);
//        EasyMock.replay(callingContext);
//        EasyMock.replay(sd);
//
//        try {
//            p.createServer(sr, dswContext, callingContext, sd, String.class, myService);
//            fail("Should have thrown an IntentUnsatifiedException");
//        } catch (IntentUnsatifiedException iue) {
//            // good
//        }
//        EasyMock.verify(ea);
//    }
    
}
