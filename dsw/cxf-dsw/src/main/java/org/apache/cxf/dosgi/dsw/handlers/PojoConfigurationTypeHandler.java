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

import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentUnsatifiedException;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PojoConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PojoConfigurationTypeHandler.class);

    public PojoConfigurationTypeHandler(BundleContext dswBC, IntentManager intentManager, HttpServiceManager httpServiceManager) {
        super(dswBC, intentManager, httpServiceManager);
    }
    
    public String[] getSupportedTypes() {
        return new String[] {Constants.WS_CONFIG_TYPE, Constants.WS_CONFIG_TYPE_OLD};
    }

    public Object createProxy(ServiceReference serviceReference, BundleContext dswContext,
            BundleContext callingContext, Class<?> iClass, EndpointDescription sd) throws IntentUnsatifiedException {
        String address = getClientAddress(sd.getProperties(), iClass);
        if (address == null) {
            LOG.warn("Remote address is unavailable");
            // TODO: fire Event
            return null;
        }

        LOG.info("Creating a " + iClass.getName() + " client, endpoint address is " + address);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClientProxyFactoryBean factory = createClientProxyFactoryBean(serviceReference, iClass);
            factory.setServiceClass(iClass);
            factory.setAddress(address);
            InterceptorUtils.addWsInterceptorsFeaturesProps(factory.getClientFactoryBean(), callingContext, sd.getProperties());
            setClientWsdlProperties(factory.getClientFactoryBean(), dswContext, sd.getProperties(), false);

            intentManager.applyIntents(factory.getFeatures(), factory.getClientFactoryBean(), sd.getProperties());

            Thread.currentThread().setContextClassLoader(ClientProxyFactoryBean.class.getClassLoader());
            Object proxy = getProxy(factory.create(), iClass);
            return proxy;
        } catch (Exception e) {
            LOG.warn("proxy creation failed", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        return null;
    }
    
    public ExportResult createServer(ServiceReference sref, BundleContext dswContext,
                             BundleContext callingContext, Map<String, Object> sd, Class<?> iClass, Object serviceBean) throws IntentUnsatifiedException {
        String address = getServerAddress(sd, iClass);
        String contextRoot = httpServiceManager.getServletContextRoot(sd, iClass);
        
        ServerFactoryBean factory = createServerFactoryBean(sref, iClass);
        if (contextRoot != null) {
            Bus bus = httpServiceManager.registerServletAndGetBus(contextRoot, callingContext, sref);
            factory.setBus(bus);
        }
        factory.setServiceClass(iClass);
        factory.setAddress(address);
        factory.setServiceBean(serviceBean);
        InterceptorUtils.addWsInterceptorsFeaturesProps(factory, callingContext, sd);
        setWsdlProperties(factory, callingContext, sd, false);
        String[] intents = intentManager.applyIntents(factory.getFeatures(), factory, sd);
        
        String completeEndpointAddress = httpServiceManager.getAbsoluteAddress(dswContext, contextRoot, address);

        // The properties for the EndpointDescription
        Map<String, Object> endpointProps = createEndpointProps(sd, iClass, new String[]{Constants.WS_CONFIG_TYPE}, completeEndpointAddress,intents);

        return createServerFromFactory(factory, endpointProps);
    }

}
