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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.TestUtils;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.service.ServiceEndpointDescriptionImpl;
import org.apache.cxf.endpoint.Server;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.ServiceEndpointDescription;


public class ServiceHookUtilsTest extends TestCase {
    public void testCreateServer() {
        IMocksControl control = EasyMock.createNiceControl();
        
        Server srvr = control.createMock(Server.class);
        ServiceReference serviceReference = control.createMock(ServiceReference.class);
        BundleContext dswContext = control.createMock(BundleContext.class);
        BundleContext callingContext = control.createMock(BundleContext.class);
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl("java.lang.String");
        Object service = "hi";

        ConfigurationTypeHandler handler = control.createMock(ConfigurationTypeHandler.class);
        handler.createServer(serviceReference, dswContext, callingContext, sd, String.class, service);
        EasyMock.expectLastCall().andReturn(srvr);
        control.replay();
        
        assertSame(srvr, 
            ServiceHookUtils.createServer(handler, serviceReference, dswContext, callingContext, sd, service));        
    }

    public void testNoServerWhenNoInterfaceSpecified() {
        IMocksControl control = EasyMock.createNiceControl();
        
        Server srvr = control.createMock(Server.class);
        ServiceReference serviceReference = control.createMock(ServiceReference.class);
        BundleContext dswContext = control.createMock(BundleContext.class);
        BundleContext callingContext = control.createMock(BundleContext.class);
        ServiceEndpointDescription sd = mockServiceDescription(control, "Foo");
        Object service = "hi";

        ConfigurationTypeHandler handler = control.createMock(ConfigurationTypeHandler.class);
        handler.createServer(serviceReference, dswContext, callingContext, sd, String.class, service);
        EasyMock.expectLastCall().andReturn(srvr);
        control.replay();
        
        assertNull(ServiceHookUtils.createServer(handler, serviceReference, dswContext, callingContext, sd, service));        
    }
    
    private ServiceEndpointDescription mockServiceDescription(IMocksControl control, 
                                                             String... interfaceNames) {
        List<String> iList = new ArrayList<String>();
        for (String iName : interfaceNames) {
	    iList.add(iName);
	}
        ServiceEndpointDescription sd = control.createMock(ServiceEndpointDescription.class);
        sd.getProvidedInterfaces();
        EasyMock.expectLastCall().andReturn(iList);
        return sd;
    }
    
}


