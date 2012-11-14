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
package org.apache.cxf.dosgi.dsw;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class ActivatorTest extends TestCase{
    private BundleContext getMockBundleContext(IMocksControl control) {
        Bundle b = control.createMock(Bundle.class);
        Hashtable<String, String> ht = new Hashtable<String, String>();
        EasyMock.expect(b.getHeaders()).andReturn(ht).anyTimes();        
        BundleContext bc = control.createMock(BundleContext.class);

        EasyMock.expect(b.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        return bc;
    }
    
    public void testCreateAndShutdownRemoteServiceAdminService() throws Exception {
        IMocksControl control = EasyMock.createNiceControl();
        BundleContext bc = getMockBundleContext(control);
        ServiceRegistration sr = control.createMock(ServiceRegistration.class);
        EasyMock.expect(bc.registerService(EasyMock.eq(RemoteServiceAdmin.class.getName()),EasyMock.anyObject(), (Dictionary)EasyMock.anyObject())).andReturn(sr).atLeastOnce();
        
        control.replay();
        Activator a = new Activator();
        a.setBundleContext(bc);
        a.start();
        control.verify();
    }
    
}
