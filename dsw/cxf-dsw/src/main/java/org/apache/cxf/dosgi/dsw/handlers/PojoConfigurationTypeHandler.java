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
import org.apache.cxf.dosgi.dsw.api.Endpoint;
import org.apache.cxf.dosgi.dsw.api.IntentUnsatisfiedException;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
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

    @SuppressWarnings("rawtypes")
    public Object importEndpoint(ClassLoader consumerLoader,
                                 BundleContext consumerContext,
                                 Class[] interfaces,
                                 EndpointDescription endpoint) throws IntentUnsatisfiedException {
        Class<?> iClass = interfaces[0];
        Map<String, Object> sd = endpoint.getProperties();
        String address = getClientAddress(sd);
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
            addWsInterceptorsFeaturesProps(factory.getClientFactoryBean(), consumerContext, sd);
            setClientWsdlProperties(factory.getClientFactoryBean(), bundleContext, sd, false);

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

    @SuppressWarnings("rawtypes")
    public Endpoint exportService(Object serviceO,
                                  BundleContext serviceContext,
                                  Map<String, Object> endpointProps,
                                  Class[] exportedInterfaces) throws IntentUnsatisfiedException {
        Class<?> iClass = exportedInterfaces[0];
        String address = getPojoAddress(endpointProps, iClass);
        ServerFactoryBean factory = createServerFactoryBean(endpointProps, iClass);
        factory.setDataBinding(getDataBinding(endpointProps, iClass));
        String contextRoot = getServletContextRoot(endpointProps);

        final Long sid = (Long) endpointProps.get(RemoteConstants.ENDPOINT_SERVICE_ID);
        Bus bus = createBus(sid, serviceContext, contextRoot);
        factory.setBus(bus);
        factory.setServiceClass(iClass);
        factory.setAddress(address);
        
        factory.setServiceBean(serviceO);
        addWsInterceptorsFeaturesProps(factory, serviceContext, endpointProps);
        setWsdlProperties(factory, serviceContext, endpointProps, false);
        String[] intents = intentManager.applyIntents(factory.getFeatures(), factory, endpointProps);

        String completeEndpointAddress = httpServiceManager.getAbsoluteAddress(contextRoot, address);
        EndpointDescription epd = createEndpointDesc(endpointProps,
                                                     new String[]{Constants.WS_CONFIG_TYPE},
                                                     completeEndpointAddress, intents);
        return createServerFromFactory(factory, epd);
    }

    private String getPojoAddress(Map<String, Object> sd, Class<?> iClass) {
        String address = getClientAddress(sd);
        if (address != null) {
            return address;
        }

        // If the property is not of type string this will cause an ClassCastException which
        // will be propagated to the ExportRegistration exception property.
        Object port = sd.get(Constants.WS_PORT_PROPERTY);
        if (port == null) {
            port = "9000";
        }

        address = "http://localhost:" + port + "/" + iClass.getName().replace('.', '/');
        LOG.info("Using a default address: " + address);
        return address;
    }

    private DataBinding getDataBinding(Map<String, Object> sd, Class<?> iClass) {
        Object dataBindingBeanProp = sd.get(Constants.WS_DATABINDING_BEAN_PROP_KEY);
        if (dataBindingBeanProp instanceof DataBinding) {
            return (DataBinding)dataBindingBeanProp;
        } 
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
