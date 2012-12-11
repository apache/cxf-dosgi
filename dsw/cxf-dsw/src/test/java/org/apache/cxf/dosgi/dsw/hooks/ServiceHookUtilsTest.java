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

import junit.framework.TestCase;

public class ServiceHookUtilsTest extends TestCase {
    
    public void testDUMMY() {
        assertTrue(true);
    }
    
//    public void testCreateServer() {
//        IMocksControl control = EasyMock.createNiceControl();
//        
//        Server srvr = control.createMock(Server.class);
//        ServiceReference serviceReference = control.createMock(ServiceReference.class);
//        BundleContext dswContext = control.createMock(BundleContext.class);
//        BundleContext callingContext = control.createMock(BundleContext.class);
//        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl("java.lang.String");
//        Object service = "hi";
//
//        ConfigurationTypeHandler handler = control.createMock(ConfigurationTypeHandler.class);
//        handler.createServer(serviceReference, dswContext, callingContext, sd, String.class, service);
//        EasyMock.expectLastCall().andReturn(srvr);
//        control.replay();
//        
//        assertSame(srvr, 
//            ServiceHookUtils.createServer(handler, serviceReference, dswContext, callingContext, sd, service));
//    }
//
//    public void testNoServerWhenNoInterfaceSpecified() {
//        IMocksControl control = EasyMock.createNiceControl();
//        
//        Server srvr = control.createMock(Server.class);
//        ServiceReference serviceReference = control.createMock(ServiceReference.class);
//        BundleContext dswContext = control.createMock(BundleContext.class);
//        BundleContext callingContext = control.createMock(BundleContext.class);
//        ServiceEndpointDescription sd = mockServiceDescription(control, "Foo");
//        Object service = "hi";
//
//        ConfigurationTypeHandler handler = control.createMock(ConfigurationTypeHandler.class);
//        handler.createServer(serviceReference, dswContext, callingContext, sd, String.class, service);
//        EasyMock.expectLastCall().andReturn(srvr);
//        control.replay();
//        
//        assertNull(ServiceHookUtils.createServer(handler, serviceReference, dswContext, callingContext, sd, service));        
//    }
//    
//    public void testPublish() throws Exception {        
//        Map<String, Object> props = new HashMap<String, Object>();
//        props.put("foo", "bar");
//        props.put(ServicePublication.ENDPOINT_LOCATION, "http://localhost/xyz");
//        ServiceEndpointDescriptionImpl sed = new ServiceEndpointDescriptionImpl(String.class.getName(), props);
//        assertEquals(new URI("http://localhost/xyz"), sed.getLocation());
//        
//        final Dictionary<String, Object> expectedProps = new Hashtable<String, Object>();
//        expectedProps.put(ServicePublication.SERVICE_PROPERTIES, props);
//        expectedProps.put(ServicePublication.SERVICE_INTERFACE_NAME, Collections.singleton(String.class.getName()));
//        expectedProps.put(ServicePublication.ENDPOINT_LOCATION, new URI("http://localhost/xyz"));
//        
//        BundleContext bc = EasyMock.createMock(BundleContext.class);
//        EasyMock.expect(bc.registerService(
//            EasyMock.eq(ServicePublication.class.getName()),
//            EasyMock.anyObject(), 
//            (Dictionary<?, ?>) EasyMock.anyObject()))
//                .andAnswer(new IAnswer<ServiceRegistration>() {
//                    public ServiceRegistration answer() throws Throwable {
//                        assertTrue(EasyMock.getCurrentArguments()[1] instanceof ServicePublication);
//                        Dictionary<?, ?> actualProps = 
//                            (Dictionary<?, ?>) EasyMock.getCurrentArguments()[2];
//                        UUID uuid = UUID.fromString(actualProps.get(ServicePublication.ENDPOINT_SERVICE_ID).toString());
//                        expectedProps.put(ServicePublication.ENDPOINT_SERVICE_ID, uuid.toString());
//                        assertEquals(expectedProps, actualProps);
//                        return EasyMock.createMock(ServiceRegistration.class);
//                    }                
//                });
//        EasyMock.replay(bc);
//        
//        ServiceHookUtils.publish(bc, null, sed);
//        EasyMock.verify(bc);
//    }
//    
//    private ServiceEndpointDescription mockServiceDescription(IMocksControl control, 
//                                                              String... interfaceNames) {
//        List<String> iList = new ArrayList<String>();
//        for (String iName : interfaceNames) {
//            iList.add(iName);
//        }
//        ServiceEndpointDescription sd = control.createMock(ServiceEndpointDescription.class);
//        sd.getProvidedInterfaces();
//        EasyMock.expectLastCall().andReturn(iList);
//        return sd;
//    }
}


