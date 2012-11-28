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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.dosgi.dsw.handlers.ConfigTypeHandlerFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.handlers.HttpServiceManager;
import org.apache.cxf.dosgi.dsw.qos.DefaultIntentMapFactory;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentManagerImpl;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class RemoteServiceAdminCoreTest {

    @Test
    public void dontExportOwnServiceProxies() {

        IMocksControl c = EasyMock.createNiceControl();
        Bundle b = c.createMock(Bundle.class);
        BundleContext bc = c.createMock(BundleContext.class);

        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();

        Dictionary<?,?> d = new Properties();
        EasyMock.expect(b.getHeaders()).andReturn(d).anyTimes();

        ServiceReference sref = c.createMock(ServiceReference.class);
        EasyMock.expect(sref.getBundle()).andReturn(b).anyTimes();

		ConfigTypeHandlerFactory configTypeHandlerFactory = c.createMock(ConfigTypeHandlerFactory.class);
        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, configTypeHandlerFactory );

        c.replay();

        // must return an empty List as sref if from the same bundle
        List<ExportRegistration> exRefs = rsaCore.exportService(sref, null);

        assertNotNull(exRefs);
        assertEquals(0, exRefs.size());

        // must be empty ...
        assertEquals(rsaCore.getExportedServices().size(), 0);

        c.verify();

    }

    @Test
    public void testImport() {

        IMocksControl c = EasyMock.createNiceControl();
        Bundle b = c.createMock(Bundle.class);
        BundleContext bc = c.createMock(BundleContext.class);

        Dictionary<?,?> d = new Properties();
        EasyMock.expect(b.getHeaders()).andReturn(d).anyTimes();

        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(b.getSymbolicName()).andReturn("BundleName").anyTimes();
        
        Map<String, Object> p = new HashMap<String, Object>();
        p.put(RemoteConstants.ENDPOINT_ID, "http://google.de");
        p.put(Constants.OBJECTCLASS, new String[] {
            "es.schaaf.my.class"
        });
        p.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "unsupportetConfiguration");
        EndpointDescription endpoint = new EndpointDescription(p);
        IntentMap intentMap = new IntentMap(new DefaultIntentMapFactory().create());
        IntentManager intentManager = new IntentManagerImpl(intentMap, 10000);
        HttpServiceManager httpServiceManager = c.createMock(HttpServiceManager.class);
        ConfigTypeHandlerFactory configTypeHandlerFactory = new ConfigTypeHandlerFactory(intentManager, httpServiceManager, null);
        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, configTypeHandlerFactory ) {
            @Override
            protected void proxifyMatchingInterface(String interfaceName, ImportRegistrationImpl imReg,
                                                    ConfigurationTypeHandler handler,
                                                    BundleContext requestingContext) {

            }
        };
        c.replay();

        // must be null as the endpoint doesn't contain any usable configurations
        assertNull(rsaCore.importService(endpoint));
        // must be empty ...
        assertEquals(0, rsaCore.getImportedEndpoints().size());

        p.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, org.apache.cxf.dosgi.dsw.Constants.WS_CONFIG_TYPE);
        endpoint = new EndpointDescription(p);

        ImportRegistration ireg = rsaCore.importService(endpoint);
        assertNotNull(ireg);

        assertEquals(1, rsaCore.getImportedEndpoints().size());

        // lets import the same endpoint once more -> should get a copy of the ImportRegistration
        ImportRegistration ireg2 = rsaCore.importService(endpoint);
        assertNotNull(ireg2);
        assertEquals(2,rsaCore.getImportedEndpoints().size());

        assertEquals(ireg.getImportReference(), (rsaCore.getImportedEndpoints().toArray())[0]);

        assertEquals(ireg.getImportReference().getImportedEndpoint(), ireg2.getImportReference()
            .getImportedEndpoint());

        // remove the registration ....

        // first call shouldn't remove the import ...
        ireg2.close();
        assertEquals(1, rsaCore.getImportedEndpoints().size());

        // second call should really close and remove the import ...
        ireg.close();
        assertEquals(0, rsaCore.getImportedEndpoints().size());

        c.verify();

    }

}
