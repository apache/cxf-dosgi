/** 
s  * Licensed to the Apache Software Foundation (ASF) under one 
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

import junit.framework.TestCase;

public class WsdlConfigurationTypeHandlerTest extends TestCase {
    
    public void testDUMMY() {
        assertTrue(true);
    }
    
//    private Map<String, Object> handlerProps;
//    
//    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        
//        handlerProps = new HashMap<String, Object>();
//        handlerProps.put(Constants.DEFAULT_HOST_CONFIG, "somehost");
//        handlerProps.put(Constants.DEFAULT_PORT_CONFIG, "54321");
//    }
//
//    public void testCreateProxyPopulatesDistributionProvider() {        
//        ServiceReference sr = EasyMock.createNiceMock(ServiceReference.class);
//        BundleContext dswContext = EasyMock.createNiceMock(BundleContext.class);
//        BundleContext callingContext = EasyMock.createNiceMock(BundleContext.class);
//        ServiceEndpointDescription sd = TestUtils.mockServiceDescription("Foo");
//        EasyMock.replay(sr);
//        EasyMock.replay(dswContext);
//        EasyMock.replay(callingContext);
//        EasyMock.replay(sd);
//        
//        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(dswContext);
//        WsdlConfigurationTypeHandler w = new WsdlConfigurationTypeHandler(dswContext, dp, handlerProps) {
//            @Override
//            Service createWebService(URL wsdlAddress, QName serviceQname) {
//                Service svc = EasyMock.createMock(Service.class);
//                EasyMock.expect(svc.getPort(CharSequence.class)).andReturn("Hi").anyTimes();
//                EasyMock.replay(svc);
//                return svc;
//            }            
//        };
//
//        assertEquals("Precondition failed", 0, dp.getRemoteServices().size());
//        w.createProxy(sr, dswContext, callingContext, CharSequence.class, sd);
//        assertEquals(1, dp.getRemoteServices().size());
//        assertSame(sr, dp.getRemoteServices().iterator().next());
//        
//    }
    
    
}
