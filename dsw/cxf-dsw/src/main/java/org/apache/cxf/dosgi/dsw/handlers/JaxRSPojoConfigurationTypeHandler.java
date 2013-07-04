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
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentUnsatisfiedException;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.ProxyClassLoader;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.UserResource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
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

    public Object createProxy(ServiceReference serviceReference, BundleContext dswContext,
                              BundleContext callingContext, Class<?> iClass,
                              EndpointDescription endpoint) throws IntentUnsatisfiedException {
        String address = getPojoAddress(endpoint, iClass);
        if (address == null) {
            LOG.warn("Remote address is unavailable");
            return null;
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            return createJaxrsProxy(address, callingContext, dswContext, iClass, null, endpoint);
        } catch (Throwable e) {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

        try {
            ProxyClassLoader cl = new ProxyClassLoader();
            cl.addLoader(iClass.getClassLoader());
            cl.addLoader(Client.class.getClassLoader());
            return createJaxrsProxy(address, callingContext, dswContext, iClass, cl, endpoint);
        } catch (Throwable e) {
            LOG.warn("proxy creation failed", e);
        }

        return null;
    }

    protected Object createJaxrsProxy(String address,
                                      BundleContext dswContext,
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
        List<Object> providers = JaxRSUtils.getProviders(callingContext, dswContext, endpoint.getProperties());
        if (providers != null && !providers.isEmpty()) {
            bean.setProviders(providers);
        }
        Thread.currentThread().setContextClassLoader(JAXRSClientFactoryBean.class.getClassLoader());
        return getProxy(bean.create(), iClass);
    }

    public ExportResult createServer(ServiceReference sref,
                                     BundleContext dswContext,
                                     BundleContext callingContext,
                                     Map<String, Object> sd, Class<?> iClass,
                                     Object serviceBean) throws IntentUnsatisfiedException {
        String contextRoot = httpServiceManager.getServletContextRoot(sd, iClass);
        String address;
        if (contextRoot == null) {
            address = getServerAddress(sd, iClass);
        } else {
            address = getClientAddress(sd, iClass);
            if (address == null) {
                address = "/";
            }
        }

        Bus bus = contextRoot != null
                ? httpServiceManager.registerServletAndGetBus(contextRoot, callingContext, sref) : null;

        LOG.info("Creating a " + iClass.getName()
                 + " endpoint via JaxRSPojoConfigurationTypeHandler, address is " + address);

        JAXRSServerFactoryBean factory = createServerFactory(dswContext, callingContext, sd,
                                                             iClass, serviceBean, address, bus);
        String completeEndpointAddress = httpServiceManager.getAbsoluteAddress(dswContext, contextRoot, address);

        // The properties for the EndpointDescription
        Map<String, Object> endpointProps = createEndpointProps(sd, iClass, new String[] {Constants.RS_CONFIG_TYPE},
                completeEndpointAddress, new String[] {"HTTP"});

        return createServerFromFactory(factory, endpointProps);
    }

    private ExportResult createServerFromFactory(JAXRSServerFactoryBean factory,
                                                       Map<String, Object> endpointProps) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JAXRSServerFactoryBean.class.getClassLoader());
            Server server = factory.create();
            return new ExportResult(endpointProps, server);
        } catch (Exception e) {
            return new ExportResult(endpointProps, e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private JAXRSServerFactoryBean createServerFactory(BundleContext dswContext,
                                                       BundleContext callingContext,
                                                       Map<String, Object> sd,
                                                       Class<?> iClass,
                                                       Object serviceBean,
                                                       String address,
                                                       Bus bus) {
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        if (bus != null) {
            factory.setBus(bus);
        }
        List<UserResource> resources = JaxRSUtils.getModel(callingContext, iClass);
        if (resources != null) {
            factory.setModelBeansWithServiceClass(resources, iClass);
            factory.setServiceBeanObjects(serviceBean);
        } else {
            factory.setServiceClass(iClass);
            factory.setResourceProvider(iClass, new SingletonResourceProvider(serviceBean));
        }
        factory.setAddress(address);
        List<Object> providers = JaxRSUtils.getProviders(callingContext, dswContext, sd);
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
