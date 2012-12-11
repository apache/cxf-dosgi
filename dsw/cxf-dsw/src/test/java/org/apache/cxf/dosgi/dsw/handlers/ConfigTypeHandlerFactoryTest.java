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

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.DefaultIntentMapFactory;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentManagerImpl;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import static org.junit.Assert.assertTrue;


public class ConfigTypeHandlerFactoryTest {

    @Test
    public void testGetDefaultHandlerNoIntents() {
        ConfigurationTypeHandler handler = getHandlerWith(null, null);
        assertTrue(handler instanceof PojoConfigurationTypeHandler);
    }
    
    @Test
    public void testGetJaxrsHandlerNoIntents() {
        ConfigurationTypeHandler handler = getHandlerWith(Constants.RS_CONFIG_TYPE, null);
        assertTrue(handler instanceof JaxRSPojoConfigurationTypeHandler);
    }

    @Test
    public void testGetJaxrsHandlerHttpIntents() {
        ConfigurationTypeHandler handler = getHandlerWith(Constants.RS_CONFIG_TYPE, "HTTP");
        assertTrue(handler instanceof JaxRSPojoConfigurationTypeHandler);
    }

    @Test
    public void testJaxrsPropertyIgnored() {
        ConfigurationTypeHandler handler = getHandlerWith(Constants.RS_CONFIG_TYPE, "SOAP HTTP");
        assertTrue(handler instanceof PojoConfigurationTypeHandler);
        assertTrue(!(handler instanceof JaxRSPojoConfigurationTypeHandler));
    }

    @Test
    public void testGetPojoHandler() {
        ConfigurationTypeHandler handler = getHandlerWith(Constants.WS_CONFIG_TYPE, null);
        assertTrue(handler instanceof PojoConfigurationTypeHandler);
    }

    @Test
    public void testGetWSDLHandler() {
        ConfigurationTypeHandler handler = getHandlerWith(Constants.WSDL_CONFIG_TYPE, null);
        assertTrue(handler instanceof WsdlConfigurationTypeHandler);
    }

    @Test(expected = RuntimeException.class)
    public void testUnsupportedConfiguration() {
        getHandlerWith("notSupportedConfig", null);
    }
    
    private ConfigurationTypeHandler getHandlerWith(String configType, String intents) {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(RemoteConstants.SERVICE_EXPORTED_CONFIGS, configType);
        serviceProps.put(RemoteConstants.SERVICE_EXPORTED_INTENTS, intents);
        IntentMap intentMap = new IntentMap(new DefaultIntentMapFactory().create());
        IntentManager intentManager = new IntentManagerImpl(intentMap);
        HttpServiceManager httpServiceManager = new HttpServiceManager(bc, null, null);
        ConfigTypeHandlerFactory f = new ConfigTypeHandlerFactory(bc, intentManager, httpServiceManager);
        return f.getHandler(bc, serviceProps);
    }
}
