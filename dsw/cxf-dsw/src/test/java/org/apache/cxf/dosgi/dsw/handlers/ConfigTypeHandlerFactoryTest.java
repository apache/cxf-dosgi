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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentManagerImpl;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class ConfigTypeHandlerFactoryTest extends TestCase {

    public void testGetJaxrsHandlerNoIntents() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        List<String> configs = new ArrayList<String>();
        Map<String, Object> serviceProps = new HashMap<String, Object>();

        IntentMap intentMap = new IntentMap();
        IntentManager intentManager = new IntentManagerImpl(intentMap );
        ConfigTypeHandlerFactory f = new ConfigTypeHandlerFactory(intentManager );

        configs.add(Constants.RS_CONFIG_TYPE);

        ConfigurationTypeHandler handler = f.getHandler(bc, configs, serviceProps, null);
        assertTrue(handler instanceof JaxRSPojoConfigurationTypeHandler);
    }

    public void testGetJaxrsHandlerHttpIntents() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        List<String> configs = new ArrayList<String>();
        Map<String, Object> serviceProps = new HashMap<String, Object>();

        IntentMap intentMap = new IntentMap();
        IntentManager intentManager = new IntentManagerImpl(intentMap);
        ConfigTypeHandlerFactory f = new ConfigTypeHandlerFactory(intentManager);
        configs.add(Constants.RS_CONFIG_TYPE);
        serviceProps.put(RemoteConstants.SERVICE_EXPORTED_INTENTS, "HTTP");

        ConfigurationTypeHandler handler = f.getHandler(bc, configs, serviceProps, null);
        assertTrue(handler instanceof JaxRSPojoConfigurationTypeHandler);
    }

    public void testJaxrsPropertyIgnored() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        List<String> configs = new ArrayList<String>();
        Map<String, Object> serviceProps = new HashMap<String, Object>();

        IntentMap intentMap = new IntentMap();
        IntentManager intentManager = new IntentManagerImpl(intentMap);
        ConfigTypeHandlerFactory f = new ConfigTypeHandlerFactory(intentManager);
        configs.add(Constants.RS_CONFIG_TYPE);
        serviceProps.put(RemoteConstants.SERVICE_EXPORTED_INTENTS, "SOAP HTTP");

        ConfigurationTypeHandler handler = f.getHandler(bc, configs, serviceProps, null);
        assertTrue(handler instanceof PojoConfigurationTypeHandler);
        assertTrue(!(handler instanceof JaxRSPojoConfigurationTypeHandler));
    }

    public void testGetPojoHandler() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        List<String> configs = new ArrayList<String>();
        Map<String, Object> serviceProps = new HashMap<String, Object>();

        IntentMap intentMap = new IntentMap();
        IntentManager intentManager = new IntentManagerImpl(intentMap);
        ConfigTypeHandlerFactory f = new ConfigTypeHandlerFactory(intentManager);
        configs.add(Constants.WS_CONFIG_TYPE);

        ConfigurationTypeHandler handler = f.getHandler(bc, configs, serviceProps, null);
        assertTrue(handler instanceof PojoConfigurationTypeHandler);
    }

    public void testGetHttpServiceHandler() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        IntentMap intentMap = new IntentMap();
        IntentManager intentManager = new IntentManagerImpl(intentMap);
        ConfigTypeHandlerFactory f = new ConfigTypeHandlerFactory(intentManager);

        List<String> configs = new ArrayList<String>();
        Map<String, Object> serviceProps = new HashMap<String, Object>();

        configs.add(Constants.WS_CONFIG_TYPE);
        serviceProps.put(Constants.WS_HTTP_SERVICE_CONTEXT, "/abc");

        ConfigurationTypeHandler handler = f.getHandler(bc, configs, serviceProps, null);
        assertTrue(handler instanceof HttpServiceConfigurationTypeHandler);
    }

    public void testGetWSDLHandler() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        IntentMap intentMap = new IntentMap();
        IntentManager intentManager = new IntentManagerImpl(intentMap);
        ConfigTypeHandlerFactory f = new ConfigTypeHandlerFactory(intentManager);

        List<String> configs = new ArrayList<String>();
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        configs.add(Constants.WSDL_CONFIG_TYPE);

        ConfigurationTypeHandler handler = f.getHandler(bc, configs, serviceProps, null);

        assertTrue(handler instanceof WsdlConfigurationTypeHandler);
    }

    public void testUnsupportedConfiguration() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        IntentMap intentMap = new IntentMap();
        IntentManager intentManager = new IntentManagerImpl(intentMap);
        ConfigTypeHandlerFactory f = new ConfigTypeHandlerFactory(intentManager);

        List<String> configs = new ArrayList<String>();
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        configs.add("notSupportedConfig");

        assertNull("Should not get a handler as this an unsupported config type", f.getHandler(bc, configs,
                                                                                               serviceProps,
                                                                                               null));
    }
}
