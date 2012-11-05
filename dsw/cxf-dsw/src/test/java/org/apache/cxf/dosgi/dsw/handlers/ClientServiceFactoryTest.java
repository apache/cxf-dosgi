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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.service.ClientServiceFactory;
import org.apache.cxf.dosgi.dsw.service.ImportRegistrationImpl;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class ClientServiceFactoryTest extends TestCase {
    
    public void testGetService() {
        Object myTestProxyObject = new Object();
        
        IMocksControl control = EasyMock.createNiceControl();
        BundleContext dswContext = control.createMock(BundleContext.class);
        Map<String, Object> map = new HashMap<String,Object>();
        map.put(RemoteConstants.ENDPOINT_ID, "http://google.de");
        map.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "myGreatConfiguration");
        map.put(Constants.OBJECTCLASS, new String[]{"my.class"});
        
        EndpointDescription ed = new EndpointDescription(map);
        ConfigurationTypeHandler handler = control.createMock(ConfigurationTypeHandler.class);

        ImportRegistrationImpl iri = new ImportRegistrationImpl(ed, null);
        
        BundleContext requestingContext = control.createMock(BundleContext.class);
        Bundle requestingBundle = control.createMock(Bundle.class);
        EasyMock.expect(requestingBundle.getBundleContext()).andReturn(requestingContext);
        
        ServiceReference sr = control.createMock(ServiceReference.class);
        ServiceRegistration sreg = control.createMock(ServiceRegistration.class);
        EasyMock.expect(sreg.getReference()).andReturn(sr);
        
        handler.createProxy(sr, dswContext, requestingContext, String.class, ed);
        EasyMock.expectLastCall().andReturn(myTestProxyObject);        
        control.replay();       
        
        ClientServiceFactory csf = new ClientServiceFactory(dswContext, String.class, ed, handler,iri);
        assertSame(myTestProxyObject, csf.getService(requestingBundle, sreg));
    }
}
