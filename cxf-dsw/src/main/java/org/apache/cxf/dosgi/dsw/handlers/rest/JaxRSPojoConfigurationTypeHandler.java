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
package org.apache.cxf.dosgi.dsw.handlers.rest;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.aries.rsa.spi.DistributionProvider;
import org.apache.aries.rsa.spi.Endpoint;
import org.apache.aries.rsa.spi.IntentUnsatisfiedException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ProxyClassLoader;
import org.apache.cxf.dosgi.common.httpservice.HttpServiceManager;
import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.dosgi.common.proxy.ProxyFactory;
import org.apache.cxf.dosgi.common.util.OsgiUtils;
import org.apache.cxf.dosgi.common.util.ServerWrapper;
import org.apache.cxf.dosgi.dsw.handlers.pojo.InterceptorSupport;
import org.apache.cxf.dosgi.dsw.osgi.Constants;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
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

public class JaxRSPojoConfigurationTypeHandler implements DistributionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(JaxRSPojoConfigurationTypeHandler.class);
    protected BundleContext bundleContext;
    protected IntentManager intentManager;
    protected HttpServiceManager httpServiceManager;

    public JaxRSPojoConfigurationTypeHandler(BundleContext dswBC, IntentManager intentManager,
                                             HttpServiceManager httpServiceManager) {
        this.bundleContext = dswBC;
        this.intentManager = intentManager;
        this.httpServiceManager = httpServiceManager;
    }

    public String[] getSupportedTypes() {
        return new String[] {Constants.RS_CONFIG_TYPE};
    }
    
    protected EndpointDescription createEndpointDesc(Map<String, Object> props, 
                                                     String[] importedConfigs,
                                                     String address, 
                                                     String[] intents) {
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, importedConfigs);
        for (String configurationType : importedConfigs) {
            if (Constants.RS_CONFIG_TYPE.equals(configurationType)) {
                props.put(Constants.RS_ADDRESS_PROPERTY, address);
            }
        }
        props.put(RemoteConstants.SERVICE_INTENTS, intents);
        props.put(RemoteConstants.ENDPOINT_ID, address);
        return new EndpointDescription(props);
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
        return ProxyFactory.create(bean.create(), iClass);
    }

    @SuppressWarnings("rawtypes")
    public Endpoint exportService(Object serviceBean,
                                  BundleContext callingContext,
                                  Map<String, Object> endpointProps,
                                  Class[] exportedInterfaces) throws IntentUnsatisfiedException {
        String contextRoot = OsgiUtils.getProperty(endpointProps, Constants.RS_HTTP_SERVICE_CONTEXT);
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
    
    protected String getClientAddress(Map<String, Object> sd) {
        return OsgiUtils.getFirstNonEmptyStringProperty(sd, RemoteConstants.ENDPOINT_ID,
                                                            Constants.RS_ADDRESS_PROPERTY);
    }

    protected String getServerAddress(Map<String, Object> sd, Class<?> iClass) {
        String address = OsgiUtils.getProperty(sd, Constants.RS_ADDRESS_PROPERTY);
        return address == null ? httpServiceManager.getDefaultAddress(iClass) : address;
    }
    
    protected Bus createBus(Long sid, BundleContext callingContext, String contextRoot) {
        Bus bus = BusFactory.newInstance().createBus();
        if (contextRoot != null) {
            httpServiceManager.registerServlet(bus, contextRoot, callingContext, sid);
        }
        return bus;
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
    
    protected void addRsInterceptorsFeaturesProps(AbstractEndpointFactory factory,
                                                         BundleContext callingContext,
                                                         Map<String, Object> sd) {
        InterceptorSupport.addInterceptors(factory, callingContext, sd, Constants.RS_IN_INTERCEPTORS_PROP_KEY);
        InterceptorSupport.addInterceptors(factory, callingContext, sd, Constants.RS_OUT_INTERCEPTORS_PROP_KEY);
        InterceptorSupport.addInterceptors(factory, callingContext, sd, Constants.RS_OUT_FAULT_INTERCEPTORS_PROP_KEY);
        InterceptorSupport.addInterceptors(factory, callingContext, sd, Constants.RS_IN_FAULT_INTERCEPTORS_PROP_KEY);
        InterceptorSupport.addFeatures(factory, callingContext, sd, Constants.RS_FEATURES_PROP_KEY);
        addContextProperties(factory, sd, Constants.RS_CONTEXT_PROPS_PROP_KEY);
    }
    
    private void addContextProperties(AbstractEndpointFactory factory, Map<String, Object> sd, String propName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>)sd.get(propName);
        if (props != null) {
            factory.getProperties(true).putAll(props);
        }
    }
}
