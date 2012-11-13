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
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.jaxrs.provider.AegisElementProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class JaxRSUtilsTest extends TestCase {

    private void addRequiredProps(Map<String, Object> props) {
        props.put(RemoteConstants.ENDPOINT_ID, "http://google.de");
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "myGreatConfiguration");
        props.put(org.osgi.framework.Constants.OBJECTCLASS, new String[] {
            "my.class"
        });
    }

    public void testNoGlobalProviders() {
        Map<String, Object> props = new HashMap<String, Object>();

        addRequiredProps(props);

        props.put(Constants.RS_PROVIDER_GLOBAL_PROP_KEY, "false");

        assertEquals(0, JaxRSUtils.getProviders(null, null, props).size());
    }

    public void testAegisProvider() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.RS_DATABINDING_PROP_KEY, "aegis");
        props.put(Constants.RS_PROVIDER_GLOBAL_PROP_KEY, "false");

        addRequiredProps(props);

        List<Object> providers = JaxRSUtils.getProviders(null, null, props);
        assertEquals(1, providers.size());
        assertEquals(AegisElementProvider.class.getName(), providers.get(0).getClass().getName());
    }

    public void testServiceProviders() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.RS_PROVIDER_PROP_KEY, new Object[] {
            new AegisElementProvider()
        });
        props.put(Constants.RS_PROVIDER_GLOBAL_PROP_KEY, "false");
        addRequiredProps(props);

        List<Object> providers = JaxRSUtils.getProviders(null, null, props);
        assertEquals(1, providers.size());
        assertEquals(AegisElementProvider.class.getName(), providers.get(0).getClass().getName());
    }

    public void testServiceProviderProperty() throws Exception {

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        bc.getBundle();
        EasyMock.expectLastCall().andReturn(bundle).times(2);
        bundle.loadClass(AegisElementProvider.class.getName());
        EasyMock.expectLastCall().andReturn(AegisElementProvider.class);
        bundle.loadClass(JAXBElementProvider.class.getName());
        EasyMock.expectLastCall().andReturn(JAXBElementProvider.class);
        EasyMock.replay(bc, bundle);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.RS_PROVIDER_PROP_KEY,
                  "\r\n org.apache.cxf.jaxrs.provider.AegisElementProvider , \r\n"
                      + "org.apache.cxf.jaxrs.provider.JAXBElementProvider\r\n");

        props.put(Constants.RS_PROVIDER_GLOBAL_PROP_KEY, "false");
        addRequiredProps(props);

        List<Object> providers = JaxRSUtils.getProviders(bc, null, props);
        assertEquals(2, providers.size());
        assertEquals(AegisElementProvider.class.getName(), providers.get(0).getClass().getName());
        assertEquals(JAXBElementProvider.class.getName(), providers.get(1).getClass().getName());
    }

    public void testServiceProviderStrings() throws Exception {

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        bc.getBundle();
        EasyMock.expectLastCall().andReturn(bundle).times(2);
        bundle.loadClass(AegisElementProvider.class.getName());
        EasyMock.expectLastCall().andReturn(AegisElementProvider.class);
        bundle.loadClass(JAXBElementProvider.class.getName());
        EasyMock.expectLastCall().andReturn(JAXBElementProvider.class);
        EasyMock.replay(bc, bundle);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.RS_PROVIDER_PROP_KEY, new String[] {
            "\r\n org.apache.cxf.jaxrs.provider.AegisElementProvider",
            "org.apache.cxf.jaxrs.provider.JAXBElementProvider\r\n"
        });

        props.put(Constants.RS_PROVIDER_GLOBAL_PROP_KEY, "false");
        addRequiredProps(props);

        List<Object> providers = JaxRSUtils.getProviders(bc, null, props);
        assertEquals(2, providers.size());
        assertEquals(AegisElementProvider.class.getName(), providers.get(0).getClass().getName());
        assertEquals(JAXBElementProvider.class.getName(), providers.get(1).getClass().getName());
    }

    public void testCustomGlobalProvider() throws Exception {
        ServiceReference sref = EasyMock.createNiceMock(ServiceReference.class);
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        bc.getServiceReferences(null, JaxRSUtils.PROVIDERS_FILTER);
        EasyMock.expectLastCall().andReturn(new ServiceReference[] {
            sref
        });
        sref.getProperty(Constants.RS_PROVIDER_EXPECTED_PROP_KEY);
        EasyMock.expectLastCall().andReturn(false);
        bc.getService(sref);
        AegisElementProvider p = new AegisElementProvider();
        EasyMock.expectLastCall().andReturn(p);
        EasyMock.replay(bc, sref);
        Map<String, Object> props = new HashMap<String, Object>();
        addRequiredProps(props);

        List<Object> providers = JaxRSUtils.getProviders(bc, null, props);
        assertEquals(1, providers.size());
        assertSame(p, providers.get(0));
    }

    public void testNoCustomGlobalProvider() throws Exception {
        ServiceReference sref = EasyMock.createNiceMock(ServiceReference.class);
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        bc.getServiceReferences(null, JaxRSUtils.PROVIDERS_FILTER);
        EasyMock.expectLastCall().andReturn(new ServiceReference[] {
            sref
        });
        sref.getProperty(Constants.RS_PROVIDER_PROP_KEY);
        EasyMock.expectLastCall().andReturn(false);
        bc.getService(sref);
        AegisElementProvider p = new AegisElementProvider();
        EasyMock.expectLastCall().andReturn(p);
        EasyMock.replay(bc);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.RS_PROVIDER_EXPECTED_PROP_KEY, "true");
        addRequiredProps(props);

        List<Object> providers = JaxRSUtils.getProviders(bc, null, props);
        assertEquals(0, providers.size());
    }

    public void testCustomGlobalProviderExpected() throws Exception {
        ServiceReference sref = EasyMock.createNiceMock(ServiceReference.class);
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        bc.getServiceReferences(null, JaxRSUtils.PROVIDERS_FILTER);
        EasyMock.expectLastCall().andReturn(new ServiceReference[] {
            sref
        });
        sref.getProperty(Constants.RS_PROVIDER_PROP_KEY);
        EasyMock.expectLastCall().andReturn(true);
        bc.getService(sref);
        AegisElementProvider p = new AegisElementProvider();
        EasyMock.expectLastCall().andReturn(p);
        EasyMock.replay(bc, sref);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.RS_PROVIDER_EXPECTED_PROP_KEY, "true");
        addRequiredProps(props);

        List<Object> providers = JaxRSUtils.getProviders(bc, null, props);
        assertEquals(1, providers.size());
        assertSame(p, providers.get(0));
    }

}
