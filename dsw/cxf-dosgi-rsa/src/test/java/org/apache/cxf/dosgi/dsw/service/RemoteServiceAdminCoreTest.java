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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.api.DistributionProvider;
import org.apache.cxf.dosgi.dsw.api.Endpoint;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({
    "rawtypes", "unchecked", "deprecation"
   })
public class RemoteServiceAdminCoreTest {

    private static final String MYCONFIG = "myconfig";

    @Test
    public void testDontExportOwnServiceProxies() throws InvalidSyntaxException {
        IMocksControl c = EasyMock.createControl();
        Bundle b = c.createMock(Bundle.class);
        BundleContext bc = c.createMock(BundleContext.class);

        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        bc.addServiceListener(EasyMock.<ServiceListener>anyObject(), EasyMock.<String>anyObject());
        EasyMock.expectLastCall().anyTimes();
        bc.removeServiceListener(EasyMock.<ServiceListener>anyObject());
        EasyMock.expectLastCall().anyTimes();

        Dictionary<String, String> d = new Hashtable<String, String>();
        EasyMock.expect(b.getHeaders()).andReturn(d).anyTimes();

        ServiceReference sref = c.createMock(ServiceReference.class);
        EasyMock.expect(sref.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(sref.getPropertyKeys())
            .andReturn(new String[]{"objectClass", "service.exported.interfaces"}).anyTimes();
        EasyMock.expect(sref.getProperty("objectClass")).andReturn(new String[] {"a.b.C"}).anyTimes();
        EasyMock.expect(sref.getProperty(RemoteConstants.SERVICE_IMPORTED)).andReturn(true).anyTimes();
        EasyMock.expect(sref.getProperty("service.exported.interfaces")).andReturn("*").anyTimes();

        DistributionProvider provider = c.createMock(DistributionProvider.class);

        c.replay();

        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, bc, provider);

        // must return an empty List as sref if from the same bundle
        List<ExportRegistration> exRefs = rsaCore.exportService(sref, null);

        assertNotNull(exRefs);
        assertEquals(0, exRefs.size());

        // must be empty
        assertEquals(rsaCore.getExportedServices().size(), 0);

        c.verify();
    }

    @Test
    public void testImport() {
        IMocksControl c = EasyMock.createNiceControl();
        Bundle b = c.createMock(Bundle.class);
        BundleContext bc = c.createMock(BundleContext.class);

        Dictionary<String, String> d = new Hashtable<String, String>();
        EasyMock.expect(b.getHeaders()).andReturn(d).anyTimes();

        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(b.getSymbolicName()).andReturn("BundleName").anyTimes();

        EndpointDescription endpoint = creatEndpointDesc("unsupportedConfiguration");

        DistributionProvider provider = c.createMock(DistributionProvider.class);
        EasyMock.expect(provider.getSupportedTypes())
            .andReturn(new String[]{MYCONFIG}).atLeastOnce();
        c.replay();

        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, bc, provider);

        // must be null as the endpoint doesn't contain any usable configurations
        assertNull(rsaCore.importService(endpoint));
        // must be empty
        assertEquals(0, rsaCore.getImportedEndpoints().size());

        EndpointDescription endpoint2 = creatEndpointDesc(MYCONFIG);

        ImportRegistration ireg = rsaCore.importService(endpoint2);
        assertNotNull(ireg);

        assertEquals(1, rsaCore.getImportedEndpoints().size());

        // lets import the same endpoint once more -> should get a copy of the ImportRegistration
        ImportRegistration ireg2 = rsaCore.importService(endpoint2);
        assertNotNull(ireg2);
        assertEquals(2, rsaCore.getImportedEndpoints().size());

        assertEquals(ireg.getImportReference(), (rsaCore.getImportedEndpoints().toArray())[0]);

        assertEquals(ireg.getImportReference().getImportedEndpoint(), ireg2.getImportReference()
            .getImportedEndpoint());

        // remove the registration

        // first call shouldn't remove the import
        ireg2.close();
        assertEquals(1, rsaCore.getImportedEndpoints().size());

        // second call should really close and remove the import
        ireg.close();
        assertEquals(0, rsaCore.getImportedEndpoints().size());

        c.verify();
    }

    private EndpointDescription creatEndpointDesc(String configType) {
        Map<String, Object> p = new HashMap<String, Object>();
        p.put(RemoteConstants.ENDPOINT_ID, "http://google.de");
        p.put(Constants.OBJECTCLASS, new String[] {
            "es.schaaf.my.class"
        });
        p.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, configType);
        EndpointDescription endpoint = new EndpointDescription(p);
        return endpoint;
    }

    @Test
    public void testExport() throws Exception {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.getProperty(Constants.FRAMEWORK_VERSION)).andReturn(null).anyTimes();
        bc.addServiceListener(EasyMock.<ServiceListener>anyObject(), EasyMock.<String>anyObject());
        EasyMock.expectLastCall().anyTimes();
        bc.removeServiceListener(EasyMock.<ServiceListener>anyObject());
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(bc.getServiceReferences(EasyMock.<String>anyObject(),
                                                EasyMock.<String>anyObject())).andReturn(null).anyTimes();
        EasyMock.expect(bc.getAllServiceReferences(EasyMock.<String>anyObject(),
                                                   EasyMock.<String>anyObject())).andReturn(null).anyTimes();

        Bundle b = createDummyRsaBundle(bc);

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
        EasyMock.expect(bc.createFilter("(service.id=51)"))
            .andReturn(FrameworkUtil.createFilter("(service.id=51)")).anyTimes();
        EasyMock.expect(bc.getProperty(org.osgi.framework.Constants.FRAMEWORK_UUID)).andReturn("1111");
        EasyMock.expect(bc.getServiceReference(PackageAdmin.class)).andReturn(null);
        EasyMock.replay(bc);

        Map<String, Object> eProps = new HashMap<String, Object>(sProps);
        eProps.put("endpoint.id", "http://something");
        eProps.put("service.imported.configs", new String[] {"org.apache.cxf.ws"});
        final EndpointDescription epd = new EndpointDescription(eProps);
        Endpoint er = new Endpoint() {
            
            @Override
            public void close() throws IOException {
            }
            
            @Override
            public EndpointDescription description() {
                return epd;
            }
        };

        DistributionProvider handler = EasyMock.createMock(DistributionProvider.class);
        EasyMock.expect(handler.exportService(anyObject(ServiceReference.class), 
                                              anyObject(Map.class), isA(Class[].class))).andReturn(er);
        EasyMock.replay(handler);

        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, bc, handler);

        // Export the service for the first time
        List<ExportRegistration> ereg = rsaCore.exportService(sref, null);
        assertEquals(1, ereg.size());
        assertNull(ereg.get(0).getException());
        assertSame(sref, ereg.get(0).getExportReference().getExportedService());
        EndpointDescription endpoint = ereg.get(0).getExportReference().getExportedEndpoint();

        Map<String, Object> edProps = endpoint.getProperties();
        assertEquals("http://something", edProps.get("endpoint.id"));
        assertNotNull(edProps.get("service.imported"));
        assertTrue(Arrays.equals(new String[] {"java.lang.Runnable"},
                                 (Object[]) edProps.get("objectClass")));
        assertTrue(Arrays.equals(new String[] {"org.apache.cxf.ws"},
                                 (Object[]) edProps.get("service.imported.configs")));

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

    private Bundle createDummyRsaBundle(BundleContext bc) {
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.expect(b.getSymbolicName()).andReturn("rsabundle").anyTimes();
        EasyMock.expect(b.getHeaders()).andReturn(new Hashtable<String, String>()).anyTimes();
        EasyMock.replay(b);
        return b;
    }

    @Test
    public void testExportException() throws Exception {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);

        Bundle b = createDummyRsaBundle(bc);

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

        Map<String, Object> eProps = new HashMap<String, Object>(sProps);
        eProps.put("endpoint.id", "http://something");
        eProps.put("service.imported.configs", new String[] {"org.apache.cxf.ws"});

        DistributionProvider handler = EasyMock.createMock(DistributionProvider.class);
        EasyMock.expect(handler.exportService(anyObject(ServiceReference.class), 
                                              anyObject(Map.class), isA(Class[].class))).andThrow(new TestException());
        EasyMock.replay(handler);

        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, bc, handler);

        List<ExportRegistration> ereg = rsaCore.exportService(sref, sProps);
        assertEquals(1, ereg.size());
        assertTrue(ereg.get(0).getException() instanceof TestException);

        // Look at the exportedServices data structure
        Field field = RemoteServiceAdminCore.class.getDeclaredField("exportedServices");
        field.setAccessible(true);
        Map<Map<String, Object>, Collection<ExportRegistration>> exportedServices =
                (Map<Map<String, Object>, Collection<ExportRegistration>>) field.get(rsaCore);

        assertEquals("One service was exported", 1, exportedServices.size());
        assertEquals("There is 1 export registration",
                1, exportedServices.values().iterator().next().size());

    }

    private ServiceReference mockServiceReference(final Map<String, Object> sProps) throws ClassNotFoundException {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);

        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.expect((Class)b.loadClass(Runnable.class.getName())).andReturn(Runnable.class);
        EasyMock.replay(b);

        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.replay(bc);

        ServiceReference sref = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.expect(sref.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(sref.getPropertyKeys()).andReturn(sProps.keySet().toArray(new String[] {})).anyTimes();
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
    private static class TestException extends RuntimeException {
    }
    
    @Test
    public void testOverlayProperties() {
        Map<String, Object> sProps = new HashMap<String, Object>();
        Map<String, Object> aProps = new HashMap<String, Object>();

        RemoteServiceAdminCore.overlayProperties(sProps, aProps);
        assertEquals(0, sProps.size());

        sProps.put("aaa", "aval");
        sProps.put("bbb", "bval");
        sProps.put(Constants.OBJECTCLASS, new String[] {"X"});
        sProps.put(Constants.SERVICE_ID, 17L);

        aProps.put("AAA", "achanged");
        aProps.put("CCC", "CVAL");
        aProps.put(Constants.OBJECTCLASS, new String[] {"Y"});
        aProps.put(Constants.SERVICE_ID.toUpperCase(), 51L);

        Map<String, Object> aPropsOrg = new HashMap<String, Object>(aProps);
        RemoteServiceAdminCore.overlayProperties(sProps, aProps);
        assertEquals("The additional properties should not be modified", aPropsOrg, aProps);

        assertEquals(5, sProps.size());
        assertEquals("achanged", sProps.get("aaa"));
        assertEquals("bval", sProps.get("bbb"));
        assertEquals("CVAL", sProps.get("CCC"));
        assertTrue("Should not be possible to override the objectClass property",
                Arrays.equals(new String[] {"X"}, (Object[]) sProps.get(Constants.OBJECTCLASS)));
        assertEquals("Should not be possible to override the service.id property",
                17L, sProps.get(Constants.SERVICE_ID));
    }
    
    @Test
    public void testOverlayProperties2() {
        Map<String, Object> original = new HashMap<String, Object>();

        original.put("MyProp", "my value");
        original.put(Constants.OBJECTCLASS, "myClass");

        Map<String, Object> copy = new HashMap<String, Object>();
        copy.putAll(original);

        // nothing should change here
        Map<String, Object> overload = new HashMap<String, Object>();
        RemoteServiceAdminCore.overlayProperties(copy, overload);

        assertEquals(original.size(), copy.size());
        for (Object key : original.keySet()) {
            assertEquals(original.get(key), copy.get(key));
        }

        copy.clear();
        copy.putAll(original);

        // a property should be added
        overload = new HashMap<String, Object>();
        overload.put("new", "prop");

        RemoteServiceAdminCore.overlayProperties(copy, overload);

        assertEquals(original.size() + 1, copy.size());
        for (Object key : original.keySet()) {
            assertEquals(original.get(key), copy.get(key));
        }
        assertNotNull(overload.get("new"));
        assertEquals("prop", overload.get("new"));

        copy.clear();
        copy.putAll(original);

        // only one property should be added
        overload = new HashMap<String, Object>();
        overload.put("new", "prop");
        overload.put("NEW", "prop");

        RemoteServiceAdminCore.overlayProperties(copy, overload);

        assertEquals(original.size() + 1, copy.size());
        for (Object key : original.keySet()) {
            assertEquals(original.get(key), copy.get(key));
        }
        assertNotNull(overload.get("new"));
        assertEquals("prop", overload.get("new"));

        copy.clear();
        copy.putAll(original);

        // nothing should change here
        overload = new HashMap<String, Object>();
        overload.put(Constants.OBJECTCLASS, "assd");
        overload.put(Constants.SERVICE_ID, "asasdasd");
        RemoteServiceAdminCore.overlayProperties(copy, overload);

        assertEquals(original.size(), copy.size());
        for (Object key : original.keySet()) {
            assertEquals(original.get(key), copy.get(key));
        }

        copy.clear();
        copy.putAll(original);

        // overwrite own prop
        overload = new HashMap<String, Object>();
        overload.put("MyProp", "newValue");
        RemoteServiceAdminCore.overlayProperties(copy, overload);

        assertEquals(original.size(), copy.size());
        for (Object key : original.keySet()) {
            if (!"MyProp".equals(key)) {
                assertEquals(original.get(key), copy.get(key));
            }
        }
        assertEquals("newValue", copy.get("MyProp"));

        copy.clear();
        copy.putAll(original);

        // overwrite own prop in different case
        overload = new HashMap<String, Object>();
        overload.put("MYPROP", "newValue");
        RemoteServiceAdminCore.overlayProperties(copy, overload);

        assertEquals(original.size(), copy.size());
        for (Object key : original.keySet()) {
            if (!"MyProp".equals(key)) {
                assertEquals(original.get(key), copy.get(key));
            }
        }
        assertEquals("newValue", copy.get("MyProp"));
    }
    
    @Test
    public void testCreateEndpointProps() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getProperty("org.osgi.framework.uuid")).andReturn("some_uuid1");
        EasyMock.replay(bc);

        Map<String, Object> sd = new HashMap<String, Object>();
        sd.put(org.osgi.framework.Constants.SERVICE_ID, 42);
        DistributionProvider provider = null;
        RemoteServiceAdminCore rsa = new RemoteServiceAdminCore(bc, bc, provider);
        Map<String, Object> props = rsa.createEndpointProps(sd, new Class[]{String.class});

        Assert.assertFalse(props.containsKey(org.osgi.framework.Constants.SERVICE_ID));
        assertEquals(42, props.get(RemoteConstants.ENDPOINT_SERVICE_ID));
        assertEquals("some_uuid1", props.get(RemoteConstants.ENDPOINT_FRAMEWORK_UUID));
        assertEquals(Arrays.asList("java.lang.String"),
                     Arrays.asList((Object[]) props.get(org.osgi.framework.Constants.OBJECTCLASS)));
        assertEquals("0.0.0", props.get("endpoint.package.version.java.lang"));
    }
}
