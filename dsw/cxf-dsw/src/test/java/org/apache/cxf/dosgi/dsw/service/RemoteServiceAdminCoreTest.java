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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.dosgi.dsw.handlers.ConfigTypeHandlerFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.handlers.ExportResult;
import org.apache.cxf.dosgi.dsw.handlers.HttpServiceManager;
import org.apache.cxf.dosgi.dsw.qos.DefaultIntentMapFactory;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentManagerImpl;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.endpoint.Server;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;


public class RemoteServiceAdminCoreTest {

    @Test
    public void testDontExportOwnServiceProxies() {
        IMocksControl c = EasyMock.createControl();
        Bundle b = c.createMock(Bundle.class);
        BundleContext bc = c.createMock(BundleContext.class);

        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();

        Dictionary<?, ?> d = new Properties();
        EasyMock.expect(b.getHeaders()).andReturn(d).anyTimes();

        ServiceReference sref = c.createMock(ServiceReference.class);
        EasyMock.expect(sref.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(sref.getPropertyKeys()).andReturn(new String[]{
                "objectClass", "service.exported.interfaces"}).anyTimes();
        EasyMock.expect(sref.getProperty("objectClass")).andReturn(new String [] {"a.b.C"}).anyTimes();
        EasyMock.expect(sref.getProperty("service.exported.interfaces")).andReturn("*").anyTimes();

        ConfigTypeHandlerFactory configTypeHandlerFactory = c.createMock(ConfigTypeHandlerFactory.class);
        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, configTypeHandlerFactory);

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

        Dictionary<?, ?> d = new Properties();
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
        ConfigTypeHandlerFactory configTypeHandlerFactory
            = new ConfigTypeHandlerFactory(bc, intentManager, httpServiceManager);
        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, configTypeHandlerFactory) {
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
        assertEquals(2, rsaCore.getImportedEndpoints().size());

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

    @Test
    public void testExport() throws Exception {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);

        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.expect(b.getSymbolicName()).andReturn("rsabundle").anyTimes();
        EasyMock.expect(b.getHeaders()).andReturn(new Hashtable<String, String>()).anyTimes();
        EasyMock.replay(b);

        final Map<String, Object> sProps = new HashMap<String, Object>();
        sProps.put("objectClass", new String[] {"java.lang.Runnable"});
        sProps.put("service.id", 51L);
        sProps.put("myProp", "myVal");
        sProps.put("service.exported.interfaces", "*");
        ServiceReference sref = mockServiceReference(sProps);

        Runnable svcObject = EasyMock.createNiceMock(Runnable.class);
        EasyMock.replay(svcObject);

        EasyMock.expect(bc.getService(sref)).andReturn(svcObject).anyTimes();
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(bc.createFilter("(service.id=51)")).andReturn(FrameworkUtil.createFilter("(service.id=51)")).anyTimes();
        EasyMock.replay(bc);

        // The service properties but with Arrays converted into lists (which is what the impl internally uses
        // to allow these things to be compared).
        Map<String, Object> sPropsMod = new HashMap<String, Object>();
        sPropsMod.put("objectClass", Collections.singletonList("java.lang.Runnable"));
        sPropsMod.put("service.id", 51L);
        sPropsMod.put("myProp", "myVal");
        sPropsMod.put("service.exported.interfaces", "*");

        HashMap<String, Object> eProps = new HashMap<String, Object>(sProps);
        eProps.put("endpoint.id", "http://something");
        eProps.put("service.imported.configs", new String[] {"org.apache.cxf.ws"});
        ExportResult er = new ExportResult(eProps, (Server) null);

        ConfigurationTypeHandler handler = EasyMock.createNiceMock(ConfigurationTypeHandler.class);
        EasyMock.expect(handler.createServer(sref, bc, sref.getBundle().getBundleContext(), sPropsMod, Runnable.class, svcObject)).andReturn(er).once();
        EasyMock.replay(handler);

        ConfigTypeHandlerFactory handlerFactory = EasyMock.createNiceMock(ConfigTypeHandlerFactory.class);
        EasyMock.expect(handlerFactory.getHandler(bc, sPropsMod)).andReturn(handler).once(); // Second time shouldn't get there because it should simply copy
        EasyMock.replay(handlerFactory);
        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, handlerFactory) {};

        // Export the service for the first time
        List<ExportRegistration> ereg = rsaCore.exportService(sref, null);
        assertEquals(1, ereg.size());
        assertNull(ereg.get(0).getException());
        assertSame(sref, ereg.get(0).getExportReference().getExportedService());
        EndpointDescription ed = ereg.get(0).getExportReference().getExportedEndpoint();

        Map<String, Object> edProps = ed.getProperties();
        assertEquals("http://something", edProps.get("endpoint.id"));
        assertNotNull(edProps.get("service.imported"));
        assertTrue(Arrays.equals(new String [] {"java.lang.Runnable"}, (Object[]) edProps.get("objectClass")));
        assertTrue(Arrays.equals(new String[] {"org.apache.cxf.ws"}, (Object[]) edProps.get("service.imported.configs")));

        // Ask to export the same service again, this should not go through the whole process again but simply return
        // a copy of the first instance.
        final Map<String, Object> sProps2 = new HashMap<String, Object>();
        sProps2.put("objectClass", new String[] {"java.lang.Runnable"});
        sProps2.put("service.id", 51L);
        sProps2.put("service.exported.interfaces", "*");
        ServiceReference sref2 = mockServiceReference(sProps2);
        Map<String, Object> props2 = new HashMap<String, Object>();
        props2.put("myProp", "myVal");
        List<ExportRegistration> ereg2 = rsaCore.exportService(sref2, props2);

        assertEquals(1, ereg2.size());
        assertNull(ereg2.get(0).getException());
        assertEquals(ereg.get(0).getExportReference().getExportedEndpoint().getProperties(),
                ereg2.get(0).getExportReference().getExportedEndpoint().getProperties());

        // Look at the exportedServices data structure
        Field field = RemoteServiceAdminCore.class.getDeclaredField("exportedServices");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Map<String, Object>, Collection<ExportRegistration>> exportedServices =
                (Map<Map<String, Object>, Collection<ExportRegistration>>) field.get(rsaCore);

        assertEquals("One service was exported", 1, exportedServices.size());
        assertEquals("There are 2 export registrations (identical copies)",
                2, exportedServices.values().iterator().next().size());

        // Unregister one of the exports
        rsaCore.removeExportRegistration((ExportRegistrationImpl) ereg.get(0));
        assertEquals("One service was exported", 1, exportedServices.size());
        assertEquals("There 1 export registrations left",
                1, exportedServices.values().iterator().next().size());

        // Unregister the other export
        rsaCore.removeExportRegistration((ExportRegistrationImpl) ereg2.get(0));
        assertEquals("No more exported services", 0, exportedServices.size());
    }

    @Test
    public void testExportException() throws Exception {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);

        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.expect(b.getSymbolicName()).andReturn("rsabundle").anyTimes();
        EasyMock.expect(b.getHeaders()).andReturn(new Hashtable<String, String>()).anyTimes();
        EasyMock.replay(b);

        final Map<String, Object> sProps = new HashMap<String, Object>();
        sProps.put("objectClass", new String[] {"java.lang.Runnable"});
        sProps.put("service.id", 51L);
        sProps.put("service.exported.interfaces", "*");
        ServiceReference sref = mockServiceReference(sProps);

        Runnable svcObject = EasyMock.createNiceMock(Runnable.class);
        EasyMock.replay(svcObject);

        EasyMock.expect(bc.getService(sref)).andReturn(svcObject).anyTimes();
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.replay(bc);

        // The service properties but with Arrays converted into lists (which is what the impl internally uses
        // to allow these things to be compared).
        Map<String, Object> sPropsMod = new HashMap<String, Object>();
        sPropsMod.put("objectClass", Collections.singletonList("java.lang.Runnable"));
        sPropsMod.put("service.id", 51L);
        sPropsMod.put("service.exported.interfaces", "*");

        HashMap<String, Object> eProps = new HashMap<String, Object>(sProps);
        eProps.put("endpoint.id", "http://something");
        eProps.put("service.imported.configs", new String[] {"org.apache.cxf.ws"});
        ExportResult er = new ExportResult(eProps, new TestException());

        ConfigurationTypeHandler handler = EasyMock.createNiceMock(ConfigurationTypeHandler.class);
        EasyMock.expect(handler.createServer(sref, bc, sref.getBundle().getBundleContext(), sPropsMod, Runnable.class, svcObject)).andReturn(er);
        EasyMock.replay(handler);

        ConfigTypeHandlerFactory handlerFactory = EasyMock.createNiceMock(ConfigTypeHandlerFactory.class);
        EasyMock.expect(handlerFactory.getHandler(bc, sPropsMod)).andReturn(handler).anyTimes();
        EasyMock.replay(handlerFactory);
        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, handlerFactory) {};

        List<ExportRegistration> ereg = rsaCore.exportService(sref, null);
        assertEquals(1, ereg.size());
        assertTrue(ereg.get(0).getException() instanceof TestException);
        assertSame(sref, ereg.get(0).getExportReference().getExportedService());

        // Look at the exportedServices data structure
        Field field = RemoteServiceAdminCore.class.getDeclaredField("exportedServices");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Map<String, Object>, Collection<ExportRegistration>> exportedServices =
                (Map<Map<String, Object>, Collection<ExportRegistration>>) field.get(rsaCore);

        assertEquals("One service was exported", 1, exportedServices.size());
        assertEquals("There is 1 export registration",
                1, exportedServices.values().iterator().next().size());

        // Remove all export registrations from the service bundle
        rsaCore.removeExportRegistrations(sref.getBundle().getBundleContext());
        assertEquals("No more exported services", 0, exportedServices.size());
    }

    private ServiceReference mockServiceReference(final Map<String, Object> sProps) {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);

        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.replay(b);

        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.replay(bc);

        ServiceReference sref = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.expect(sref.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(sref.getPropertyKeys()).andReturn(sProps.keySet().toArray(new String [] {})).anyTimes();
        EasyMock.expect(sref.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                return sProps.get(EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(sref);
        return sref;
    }

    @SuppressWarnings("serial")
    private static class TestException extends Exception {}
}
