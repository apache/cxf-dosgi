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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.TestUtils;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.dosgi.dsw.service.DistributionProviderImpl;
import org.apache.cxf.dosgi.dsw.service.ServiceEndpointDescriptionImpl;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
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
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;


public class PojoConfigurationTypeHandlerTest extends TestCase {
    private Map<String, Object> handlerProps;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        handlerProps = new HashMap<String, Object>();
        handlerProps.put(Constants.DEFAULT_HOST_CONFIG, "somehost");
        handlerProps.put(Constants.DEFAULT_PORT_CONFIG, "54321");
    }

    public void testCreateProxyPopulatesDistributionProvider() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        ReflectionServiceFactoryBean sf = EasyMock.createNiceMock(ReflectionServiceFactoryBean.class);
        EasyMock.replay(sf);
        
        final ClientProxyFactoryBean cpfb = EasyMock.createNiceMock(ClientProxyFactoryBean.class);
        EasyMock.expect(cpfb.getServiceFactory()).andReturn(sf).anyTimes();
        EasyMock.replay(cpfb);
        
        DistributionProviderImpl dp = new DistributionProviderImpl(bc);
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(bc, dp, handlerProps) {
            @Override
            ClientProxyFactoryBean createClientProxyFactoryBean(String frontend) {
                return cpfb;
            }
            
            @Override
            String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
                List<AbstractFeature> features, AbstractEndpointFactory factory, ServiceEndpointDescription sd) {
                return new String[0];
            }
        };
        
        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
        ServiceEndpointDescription sd = TestUtils.mockServiceDescription("Foo");
        EasyMock.replay(sr);
        EasyMock.replay(dswContext);
        EasyMock.replay(callingContext);
        EasyMock.replay(sd);
        
        assertEquals("Precondition failed", 0, dp.getRemoteServices().size());
        p.createProxy(sr, dswContext, callingContext, CharSequence.class, sd);
        assertEquals(1, dp.getRemoteServices().size());
        assertSame(sr, dp.getRemoteServices().iterator().next());
    }
    
    public void testCreateServerPopulatesDistributionProvider() {
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(dswContext);

        String myService = "Hi";                
        final ServerFactoryBean sfb = createMockServerFactoryBean();
        
        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            ServerFactoryBean createServerFactoryBean(String frontend) {
                return sfb;
            }

            @Override
            String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
                List<AbstractFeature> features, AbstractEndpointFactory factory, ServiceEndpointDescription sd) {
                return new String []{"A", "B"};
            }
        };
        
        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
        ServiceEndpointDescription sd = TestUtils.mockServiceDescription("Foo");
        EasyMock.replay(sr);
        EasyMock.replay(callingContext);
        EasyMock.replay(sd);
        
        assertEquals("Precondition failed", 0, dp.getExposedServices().size());
        p.createServer(sr, dswContext, callingContext, sd, String.class, myService);
        assertEquals(1, dp.getExposedServices().size());
        assertSame(sr, dp.getExposedServices().iterator().next());
        
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("service.exported.configs", "org.apache.cxf.ws");
        expected.put("org.apache.cxf.ws.address", "http://somehost:54321/java/lang/String");
        expected.put("service.intents", "A B");
        assertEquals(expected, dp.getExposedProperties(sr));
    }
    
    public void testCreateServerWithAddressProprety() {
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(dswContext);

        String myService = "Hi";                
        final ServerFactoryBean sfb = createMockServerFactoryBean();
        
        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            ServerFactoryBean createServerFactoryBean(String frontend) {
                return sfb;
            }

            @Override
            String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
                List<AbstractFeature> features, AbstractEndpointFactory factory, ServiceEndpointDescription sd) {
                return new String []{};
            }
        };
        
        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(sr);
        EasyMock.replay(callingContext);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.WS_ADDRESS_PROPERTY, "http://alternate_host:80/myString");
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Runnable.class.getName(), props);
        
        assertEquals("Precondition failed", 0, dp.getExposedServices().size());
        p.createServer(sr, dswContext, callingContext, sd, String.class, myService);
        assertEquals(1, dp.getExposedServices().size());
        assertSame(sr, dp.getExposedServices().iterator().next());
        
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("service.exported.configs", "org.apache.cxf.ws");
        expected.put("org.apache.cxf.ws.address", "http://alternate_host:80/myString");
        assertEquals(expected, dp.getExposedProperties(sr));
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
    
    public void testIntents() throws Exception {
        Map<String, Object> intents = new HashMap<String, Object>();
        intents.put("A", new AbstractFeature() {});
        intents.put("SOAP", new AbstractFeature() {});
        final IntentMap intentMap = new IntentMap();
        intentMap.setIntents(intents);
        
        IMocksControl control = EasyMock.createNiceControl();
        BundleContext dswContext = control.createMock(BundleContext.class);
        BundleContext callingContext = control.createMock(BundleContext.class);        
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();

        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        AbstractPojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            IntentMap getIntentMap(BundleContext callingContext) {
                return intentMap;
            }

            @Override
            String getDefaultBindingIntent() {
                return null;
            }        
        };
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "A");
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);

        List<String> effectiveIntents = 
            Arrays.asList(p.applyIntents(dswContext, callingContext, features, factory, sd));
        assertEquals(Arrays.asList("A"), effectiveIntents);
    }    
    
    public void testMultiIntents() {
        Map<String, Object> intents = new HashMap<String, Object>();
        intents.put("confidentiality.message", new AbstractFeature() {});
        intents.put("transactionality", new AbstractFeature() {});
        final IntentMap intentMap = new IntentMap();
        intentMap.setIntents(intents);
        
        IMocksControl control = EasyMock.createNiceControl();
        BundleContext dswContext = control.createMock(BundleContext.class);
        BundleContext callingContext = control.createMock(BundleContext.class);        
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();

        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            IntentMap getIntentMap(BundleContext callingContext) {
                return intentMap;
            }            

            @Override
            String getDefaultBindingIntent() {
                return null;
            }        
        };
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "transactionality confidentiality.message");
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);

        List<String> effectiveIntents = 
            Arrays.asList(p.applyIntents(dswContext, callingContext, features, factory, sd));
        assertTrue(effectiveIntents.contains("transactionality"));        
        assertTrue(effectiveIntents.contains("confidentiality.message"));        
    }
    
    public void testFailedIntent() {
        Map<String, Object> intents = new HashMap<String, Object>();
        intents.put("A", new AbstractFeature() {});
        final IntentMap intentMap = new IntentMap();
        intentMap.setIntents(intents);
                
        IMocksControl control = EasyMock.createNiceControl();
        BundleContext dswContext = control.createMock(BundleContext.class);
        BundleContext callingContext = control.createMock(BundleContext.class);        
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();

        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            IntentMap getIntentMap(BundleContext callingContext) {
                return intentMap;
            }            
        };

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "A B");
        ServiceEndpointDescription sd = 
            new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);

        try {
            p.applyIntents(dswContext, callingContext, features, factory, sd);
            fail("applyIntents() should have thrown an exception as there was an unsatisfiable intent");
        } catch (IntentUnsatifiedException iue) {
            assertEquals("B", iue.getIntent());
        }
    }
 
    public void testInferIntents() {
        Map<String, Object> intents = new HashMap<String, Object>();
        intents.put("Prov", "PROVIDED");
        AbstractFeature feat1 = new AbstractFeature() {};
        intents.put("A", feat1);
        intents.put("A_alt", feat1);
        intents.put("B", new AbstractFeature() {});
        final IntentMap intentMap = new IntentMap();
        intentMap.setIntents(intents);
        
        IMocksControl control = EasyMock.createNiceControl();
        BundleContext dswContext = control.createMock(BundleContext.class);
        BundleContext callingContext = control.createMock(BundleContext.class);
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();
        
        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            IntentMap getIntentMap(BundleContext callingContext) {
                return intentMap;
            }            

            @Override
            String getDefaultBindingIntent() {
                return null;
            }        
        };
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "A");
        ServiceEndpointDescription sd = 
            new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);
        
        List<String> effectiveIntents = 
            Arrays.asList(p.applyIntents(dswContext, callingContext, features, factory, sd));
        assertEquals(3, effectiveIntents.size());
        assertTrue(effectiveIntents.contains("Prov"));
        assertTrue(effectiveIntents.contains("A"));
        assertTrue(effectiveIntents.contains("A_alt"));
    }
    
    public void testDefaultBindingIntent() {        
        IMocksControl control = EasyMock.createNiceControl();

        Map<String, Object> intents = new HashMap<String, Object>();
        BindingConfiguration feat1 = control.createMock(BindingConfiguration.class);
        intents.put("A", new AbstractFeature() {});
        intents.put("SOAP", feat1);
        intents.put("SOAP.1_1", feat1);
        intents.put("SOAP.1_2", control.createMock(BindingConfiguration.class));
        final IntentMap intentMap = new IntentMap();
        intentMap.setIntents(intents);

        BundleContext dswContext = control.createMock(BundleContext.class);
        BundleContext callingContext = control.createMock(BundleContext.class);
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();
        
        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            IntentMap getIntentMap(BundleContext callingContext) {
                return intentMap;
            }            
        };

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "A");
        ServiceEndpointDescription sd = 
            new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);
        
        List<String> effectiveIntents = 
            Arrays.asList(p.applyIntents(dswContext, callingContext, features, factory, sd));
        assertEquals(3, effectiveIntents.size());
        assertTrue(effectiveIntents.contains("A"));
        assertTrue(effectiveIntents.contains("SOAP"));
        assertTrue(effectiveIntents.contains("SOAP.1_1"));
    }
    
    public void testExplicitBindingIntent() {
        IMocksControl control = EasyMock.createNiceControl();

        Map<String, Object> intents = new HashMap<String, Object>();
        BindingConfiguration feat1 = control.createMock(BindingConfiguration.class);
        intents.put("A", new AbstractFeature() {});
        intents.put("SOAP", feat1);
        intents.put("SOAP.1_1", feat1);
        intents.put("SOAP.1_2", control.createMock(BindingConfiguration.class));
        final IntentMap intentMap = new IntentMap();
        intentMap.setIntents(intents);

        BundleContext dswContext = control.createMock(BundleContext.class);
        BundleContext callingContext = control.createMock(BundleContext.class);
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();
        
        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            IntentMap getIntentMap(BundleContext callingContext) {
                return intentMap;
            }            
        };

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "A SOAP.1_2");
        ServiceEndpointDescription sd = 
            new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);
        
        List<String> effectiveIntents = 
            Arrays.asList(p.applyIntents(dswContext, callingContext, features, factory, sd));
        assertEquals(2, effectiveIntents.size());
        assertTrue(effectiveIntents.contains("A"));
        assertTrue(effectiveIntents.contains("SOAP.1_2"));        
    }
    
    public void testInheritMasterIntentMapDefault() {
        List<String> features = runTestInheritMasterIntentMap("A B");
        
        assertEquals(2, features.size());
        assertTrue(features.contains("appFeatureA"));
        assertTrue(features.contains("masterFeatureB"));
    }
    
    public void testInheritMasterIntentMap() {
        handlerProps.put(Constants.USE_MASTER_MAP, "true");
        List<String> features = runTestInheritMasterIntentMap("A B");
        
        assertEquals(2, features.size());
        assertTrue(features.contains("appFeatureA"));
        assertTrue(features.contains("masterFeatureB"));
    }

    public void testDontInheritMasterIntentMapFails() {
        handlerProps.put(Constants.USE_MASTER_MAP, "false");
        try {
            runTestInheritMasterIntentMap("A B");
            fail("Should have failed as intent B was not satisfied");
        } catch (IntentUnsatifiedException iue) {
            assertEquals("B", iue.getIntent());
        }    
    }

    public void testDontInheritMasterIntentMapSucceeds() {
        handlerProps.put(Constants.USE_MASTER_MAP, "false");
        List<String> features = runTestInheritMasterIntentMap("A");
        
        assertEquals(1, features.size());
        assertTrue(features.contains("appFeatureA"));
    }

    private List<String> runTestInheritMasterIntentMap(String requestedIntents) {
        Map<String, Object> masterIntents = new HashMap<String, Object>();
        masterIntents.put("A", new TestFeature("masterFeatureA"));
        masterIntents.put("B", new TestFeature("masterFeatureB"));
        final IntentMap masterIntentMap = new IntentMap();
        masterIntentMap.setIntents(masterIntents);
        
        Map<String, Object> appIntents = new HashMap<String, Object>();
        appIntents.put("A", new TestFeature("appFeatureA"));
        final IntentMap appIntentMap = new IntentMap();
        appIntentMap.setIntents(appIntents);

        IMocksControl control = EasyMock.createNiceControl();
        final BundleContext dswContext = control.createMock(BundleContext.class);
        final BundleContext callingContext = control.createMock(BundleContext.class);        
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();

        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", requestedIntents);
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);
        
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            IntentMap getIntentMap(BundleContext ctx) {
                if (ctx == callingContext) {
                    return appIntentMap;
                } else if (ctx == dswContext) {
                    return masterIntentMap;
                } else {
                    return null;
                }
            }            

            @Override
            String getDefaultBindingIntent() {
                return null;
            }        
        };
        
        p.applyIntents(dswContext, callingContext, features, factory, sd);

        List<String> featureNames = new ArrayList<String>();
        for (AbstractFeature f : features) {
            featureNames.add(f.toString());
        }
        return featureNames;
    }
    
    public void testProvidedIntents() {
        Map<String, Object> masterIntents = new HashMap<String, Object>();
        masterIntents.put("A", "Provided");
        masterIntents.put("B", "PROVIDED");
        final IntentMap masterIntentMap = new IntentMap();
        masterIntentMap.setIntents(masterIntents);        
        final IntentMap appIntentMap = new IntentMap();
        appIntentMap.setIntents(new HashMap<String, Object>());

        IMocksControl control = EasyMock.createNiceControl();
        final BundleContext dswContext = control.createMock(BundleContext.class);
        final BundleContext callingContext = control.createMock(BundleContext.class);        
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();
        
        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "B A");
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);
        
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            IntentMap getIntentMap(BundleContext ctx) {
                if (ctx == callingContext) {
                    return appIntentMap;
                } else if (ctx == dswContext) {
                    return masterIntentMap;
                } else {
                    return null;
                }
            }                      

            @Override
            String getDefaultBindingIntent() {
                return null;
            }        
        };
        
        Set<String> effectiveIntents = new HashSet<String>(Arrays.asList( 
            p.applyIntents(dswContext, callingContext, features, factory, sd)));
        Set<String> expectedIntents = new HashSet<String>(Arrays.asList(new String [] {"A", "B"}));
        assertEquals(expectedIntents, effectiveIntents);
    }
    
    public void testServiceExposedAdminEvent() throws Exception {
        EventAdmin ea = EasyMock.createMock(EventAdmin.class);
        ea.postEvent((Event) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                Event e = (Event) EasyMock.getCurrentArguments()[0];
                assertEquals("org/osgi/service/distribution/DistributionProvider/service/exposed", e.getTopic());                
                List<String> keys = Arrays.asList(e.getPropertyNames());
                assertTrue(keys.contains(EventConstants.SERVICE));
                assertNotNull(e.getProperty(EventConstants.SERVICE));
                assertTrue(keys.contains(EventConstants.SERVICE_ID));
                assertEquals(17L, e.getProperty(EventConstants.SERVICE_ID));
                assertTrue(keys.contains(EventConstants.SERVICE_OBJECTCLASS));
                assertEquals(Arrays.asList(String.class.getName()), 
                             Arrays.asList((Object []) e.getProperty(EventConstants.SERVICE_OBJECTCLASS)));
                return null;
            }
        });            
        EasyMock.replay(ea);
        
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(dswContext);

        String myService = "Hi";                
        final ServerFactoryBean sfb = createMockServerFactoryBean();
        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        dp.addEventAdmin(ea);
        
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            ServerFactoryBean createServerFactoryBean(String frontend) {
                return sfb;
            }

            @Override
            String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
                List<AbstractFeature> features, AbstractEndpointFactory factory, ServiceEndpointDescription sd) {
                return new String []{};
            }
        };
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(org.osgi.framework.Constants.SERVICE_ID, 17L);
        props.put(org.osgi.framework.Constants.OBJECTCLASS, new String [] {String.class.getName()});
        props.put("boo", "baa");
        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.expect(sr.getPropertyKeys()).andReturn(props.keySet().toArray(new String [] {})).anyTimes();
        EasyMock.expect(sr.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                return props.get(EasyMock.getCurrentArguments()[0]);
            }            
        }).anyTimes();
        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
        ServiceEndpointDescription sd = TestUtils.mockServiceDescription("Foo");
        EasyMock.replay(sr);
        EasyMock.replay(callingContext);
        EasyMock.replay(sd);

        p.createServer(sr, dswContext, callingContext, sd, String.class, myService);
        EasyMock.verify(ea);
    }

    public void testServiceUnsatisfiedAdminEvent() throws Exception {
        EventAdmin ea = EasyMock.createMock(EventAdmin.class);
        ea.postEvent((Event) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                Event e = (Event) EasyMock.getCurrentArguments()[0];
                assertEquals("org/osgi/service/distribution/DistributionProvider/service/unsatisfied", e.getTopic());                
                return null;
            }
        });            
        EasyMock.replay(ea);
        
        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(dswContext);

        String myService = "Hi";                
        final ServerFactoryBean sfb = createMockServerFactoryBean();
        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        dp.addEventAdmin(ea);
        
        PojoConfigurationTypeHandler p = new PojoConfigurationTypeHandler(dswContext, dp, handlerProps) {
            @Override
            ServerFactoryBean createServerFactoryBean(String frontend) {
                return sfb;
            }

            @Override
            String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
                List<AbstractFeature> features, AbstractEndpointFactory factory, ServiceEndpointDescription sd) {
                throw new IntentUnsatifiedException("XYZ");
            }
        };
        
        final Map<String, Object> props = new HashMap<String, Object>();
        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.expect(sr.getPropertyKeys()).andReturn(props.keySet().toArray(new String [] {})).anyTimes();
        EasyMock.expect(sr.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                return props.get(EasyMock.getCurrentArguments()[0]);
            }            
        }).anyTimes();
        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
        ServiceEndpointDescription sd = TestUtils.mockServiceDescription("Foo");
        EasyMock.replay(sr);
        EasyMock.replay(callingContext);
        EasyMock.replay(sd);

        try {
            p.createServer(sr, dswContext, callingContext, sd, String.class, myService);
            fail("Should have thrown an IntentUnsatifiedException");
        } catch (IntentUnsatifiedException iue) {
            // good
        }
        EasyMock.verify(ea);
    }

    private static class TestFeature extends AbstractFeature {
        private final String name;
        
        private TestFeature(String n) {
            name = n;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    
}
