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
package org.apache.cxf.dosgi.dsw.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.jaxws.spring.EndpointDefinitionParser;
import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import org.junit.Test;
import static org.junit.Assert.*;

public class RenoteServiceAdminCoreTest {

    @Test
    public void dontExportOwnServiceProxies(){
        
        IMocksControl c = EasyMock.createNiceControl();
        Bundle b = c.createMock(Bundle.class);
        BundleContext bc = c.createMock(BundleContext.class);
        
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        
        ServiceReference sref = c.createMock(ServiceReference.class);
        EasyMock.expect(sref.getBundle()).andReturn(b).anyTimes();
        
        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc);
        
        
        c.replay();
        
        
        // must return null as sref if from the same bundle 
        assertNull(rsaCore.exportService(sref, null));
        
        // must be empty ... 
        assertEquals(rsaCore.getExportedServices().size(),0);
        
        c.verify();
        
    }
    
    
    @Test
    public void testImport(){
        
        IMocksControl c = EasyMock.createNiceControl();
        Bundle b = c.createMock(Bundle.class);
        BundleContext bc = c.createMock(BundleContext.class);
        
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(b.getSymbolicName()).andReturn("BundleName").anyTimes();
        
        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc){
            @Override
            protected void proxifyMatchingInterface(String interfaceName, ImportRegistrationImpl imReg,
                                                    ConfigurationTypeHandler handler,
                                                    BundleContext requestingContext) {

                
                
            }
        };
        
        Map p = new HashMap();
        p.put(RemoteConstants.ENDPOINT_URI, "http://google.de");
        
        EndpointDescription endpoint = new EndpointDescription(p);
        
        
        c.replay();
        
        // must be null as the endpoint doesn't contain any usable configurations
        assertNull(rsaCore.importService(endpoint));
        // must be empty ... 
        assertEquals(rsaCore.getImportedEndpoints().size(),0);
        
        
        p.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "org.apache.cxf.ws");
        endpoint = new EndpointDescription(p);
        
        // must be null as the endpoint has no interface
        assertNull(rsaCore.importService(endpoint));
        // must be empty ... 
        assertEquals(rsaCore.getImportedEndpoints().size(),0);
        
        
        p.put(Constants.OBJECTCLASS, new String[] {"es.schaaf.my.class"});
        endpoint = new EndpointDescription(p);
        
        
        ImportRegistration ireg = rsaCore.importService(endpoint);
        assertNotNull(ireg);
        
        assertEquals(rsaCore.getImportedEndpoints().size(),1);
        
        
        // lets import the same endpoint once more -> should get a copy of the ImportRegistration
        ImportRegistration ireg2 = rsaCore.importService(endpoint);
        assertNotNull(ireg2);
        assertEquals(rsaCore.getImportedEndpoints().size(),1);
        
        assertEquals(ireg,(rsaCore.getImportedEndpoints().toArray())[0]);

        assertEquals(ireg.getImportReference().getImportedEndpoint(),ireg2.getImportReference().getImportedEndpoint());
        
        
        // remove the registration ....
        
        // first call shouldn't remove the import ... 
        ireg2.close();
        assertEquals(1,rsaCore.getImportedEndpoints().size());
        
        // second call should really close and remove the import ...
        ireg.close();
        assertEquals(0,rsaCore.getImportedEndpoints().size());
        
        
        c.verify();
        
        
    }
}
