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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.apache.cxf.dosgi.dsw.service.DistributionProviderImpl;
import org.apache.cxf.dosgi.dsw.service.ServiceEndpointDescriptionImpl;
import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.service.discovery.ServiceEndpointDescription;


public class ConfigTypeHandlerFactoryTest extends TestCase {
    public void testGetDefaultHandler() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        ConfigTypeHandlerFactory f = ConfigTypeHandlerFactory.getInstance();
        
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl("MyInterface");
        CxfDistributionProvider dp = new DistributionProviderImpl(bc);
        ConfigurationTypeHandler handler = f.getHandler(bc, sd, dp, new HashMap<String, Object>());
        assertTrue(handler instanceof PojoConfigurationTypeHandler);        
        assertSame(dp, ((PojoConfigurationTypeHandler) handler).getDistributionProvider());
    }
    
    
    public void testGetJaxrsHandlerNoIntents() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        ConfigTypeHandlerFactory f = ConfigTypeHandlerFactory.getInstance();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.EXPORTED_CONFIGS, Constants.RS_CONFIG_TYPE);
        
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl("MyInterface", props);
        CxfDistributionProvider dp = new DistributionProviderImpl(bc);
        ConfigurationTypeHandler handler = f.getHandler(bc, sd, dp, new HashMap<String, Object>());
        assertTrue(handler instanceof JaxRSPojoConfigurationTypeHandler);        
        assertSame(dp, ((PojoConfigurationTypeHandler) handler).getDistributionProvider());
    }
    
    public void testGetJaxrsHandlerHttpIntents() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        ConfigTypeHandlerFactory f = ConfigTypeHandlerFactory.getInstance();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.EXPORTED_CONFIGS, Constants.RS_CONFIG_TYPE);
        props.put(Constants.EXPORTED_INTENTS, "HTTP");
        
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl("MyInterface", props);
        CxfDistributionProvider dp = new DistributionProviderImpl(bc);
        ConfigurationTypeHandler handler = f.getHandler(bc, sd, dp, new HashMap<String, Object>());
        assertTrue(handler instanceof JaxRSPojoConfigurationTypeHandler);        
        assertSame(dp, ((PojoConfigurationTypeHandler) handler).getDistributionProvider());
    }
    
    public void testJaxrsPropertyIgnored() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        ConfigTypeHandlerFactory f = ConfigTypeHandlerFactory.getInstance();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.EXPORTED_CONFIGS, Constants.RS_CONFIG_TYPE);
        props.put(Constants.EXPORTED_INTENTS, "SOAP HTTP");
        
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl("MyInterface", props);
        CxfDistributionProvider dp = new DistributionProviderImpl(bc);
        ConfigurationTypeHandler handler = f.getHandler(bc, sd, dp, new HashMap<String, Object>());
        assertTrue(handler instanceof PojoConfigurationTypeHandler);
        assertTrue(!(handler instanceof JaxRSPojoConfigurationTypeHandler));
        assertSame(dp, ((PojoConfigurationTypeHandler) handler).getDistributionProvider());
    }
    
    public void testGetPojoHandler() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        ConfigTypeHandlerFactory f = ConfigTypeHandlerFactory.getInstance();
        
        Map<String, Object> sdProps = new HashMap<String, Object>();
        sdProps.put("osgi.remote.configuration.type", Constants.WS_CONFIG_TYPE);
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(
                Collections.singletonList("MyInterface"), sdProps);

        CxfDistributionProvider dp = new DistributionProviderImpl(bc);
        ConfigurationTypeHandler handler = f.getHandler(bc, sd, dp, new HashMap<String, Object>());
        assertTrue(handler instanceof PojoConfigurationTypeHandler);        
        assertSame(dp, ((PojoConfigurationTypeHandler) handler).getDistributionProvider());
    }
    
    public void testGetPojoHandler2() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        ConfigTypeHandlerFactory f = ConfigTypeHandlerFactory.getInstance();
        
        Map<String, Object> sdProps = new HashMap<String, Object>();
        // use default for this: sdProps.put(Constants.CONFIG_TYPE_PROPERTY, Constants.POJO_CONFIG_TYPE);
        sdProps.put(Constants.WS_ADDRESS_PROPERTY, "http://localhost:9876/abcd");
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(
                Collections.singletonList("MyInterface"), sdProps);

        CxfDistributionProvider dp = new DistributionProviderImpl(bc);
        ConfigurationTypeHandler handler = f.getHandler(bc, sd, dp, new HashMap<String, Object>());
        assertTrue(handler instanceof PojoConfigurationTypeHandler);        
        assertSame(dp, ((PojoConfigurationTypeHandler) handler).getDistributionProvider());
    }

    public void testGetHttpServiceHandler() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        ConfigTypeHandlerFactory f = ConfigTypeHandlerFactory.getInstance();
        
        Map<String, Object> sdProps = new HashMap<String, Object>();
        sdProps.put("osgi.remote.configuration.type", Constants.WS_CONFIG_TYPE);
        sdProps.put(Constants.WS_HTTP_SERVICE_CONTEXT, "/abc");
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(
                Collections.singletonList("MyInterface"), sdProps);
        
        CxfDistributionProvider dp = new DistributionProviderImpl(bc);
        ConfigurationTypeHandler handler = f.getHandler(bc, sd, dp, new HashMap<String, Object>());
        assertTrue(handler instanceof HttpServiceConfigurationTypeHandler);
        assertSame(dp, ((HttpServiceConfigurationTypeHandler) handler).getDistributionProvider());                
    }
    
    public void testGetWSDLHandler() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        ConfigTypeHandlerFactory f = ConfigTypeHandlerFactory.getInstance();
        
        Map<String, Object> sdProps = new HashMap<String, Object>();
        sdProps.put("osgi.remote.configuration.type", Constants.WSDL_CONFIG_TYPE);
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(
                Collections.singletonList("MyInterface"), sdProps);
        
        CxfDistributionProvider dp = new DistributionProviderImpl(bc);
        ConfigurationTypeHandler handler = f.getHandler(bc, sd, dp, new HashMap<String, Object>());
        assertTrue(handler instanceof WsdlConfigurationTypeHandler);        
        assertSame(dp, ((WsdlConfigurationTypeHandler) handler).getDistributionProvider());
    }
    
    public void testUnsupportedConfiguration() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        ConfigTypeHandlerFactory f = ConfigTypeHandlerFactory.getInstance();
        
        Map<String, Object> sdProps = new HashMap<String, Object>();
        sdProps.put("osgi.remote.configuration.type", "foobar");
        ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(
                Collections.singletonList("MyInterface"), sdProps);
        
        assertNull("Should not get a handler as this an unsupported config type", 
                f.getHandler(bc, sd, null, new HashMap<String, Object>()));        
    }
}
