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

import javax.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentUnsatifiedException;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PojoConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PojoConfigurationTypeHandler.class);

    public PojoConfigurationTypeHandler(BundleContext dswBC,
                                        IntentManager intentManager,
                                        HttpServiceManager httpServiceManager) {
        super(dswBC, intentManager, httpServiceManager);
    }

    public String[] getSupportedTypes() {
        return new String[] {Constants.WS_CONFIG_TYPE, Constants.WS_CONFIG_TYPE_OLD};
    }

    public Object createProxy(ServiceReference sref, BundleContext dswContext,
            BundleContext callingContext, Class<?> iClass, EndpointDescription epd) throws IntentUnsatifiedException {
        Map<String, Object> sd = epd.getProperties();
        String address = getClientAddress(sd, iClass);
        if (address == null) {
            LOG.warn("Remote address is unavailable");
            // TODO: fire Event
            return null;
        }

        LOG.info("Creating a " + iClass.getName() + " client, endpoint address is " + address);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClientProxyFactoryBean factory = createClientProxyFactoryBean(sd, iClass);
            factory.getServiceFactory().setDataBinding(getDataBinding(sd, iClass));
            factory.setServiceClass(iClass);
            factory.setAddress(address);
            addWsInterceptorsFeaturesProps(factory.getClientFactoryBean(), callingContext, sd);
            setClientWsdlProperties(factory.getClientFactoryBean(), dswContext, sd, false);

            intentManager.applyIntents(factory.getFeatures(), factory.getClientFactoryBean(), sd);

            Thread.currentThread().setContextClassLoader(ClientProxyFactoryBean.class.getClassLoader());
            return getProxy(factory.create(), iClass);
        } catch (Exception e) {
            LOG.warn("proxy creation failed", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        return null;
    }

    public ExportResult createServer(ServiceReference sref,
                                     BundleContext dswContext,
                                     BundleContext callingContext,
                                     Map<String, Object> sd,
                                     Class<?> iClass,
                                     Object serviceBean) throws IntentUnsatifiedException {
        try {
            String address = getPojoAddress(sd, iClass);
            String contextRoot = httpServiceManager.getServletContextRoot(sd, iClass);

            ServerFactoryBean factory = createServerFactoryBean(sd, iClass);
            factory.setDataBinding(getDataBinding(sd, iClass));
            if (contextRoot != null) {
                Bus bus = httpServiceManager.registerServletAndGetBus(contextRoot, callingContext, sref);
                factory.setBus(bus);
            }
            factory.setServiceClass(iClass);
            factory.setAddress(address);
            factory.setServiceBean(serviceBean);
            addWsInterceptorsFeaturesProps(factory, callingContext, sd);
            setWsdlProperties(factory, callingContext, sd, false);
            String[] intents = intentManager.applyIntents(factory.getFeatures(), factory, sd);

            String completeEndpointAddress = httpServiceManager.getAbsoluteAddress(dswContext, contextRoot, address);

            // The properties for the EndpointDescription
            Map<String, Object> endpointProps = createEndpointProps(sd, iClass,
                                                                    new String[]{Constants.WS_CONFIG_TYPE},
                                                                    completeEndpointAddress, intents);

            return createServerFromFactory(factory, endpointProps);
        } catch (RuntimeException re) {
            return new ExportResult(sd, re);
        }
    }

    private String getPojoAddress(Map<String, Object> sd, Class<?> iClass) {
        String address = getClientAddress(sd, iClass);
        if (address != null)
            return address;

        // If the property is not of type string this will cause an ClassCastException which
        // will be propagated to the ExportRegistration exception property.
        String port = (String) sd.get(Constants.WS_PORT_PROPERTY);
        if (port == null)
            port = "9000";

        address = "http://localhost:" + port + "/" + iClass.getName().replace('.', '/');
        LOG.info("Using a default address : " + address);
        return address;
    }

    private DataBinding getDataBinding(Map<String, Object> sd, Class<?> iClass) {
        return isJAXB(sd, iClass) ? new JAXBDataBinding() : new AegisDatabinding();
    }

    private boolean isJAXB(Map<String, Object> sd, Class<?> iClass) {
        String dataBindingName = (String)sd.get(Constants.WS_DATABINDING_PROP_KEY);
        return (iClass.getAnnotation(WebService.class) != null
            || Constants.WS_DATA_BINDING_JAXB.equals(dataBindingName))
            && !Constants.WS_DATA_BINDING_AEGIS.equals(dataBindingName);
    }

    // Isolated so that it can be substituted for testing
    protected ClientProxyFactoryBean createClientProxyFactoryBean(Map<String, Object> sd, Class<?> iClass) {
        return isJAXWS(sd, iClass) ? new JaxWsProxyFactoryBean() : new ClientProxyFactoryBean();
    }

    // Isolated so that it can be substituted for testing
    protected ServerFactoryBean createServerFactoryBean(Map<String, Object> sd, Class<?> iClass) {
        return isJAXWS(sd, iClass) ? new JaxWsServerFactoryBean() : new ServerFactoryBean();
    }

    private boolean isJAXWS(Map<String, Object> sd, Class<?> iClass) {
        String frontEnd = (String)sd.get(Constants.WS_FRONTEND_PROP_KEY);
        return (iClass.getAnnotation(WebService.class) != null
            || Constants.WS_FRONTEND_JAXWS.equals(frontEnd))
            && !Constants.WS_FRONTEND_SIMPLE.equals(frontEnd);
    }

}
