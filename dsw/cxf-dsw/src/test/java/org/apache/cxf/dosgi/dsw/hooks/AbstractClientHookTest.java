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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.osgi.framework.BundleContext;
import org.osgi.service.discovery.ServiceEndpointDescription;

public class AbstractClientHookTest extends TestCase {
    public void testOSGiRemoteProperty() throws Exception {
        IMocksControl control = EasyMock.createNiceControl();
        BundleContext bc = control.createMock(BundleContext.class);
        CxfDistributionProvider dp = control.createMock(CxfDistributionProvider.class);
        ServiceEndpointDescription sed = control.createMock(ServiceEndpointDescription.class);
        EasyMock.expect(sed.getProperties()).andReturn(new HashMap<String, Object>()).anyTimes();
        control.replay();
        
        AbstractClientHook ch = new AbstractClientHook(bc, dp) {
            @Override
            protected String getIdentificationProperty() {
                return "ID";
            }            
        };
        Map<String, Object> props = ch.getProperties(sed);
        assertTrue(Boolean.valueOf((String) props.get("osgi.remote")));
    }
}
