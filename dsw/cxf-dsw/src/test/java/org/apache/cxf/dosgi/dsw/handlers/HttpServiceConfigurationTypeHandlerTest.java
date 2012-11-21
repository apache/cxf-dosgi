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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentManagerImpl;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.dosgi.dsw.service.RemoteServiceAdminCore;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
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
import org.osgi.service.http.HttpService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class HttpServiceConfigurationTypeHandlerTest extends TestCase {
    
    public void testServer() throws Exception {
        IMocksControl c = EasyMock.createNiceControl();
        BundleContext dswContext = c.createMock(BundleContext.class);
        EasyMock.expect(dswContext.getProperty("org.osgi.service.http.port")).
            andReturn("1327").anyTimes();
        HttpService httpService = c.createMock(HttpService.class);
        // expect that the cxf servlet is registered
        
        ServiceReference httpSvcSR = c.createMock(ServiceReference.class);
        EasyMock.expect(dswContext.getService(httpSvcSR)).andReturn(httpService).anyTimes();
        
        final ServerFactoryBean sfb = createMockServerFactoryBean();
        
        //        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(dswContext);
        Map<String, Object> handlerProps = new HashMap<String, Object>();
        IntentManager intentManager = new DummyIntentManager(new String [] {"a.b.c"});
        c.replay();
        
        HttpServiceConfigurationTypeHandler h = 
            new HttpServiceConfigurationTypeHandler(dswContext, intentManager , handlerProps) {
                @Override
                ServerFactoryBean createServerFactoryBean(String frontend) {
                    return sfb;
                }
        };
        h.httpServiceReferences.add(httpSvcSR);
        
        Runnable myService = new Runnable() {
            public void run() {
                System.out.println("blah");
            }            
        };
                
        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(sr);
        EasyMock.replay(callingContext);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.WS_HTTP_SERVICE_CONTEXT, "/myRunnable");
        //ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Runnable.class.getName(), props);
        
        //        assertEquals("Precondition failed", 0, dp.getExposedServices().size());
        assertEquals("Precondition failed", "", sfb.getAddress());
        ExportResult exportResult = h.createServer(sr, dswContext, callingContext, props, Runnable.class, myService);
        assertEquals("The address should be set to '/'. The Servlet context dictates the actual location.", "/", sfb.getAddress());
        //assertEquals(1, dp.getExposedServices().size());
        //assertSame(sr, dp.getExposedServices().iterator().next());
        
        String hostName;
        try {
            hostName = LocalHostUtil.getLocalHost().getHostAddress();
        } catch (Exception e) {
            hostName = "localhost";
        }
        
        Map edProps = exportResult.getEndpointProps();
        
        assertNotNull(edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS));
        assertEquals(1, ((String[])edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS)).length);
        assertEquals(Constants.WS_CONFIG_TYPE, ((String[])edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS))[0]);
        assertEquals("http://" + hostName + ":1327/myRunnable", edProps.get(RemoteConstants.ENDPOINT_ID));
        assertNotNull(edProps.get(RemoteConstants.SERVICE_INTENTS));
        assertTrue(((String[])edProps.get(RemoteConstants.SERVICE_INTENTS)).length>=1);
        boolean intentIn = false;
        for (String s : (String[])edProps.get(RemoteConstants.SERVICE_INTENTS)) {
            if("a.b.c".equals(s)){
                intentIn=true;
                break;
            }
        }
        assertTrue(intentIn);
    } 
    
    public void testServerUsingDefaultAddress() throws Exception {
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        HttpService httpService = EasyMock.createNiceMock(HttpService.class);
        // expect that the cxf servlet is registered
        EasyMock.replay(httpService);
        
        ServiceReference httpSvcSR = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.replay(httpSvcSR);
        EasyMock.expect(dswContext.getService(httpSvcSR)).andReturn(httpService).anyTimes();
        EasyMock.replay(dswContext);
        
        final ServerFactoryBean sfb = createMockServerFactoryBean();
        
        IntentManager intentManager = new DummyIntentManager(new String[]{});
        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(dswContext, intentManager);
        Map<String, Object> handlerProps = new HashMap<String, Object>();
        
        HttpServiceConfigurationTypeHandler h = 
            new HttpServiceConfigurationTypeHandler(dswContext, intentManager, handlerProps) {
                @Override
                ServerFactoryBean createServerFactoryBean(String frontend) {
                    return sfb;
                }

                            
        };
        h.httpServiceReferences.add(httpSvcSR);
        
        Runnable myService = new Runnable() {
            public void run() {
                System.out.println("blah");
            }            
        };
                
        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(sr);
        EasyMock.replay(callingContext);

        Map<String, Object> props = new HashMap<String, Object>();
        //ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Runnable.class.getName(), props);
        
        //assertEquals("Precondition failed", 0, dp.getExposedServices().size());
        ExportResult exportResult = h.createServer(sr, dswContext, callingContext, props, Runnable.class, myService);
        //assertEquals(1, dp.getExposedServices().size());
        //assertSame(sr, dp.getExposedServices().iterator().next());
        
        String hostname;
        try {
            hostname = LocalHostUtil.getLocalHost().getHostAddress();
        } catch (Exception e) {
            hostname = "localhost";
        }
        
        Map edProps = exportResult.getEndpointProps();
        
        assertNotNull(edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS));
        assertEquals(1, ((String[])edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS)).length);
        assertEquals(Constants.WS_CONFIG_TYPE, ((String[])edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS))[0]);
        assertEquals("http://" + hostname + ":8080/java/lang/Runnable", edProps.get(RemoteConstants.ENDPOINT_ID));
        
    }

    public void testServerConfiguredUsingHttps() throws Exception {
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(dswContext.getProperty("org.osgi.service.http.secure.enabled")).
            andReturn("true").anyTimes();
        EasyMock.expect(dswContext.getProperty("org.osgi.service.http.port.secure")).
            andReturn("8432").anyTimes();
        
        HttpService httpService = EasyMock.createNiceMock(HttpService.class);
        // expect that the cxf servlet is registered
        EasyMock.replay(httpService);
        
        ServiceReference httpSvcSR = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.replay(httpSvcSR);
        EasyMock.expect(dswContext.getService(httpSvcSR)).andReturn(httpService).anyTimes();
        EasyMock.replay(dswContext);
        
        final ServerFactoryBean sfb = createMockServerFactoryBean();
        
		IntentManager intentManager = new DummyIntentManager(new String [] {});
        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(dswContext, intentManager);
        Map<String, Object> handlerProps = new HashMap<String, Object>();
        HttpServiceConfigurationTypeHandler h = 
            new HttpServiceConfigurationTypeHandler(dswContext, intentManager, handlerProps) {
                @Override
                ServerFactoryBean createServerFactoryBean(String frontend) {
                    return sfb;
                }
        };
        h.httpServiceReferences.add(httpSvcSR);
        
        Runnable myService = new Runnable() {
            public void run() {
                System.out.println("blah");
            }            
        };
                
        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(sr);
        EasyMock.replay(callingContext);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.configuration.type", Constants.WS_CONFIG_TYPE);
        props.put(Constants.WS_HTTP_SERVICE_CONTEXT, "/myRunnable");
        //ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Runnable.class.getName(), props);
        
        //assertEquals("Precondition failed", 0, dp.getExposedServices().size());
        ExportResult exportResult = h.createServer(sr, dswContext, callingContext, props, Runnable.class, myService);
        //assertEquals(1, dp.getExposedServices().size());
        //assertSame(sr, dp.getExposedServices().iterator().next());
        
        String hostName;
        try {
            hostName = LocalHostUtil.getLocalHost().getHostAddress();
        } catch (Exception e) {
            hostName = "localhost";
        }
        Map edProps = exportResult.getEndpointProps();
        
        assertNotNull(edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS));
        assertEquals(1, ((String[])edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS)).length);
        assertEquals(Constants.WS_CONFIG_TYPE, ((String[])edProps.get(RemoteConstants.SERVICE_IMPORTED_CONFIGS))[0]);
        assertEquals("https://" + hostName + ":8432/myRunnable", edProps.get(RemoteConstants.ENDPOINT_ID));
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
        }).anyTimes();
        EasyMock.expect(sfb.getAddress()).andAnswer(new IAnswer<String>() {
            public String answer() throws Throwable {
                return serverURI.toString();
            }            
        }).anyTimes();
        EasyMock.replay(sfb);
        return sfb;
    }

    private Server createMockServer(final ServerFactoryBean sfb) {
        AttributedURIType addr = org.easymock.classextension.EasyMock.createMock(AttributedURIType.class);
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
    
    public void testCreateProxy() {
        IMocksControl c = EasyMock.createNiceControl();
        BundleContext bc1 = c.createMock(BundleContext.class);
        BundleContext bc2 = c.createMock(BundleContext.class);

        HttpService httpService = c.createMock(HttpService.class);

        ServiceReference httpSvcSR = c.createMock(ServiceReference.class);
        EasyMock.expect(bc1.getService(httpSvcSR)).andReturn(httpService).anyTimes();

        final ClientProxyFactoryBean cpfb = c.createMock(ClientProxyFactoryBean.class);
        ReflectionServiceFactoryBean sf = c.createMock(ReflectionServiceFactoryBean.class);
        EasyMock.expect(cpfb.getServiceFactory()).andReturn(sf).anyTimes();

        ServiceReference sr = c.createMock(ServiceReference.class);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.ENDPOINT_ID, "000001");
        props.put(Constants.WS_ADDRESS_PROPERTY, "http://google.de/");
        props.put(org.osgi.framework.Constants.OBJECTCLASS, new String[] { "my.class" });
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[] { "my.config" });
        EndpointDescription endpoint = new EndpointDescription(props);

        cpfb.setAddress((String) EasyMock.eq(props.get(Constants.WS_ADDRESS_PROPERTY)));
        EasyMock.expectLastCall().atLeastOnce();

        cpfb.setServiceClass(EasyMock.eq(CharSequence.class));
        EasyMock.expectLastCall().atLeastOnce();

        c.replay();

        Map<String, Object> handlerProps = new HashMap<String, Object>();
        IntentManager intentManager = new DummyIntentManager(new String[] { "a.b.c" });
        HttpServiceConfigurationTypeHandler h = new HttpServiceConfigurationTypeHandler(bc1, intentManager , handlerProps) {
            @Override
            ClientProxyFactoryBean createClientProxyFactoryBean(String frontend) {
                return cpfb;
            }

        };

        h.httpServiceReferences.add(httpSvcSR);

        Object proxy = h.createProxy(sr, bc1, bc2, CharSequence.class, endpoint);
        assertNotNull(proxy);

        if (proxy instanceof CharSequence) {
            CharSequence cs = (CharSequence) proxy;
        } else {
            assertTrue("Proxy is not of the requested type! ", false);
        }

        c.verify();
    }

    // TODO these tests still need to be written...
    /*
    public void testNoHttpService() {
        
    }
    
    public void testHttpServiceDynamism() {
        
    }
    
    public void testServletUnregistration() {        
    }
    */
    
    class DummyIntentManager implements IntentManager {
        private String[] applyResult;

        public DummyIntentManager(String[] applyResult) {
            this.applyResult = applyResult;
        }
        
        public List<String> getUnsupportedIntents(Properties serviceProperties) {
            return new ArrayList<String>();
        }

        public BindingConfiguration getBindingConfiguration(String[] requestedIntents,
                BindingConfiguration defaultConfig) {
            return defaultConfig;
        }

        public String[] applyIntents(List<Feature> features, String[] requestedIntents)
                throws IntentUnsatifiedException {
            return applyResult;
        }

        public String[] applyIntents(List<Feature> features, AbstractEndpointFactory factory, Map<String, Object> props) {
            return applyResult;
        }
    };
}
