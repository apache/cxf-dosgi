package org.apache.cxf.dosgi.dsw.handlers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.service.DistributionProviderImpl;
import org.apache.cxf.dosgi.dsw.service.ServiceEndpointDescriptionImpl;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.http.HttpService;

public class HttpServiceConfigurationTypeHandlerTest extends TestCase {
    public void testServer() throws Exception {
        Properties savedProps = new Properties();
        savedProps.putAll(System.getProperties());
        try {
            System.setProperty("org.osgi.service.http.port", "327");
            
            BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
            HttpService httpService = EasyMock.createNiceMock(HttpService.class);
            // expect that the cxf servlet is registered
            EasyMock.replay(httpService);
            
            ServiceReference httpSvcSR = EasyMock.createNiceMock(ServiceReference.class);
            EasyMock.replay(httpSvcSR);
            EasyMock.expect(dswContext.getService(httpSvcSR)).andReturn(httpService).anyTimes();
            EasyMock.replay(dswContext);
            
            final ServerFactoryBean sfb = createMockServerFactoryBean();
            
            DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
            Map<String, Object> handlerProps = new HashMap<String, Object>();
            HttpServiceConfigurationTypeHandler h = 
                new HttpServiceConfigurationTypeHandler(dswContext, dp, handlerProps) {
                    @Override
                    ServerFactoryBean createServerFactoryBean() {
                        return sfb;
                    }
    
                    @Override
                    String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
                            List<AbstractFeature> features, AbstractEndpointFactory factory, 
                            ServiceEndpointDescription sd) {
                        return new String [] {"a.b.c"};
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
            props.put(Constants.POJO_HTTP_SERVICE_CONTEXT, "/myRunnable");
            ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Runnable.class.getName(), props);
            
            assertEquals("Precondition failed", 0, dp.getExposedServices().size());
            h.createServer(sr, dswContext, callingContext, sd, Runnable.class, myService);
            assertEquals(1, dp.getExposedServices().size());
            assertSame(sr, dp.getExposedServices().iterator().next());
            
            String hostName = InetAddress.getLocalHost().getHostName();
            Map<String, String> expected = new HashMap<String, String>();
            expected.put("osgi.remote.configuration.type", "pojo");
            expected.put("osgi.remote.configuration.pojo.address", "http://" + hostName + ":327/myRunnable");
            expected.put("osgi.intents", "a.b.c");
            assertEquals(expected, dp.getExposedProperties(sr));
        } finally {
            System.setProperties(savedProps);
        }
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
        
        DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
        Map<String, Object> handlerProps = new HashMap<String, Object>();
        HttpServiceConfigurationTypeHandler h = 
            new HttpServiceConfigurationTypeHandler(dswContext, dp, handlerProps) {
                @Override
                ServerFactoryBean createServerFactoryBean() {
                    return sfb;
                }

                @Override
                String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
                        List<AbstractFeature> features, AbstractEndpointFactory factory, 
                        ServiceEndpointDescription sd) {
                    return new String [] {};
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
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Runnable.class.getName(), props);
        
        assertEquals("Precondition failed", 0, dp.getExposedServices().size());
        h.createServer(sr, dswContext, callingContext, sd, Runnable.class, myService);
        assertEquals(1, dp.getExposedServices().size());
        assertSame(sr, dp.getExposedServices().iterator().next());
        
        String hostname = InetAddress.getLocalHost().getHostName();
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("osgi.remote.configuration.type", "pojo");
        expected.put("osgi.remote.configuration.pojo.address", "http://" + hostname + ":80/java/lang/Runnable");
        assertEquals(expected, dp.getExposedProperties(sr));
    }

    public void testServerConfiguredUsingHttps() throws Exception {
        Properties savedProps = new Properties();
        savedProps.putAll(System.getProperties());
        try {
            System.setProperty("org.osgi.service.http.secure.enabled", "true");
            System.setProperty("org.osgi.service.http.port.secure", "8443");
            
            BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
            HttpService httpService = EasyMock.createNiceMock(HttpService.class);
            // expect that the cxf servlet is registered
            EasyMock.replay(httpService);
            
            ServiceReference httpSvcSR = EasyMock.createNiceMock(ServiceReference.class);
            EasyMock.replay(httpSvcSR);
            EasyMock.expect(dswContext.getService(httpSvcSR)).andReturn(httpService).anyTimes();
            EasyMock.replay(dswContext);
            
            final ServerFactoryBean sfb = createMockServerFactoryBean();
            
            DistributionProviderImpl dp = new DistributionProviderImpl(dswContext);
            Map<String, Object> handlerProps = new HashMap<String, Object>();
            HttpServiceConfigurationTypeHandler h = 
                new HttpServiceConfigurationTypeHandler(dswContext, dp, handlerProps) {
                    @Override
                    ServerFactoryBean createServerFactoryBean() {
                        return sfb;
                    }
    
                    @Override
                    String[] applyIntents(BundleContext dswContext, BundleContext callingContext,
                            List<AbstractFeature> features, AbstractEndpointFactory factory, 
                            ServiceEndpointDescription sd) {
                        return new String [] {};
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
            props.put(Constants.CONFIG_TYPE_PROPERTY, Constants.POJO_CONFIG_TYPE);
            props.put(Constants.POJO_HTTP_SERVICE_CONTEXT, "/myRunnable");
            ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Runnable.class.getName(), props);
            
            assertEquals("Precondition failed", 0, dp.getExposedServices().size());
            h.createServer(sr, dswContext, callingContext, sd, Runnable.class, myService);
            assertEquals(1, dp.getExposedServices().size());
            assertSame(sr, dp.getExposedServices().iterator().next());
            
            String hostName = InetAddress.getLocalHost().getHostName();
            Map<String, String> expected = new HashMap<String, String>();
            expected.put("osgi.remote.configuration.type", "pojo");
            expected.put("osgi.remote.configuration.pojo.address", "https://" + hostName + ":8443/myRunnable");
            assertEquals(expected, dp.getExposedProperties(sr));
        } finally {
            System.setProperties(savedProps);
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

    // TODO these tests still need to be written...
    /*
    public void testNoHttpService() {
        
    }
    
    public void testHttpServiceDynamism() {
        
    }
    
    public void testServletUnregistration() {        
    }
    */
}
