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
package org.apache.cxf.dosgi.dsw.hooks;

import org.junit.Assert;
import org.junit.Test;

public class CxfPublishHookTest extends Assert {

    @Test
    public void testDUMMY() throws Exception {
        
    }

    //
    // private IMocksControl control;
    //    
    // @Before
    // public void setUp() {
    // control = EasyMock.createNiceControl();
    // }
    //
    // @Test
    // public void testPublishSingleInterface() throws Exception {
    // String[] serviceNames = new String[]{TestService.class.getName()};
    // String[] addresses = new String[]{"http://localhost:9000/hello"};
    // doTestPublishHook("remote-services.xml", serviceNames, addresses);
    // }
    //
    // @Test
    // public void testPublishSingleInterfaceAltFormat() throws Exception {
    // String[] serviceNames = new String[]{TestService.class.getName()};
    // String[] addresses = new String[]{"http://localhost:9000/hello"};
    // doTestPublishHook("alt-remote-services.xml", serviceNames, addresses);
    // }
    //
    // @Test
    // public void testPublishMultiInterface() throws Exception {
    // String[] serviceNames = new String[]{TestService.class.getName(),
    // AdditionalInterface.class.getName()};
    // String[] addresses = new String[]{"http://localhost:9001/hello",
    // "http://localhost:9002/hello"};
    // doTestPublishHook("multi-services.xml", serviceNames, addresses);
    // }
    //    
    // @SuppressWarnings("unchecked")
    // private void doTestPublishHook(String remoteServices,
    // String[] serviceNames,
    // String[] addresses) throws Exception {
    //        
    // Bundle bundle = control.createMock(Bundle.class);
    // bundle.findEntries(EasyMock.eq("OSGI-INF/remote-service"),
    // EasyMock.eq("*.xml"), EasyMock.anyBoolean());
    // EasyMock.expectLastCall().andReturn(Collections.enumeration(
    // Arrays.asList(getClass().getResource("/OSGI-INF/remote-service/" + remoteServices))));
    // Dictionary<String, String> bundleHeaders = new Hashtable<String, String>();
    // bundleHeaders.put(org.osgi.framework.Constants.BUNDLE_NAME,
    // "Test Bundle");
    // bundleHeaders.put(org.osgi.framework.Constants.BUNDLE_VERSION,
    // "1.0.0");
    // bundle.getHeaders();
    // EasyMock.expectLastCall().andReturn(bundleHeaders).anyTimes();
    // BundleContext requestingContext = control.createMock(BundleContext.class);
    // bundle.getBundleContext();
    // EasyMock.expectLastCall().andReturn(requestingContext).anyTimes();
    //       
    // TestService serviceObject = new TestServiceImpl();
    // Dictionary serviceProps = new Hashtable();
    //
    // ServiceReference sref = control.createMock(ServiceReference.class);
    // sref.getBundle();
    // EasyMock.expectLastCall().andReturn(bundle).anyTimes();
    // sref.getProperty(Constants.OBJECTCLASS);
    // EasyMock.expectLastCall().andReturn(serviceNames).anyTimes();
    // sref.getPropertyKeys();
    // EasyMock.expectLastCall().andReturn(new String[]{}).anyTimes();
    //        
    // BundleTestContext dswContext = new BundleTestContext(bundle);
    //
    // ServiceRegistration[] serviceRegistrations =
    // new ServiceRegistration[serviceNames.length];
    //
    // for (int i = 0; i < serviceNames.length ; i++) {
    // serviceRegistrations[i] =
    // control.createMock(ServiceRegistration.class);
    // dswContext.addServiceRegistration(serviceNames[i],
    // serviceRegistrations[i]);
    // dswContext.addServiceReference(serviceNames[i], sref);
    // }
    // dswContext.registerService(serviceNames, serviceObject, serviceProps);
    //        
    // Server server = control.createMock(Server.class);
    //
    // String publicationClass = ServicePublication.class.getName();
    // ServiceRegistration publicationRegistration =
    // control.createMock(ServiceRegistration.class);
    // publicationRegistration.unregister();
    // EasyMock.expectLastCall().times(serviceNames.length);
    // dswContext.addServiceRegistration(publicationClass, publicationRegistration);
    // ServiceReference publicationReference =
    // control.createMock(ServiceReference.class);
    // dswContext.addServiceReference(publicationClass, publicationReference);
    // control.replay();
    //     
    // TestPublishHook hook = new TestPublishHook(dswContext,
    // serviceObject,
    // server);
    // hook.publishEndpoint(sref);
    // hook.verify();
    //
    // assertEquals(1, hook.getEndpoints().size());
    // List<EndpointInfo> list = hook.getEndpoints().get(sref);
    // assertNotNull(list);
    // assertEquals(serviceNames.length, list.size());
    // for (int i = 0; i < serviceNames.length; i++) {
    // assertNotNull(list.get(i));
    // ServiceEndpointDescription sd = list.get(i).getServiceDescription();
    // assertNotNull(sd);
    // assertNotNull(sd.getProvidedInterfaces());
    // assertEquals(1, sd.getProvidedInterfaces().size());
    // Collection names = sd.getProvidedInterfaces();
    // assertEquals(1, names.size());
    // assertEquals(serviceNames[i], names.toArray()[0]);
    // String excludeProp = "osgi.remote.interfaces";
    // assertNull(sd.getProperties().get(excludeProp));
    // String addrProp =
    // org.apache.cxf.dosgi.dsw.Constants.WS_ADDRESS_PROPERTY_OLD;
    // assertEquals(addresses[i], sd.getProperties().get(addrProp));
    // }
    //
    // Map<String, ServiceRegistration> registeredRegs =
    // dswContext.getRegisteredRegistrations();
    // assertNotNull(registeredRegs);
    // assertEquals(serviceNames.length + 1, registeredRegs.size());
    // assertNotNull(registeredRegs.get(publicationClass));
    // assertSame(publicationRegistration, registeredRegs.get(publicationClass));
    //
    // Map<String, List<Dictionary>> registeredProps =
    // dswContext.getRegisteredProperties();
    // assertNotNull(registeredProps);
    // assertEquals(serviceNames.length + 1, registeredProps.size());
    // assertNotNull(registeredProps.get(publicationClass));
    // List<Dictionary> propsList = registeredProps.get(publicationClass);
    // assertEquals(serviceNames.length, propsList.size());
    // for (Dictionary props : propsList) {
    // Collection interfaces =
    // (Collection)props.get(SERVICE_INTERFACE_NAME);
    // assertNotNull(interfaces);
    // assertTrue(interfaces.contains(TestService.class.getName())
    // || interfaces.contains(AdditionalInterface.class.getName()));
    // }
    //
    // hook.removeEndpoints();
    //
    // control.verify();
    // }
    //    
    // @SuppressWarnings("unchecked")
    // @Test
    // public void testPublishMultipleTimes() {
    // Bundle bundle = control.createMock(Bundle.class);
    // bundle.findEntries(EasyMock.eq("OSGI-INF/remote-service"),
    // EasyMock.eq("*.xml"), EasyMock.anyBoolean());
    // EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
    // public Object answer() throws Throwable {
    // return Collections.enumeration(Arrays.asList(
    // getClass().getResource("/OSGI-INF/remote-service/remote-services.xml")));
    // }
    // }).anyTimes();
    // Dictionary<String, String> bundleHeaders = new Hashtable<String, String>();
    // bundleHeaders.put(org.osgi.framework.Constants.BUNDLE_NAME,
    // "org.apache.cxf.example.bundle");
    // bundleHeaders.put(org.osgi.framework.Constants.BUNDLE_VERSION,
    // "1.0.0");
    // bundle.getHeaders();
    // EasyMock.expectLastCall().andReturn(bundleHeaders).anyTimes();
    // BundleContext requestingContext = control.createMock(BundleContext.class);
    // bundle.getBundleContext();
    // EasyMock.expectLastCall().andReturn(requestingContext).anyTimes();
    //
    // TestService serviceObject = new TestServiceImpl();
    // Dictionary serviceProps = new Hashtable();
    //
    // ServiceReference sref = control.createMock(ServiceReference.class);
    // sref.getBundle();
    // EasyMock.expectLastCall().andReturn(bundle).anyTimes();
    // sref.getProperty(Constants.OBJECTCLASS);
    // String [] serviceNames = {TestService.class.getName()};
    // EasyMock.expectLastCall().andReturn(serviceNames).anyTimes();
    // sref.getPropertyKeys();
    // EasyMock.expectLastCall().andReturn(new String[]{}).anyTimes();
    //              
    // BundleTestContext dswContext = new BundleTestContext(bundle);
    // ServiceRegistration[] serviceRegistrations =
    // new ServiceRegistration[serviceNames.length];
    // for (int i = 0; i < serviceNames.length ; i++) {
    // serviceRegistrations[i] =
    // control.createMock(ServiceRegistration.class);
    // dswContext.addServiceRegistration(serviceNames[i],
    // serviceRegistrations[i]);
    // dswContext.addServiceReference(serviceNames[i], sref);
    // }
    // dswContext.registerService(serviceNames, serviceObject, serviceProps);
    //        
    // final Server server = control.createMock(Server.class);
    // control.replay();
    //
    // CxfPublishHook hook = new CxfPublishHook(dswContext, null) {
    // @Override
    // Server createServer(ServiceReference sref, ServiceEndpointDescription sd) {
    // return server;
    // }
    // };
    // assertNull("Precondition not met", hook.getEndpoints().get(sref));
    // hook.publishEndpoint(sref);
    // assertEquals(1, hook.getEndpoints().get(sref).size());
    //
    // hook.endpoints.put(sref, new ArrayList<EndpointInfo>());
    // assertEquals("Precondition failed", 0, hook.getEndpoints().get(sref).size());
    // hook.publishEndpoint(sref);
    // assertEquals(0, hook.getEndpoints().get(sref).size());
    //        
    // control.verify();
    // }
    //
    // private static class TestPublishHook extends CxfPublishHook {
    //        
    // private boolean called;
    // private TestService serviceObject;
    // private Server server;
    //        
    // public TestPublishHook(BundleContext bc, TestService serviceObject,
    // Server s) {
    // super(bc, null);
    // this.serviceObject = serviceObject;
    // this.server = s;
    // }
    //        
    // @Override
    // protected ConfigurationTypeHandler getHandler(ServiceEndpointDescription sd,
    // Map<String, Object> props) {
    // return new ConfigurationTypeHandler() {
    // public String getType() {
    // return "test";
    // }
    //
    // public Object createProxy(ServiceReference sr,
    // BundleContext dswContext, BundleContext callingContext,
    // Class<?> iClass, ServiceEndpointDescription sd) {
    // throw new UnsupportedOperationException();
    // }
    //
    // public Server createServer(ServiceReference sr,
    // BundleContext dswContext, BundleContext callingContext,
    // ServiceEndpointDescription sd, Class<?> iClass, Object serviceBean) {
    // Assert.assertSame(serviceBean, serviceObject);
    // TestPublishHook.this.setCalled();
    // Map props = sd.getProperties();
    // String address = (String)props.get(WS_ADDRESS_PROPERTY);
    // if (address != null) {
    // props.put(ENDPOINT_LOCATION, address);
    // }
    // return server;
    // }
    //                
    // };
    // }
    //        
    // public void setCalled() {
    // called = true;
    // }
    //        
    // public void verify() {
    // Assert.assertTrue(called);
    // }
    // }
    //
    // public interface AdditionalInterface {
    // }
    //    
    // private static class TestServiceImpl implements TestService, AdditionalInterface {
    //        
    // }
}
