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

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ProxyClassLoader;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.api.Endpoint;
import org.apache.cxf.dosgi.dsw.api.IntentUnsatisfiedException;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.UserResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxRSPojoConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {

    private static final Logger LOG = LoggerFactory.getLogger(JaxRSPojoConfigurationTypeHandler.class);

    public JaxRSPojoConfigurationTypeHandler(BundleContext dswBC,
                                             IntentManager intentManager,
                                             HttpServiceManager httpServiceManager) {
        super(dswBC, intentManager, httpServiceManager);
    }

    public String[] getSupportedTypes() {
        return new String[] {Constants.RS_CONFIG_TYPE};
    }

    @SuppressWarnings("rawtypes")
    public Object importEndpoint(ClassLoader consumerLoader,
                                 BundleContext consumerContext,
                                 Class[] interfaces,
                                 EndpointDescription endpoint) {
        Class<?> iClass = interfaces[0];
        String address = getPojoAddress(endpoint, iClass);
        if (address == null) {
            LOG.warn("Remote address is unavailable");
            return null;
        }
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            return createJaxrsProxy(address, consumerContext, iClass, null, endpoint);
        } catch (Throwable e) {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

        try {
            ProxyClassLoader cl = new ProxyClassLoader(iClass.getClassLoader());
            cl.addLoader(Client.class.getClassLoader());
            return createJaxrsProxy(address, consumerContext, iClass, cl, endpoint);
        } catch (Throwable e) {
            LOG.warn("proxy creation failed", e);
        }

        return null;
    }

    protected Object createJaxrsProxy(String address,
                                      BundleContext callingContext,
                                      Class<?> iClass,
                                      ClassLoader loader,
                                      EndpointDescription endpoint) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        if (loader != null) {
            bean.setClassLoader(loader);
        }

        addRsInterceptorsFeaturesProps(bean, callingContext, endpoint.getProperties());

        List<UserResource> resources = JaxRSUtils.getModel(callingContext, iClass);
        if (resources != null) {
            bean.setModelBeansWithServiceClass(resources, iClass);
        } else {
            bean.setServiceClass(iClass);
        }
        List<Object> providers = JaxRSUtils.getProviders(callingContext, endpoint.getProperties());
        if (providers != null && !providers.isEmpty()) {
            bean.setProviders(providers);
        }
        Thread.currentThread().setContextClassLoader(JAXRSClientFactoryBean.class.getClassLoader());
        return getProxy(bean.create(), iClass);
    }

    @SuppressWarnings("rawtypes")
    public Endpoint exportService(Object serviceBean,
                                  BundleContext callingContext,
                                  Map<String, Object> endpointProps,
                                  Class[] exportedInterfaces) throws IntentUnsatisfiedException {
        String contextRoot = getServletContextRoot(endpointProps);
        String address;
        Class<?> iClass = exportedInterfaces[0];
        if (contextRoot == null) {
            address = getServerAddress(endpointProps, iClass);
        } else {
            address = getClientAddress(endpointProps);
            if (address == null) {
                address = "/";
            }
        }
        final Long sid = (Long) endpointProps.get(RemoteConstants.ENDPOINT_SERVICE_ID);
        Bus bus = createBus(sid, callingContext, contextRoot);

        LOG.info("Creating a " + iClass.getName()
                 + " endpoint via JaxRSPojoConfigurationTypeHandler, address is " + address);

        JAXRSServerFactoryBean factory = createServerFactory(callingContext, endpointProps, 
                                                             iClass, serviceBean, address, bus);
        String completeEndpointAddress = httpServiceManager.getAbsoluteAddress(contextRoot, address);

        EndpointDescription epd = createEndpointDesc(endpointProps, new String[] {Constants.RS_CONFIG_TYPE},
                completeEndpointAddress, new String[] {"HTTP"});

        return createServerFromFactory(factory, epd);
    }

    private Endpoint createServerFromFactory(JAXRSServerFactoryBean factory,
                                                       EndpointDescription epd) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JAXRSServerFactoryBean.class.getClassLoader());
            Server server = factory.create();
            return new ServerWrapper(epd, server);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private JAXRSServerFactoryBean createServerFactory(BundleContext callingContext,
                                                       Map<String, Object> sd,
                                                       Class<?> iClass,
                                                       Object serviceBean,
                                                       String address,
                                                       Bus bus) {
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setBus(bus);
        List<UserResource> resources = JaxRSUtils.getModel(callingContext, iClass);
        if (resources != null) {
            factory.setModelBeansWithServiceClass(resources, iClass);
            factory.setServiceBeanObjects(serviceBean);
        } else {
            factory.setServiceClass(iClass);
            factory.setResourceProvider(iClass, new SingletonResourceProvider(serviceBean));
        }
        factory.setAddress(address);
        List<Object> providers = JaxRSUtils.getProviders(callingContext, sd);
        if (providers != null && !providers.isEmpty()) {
            factory.setProviders(providers);
        }
        addRsInterceptorsFeaturesProps(factory, callingContext, sd);
        String location = OsgiUtils.getProperty(sd, Constants.RS_WADL_LOCATION);
        if (location != null) {
            URL wadlURL = callingContext.getBundle().getResource(location);
            if (wadlURL != null) {
                factory.setDocLocation(wadlURL.toString());
            }
        }
        return factory;
    }

    protected String getPojoAddress(EndpointDescription endpoint, Class<?> iClass) {
        String address = OsgiUtils.getProperty(endpoint, Constants.RS_ADDRESS_PROPERTY);

        if (address == null) {
            address = httpServiceManager.getDefaultAddress(iClass);
            if (address != null) {
                LOG.info("Using a default address: " + address);
            }
        }
        return address;
    }
}
