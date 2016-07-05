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

import static org.apache.cxf.dosgi.common.util.OsgiUtils.getMultiValueProperty;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_CONFIGS_SUPPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_INTENTS_SUPPORTED;

import java.net.URL;
import java.util.Collection;
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
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(property = //
{//
 REMOTE_CONFIGS_SUPPORTED + "=" + RsConstants.RS_CONFIG_TYPE,
 REMOTE_INTENTS_SUPPORTED + "="
})
public class RsProvider implements DistributionProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RsProvider.class);
    private IntentManager intentManager;
    private HttpServiceManager httpServiceManager;
    
    @Reference
    public void setHttpServiceManager(HttpServiceManager httpServiceManager) {
        this.httpServiceManager = httpServiceManager;
    }
    
    @Reference
    public void setIntentManager(IntentManager intentManager) {
        this.intentManager = intentManager;
    }
    
    public String[] getSupportedTypes() {
        return new String[] {RsConstants.RS_CONFIG_TYPE};
    }

    @SuppressWarnings("rawtypes")
    public Object importEndpoint(ClassLoader consumerLoader,
                                 BundleContext consumerContext,
                                 Class[] interfaces,
                                 EndpointDescription endpoint) {
        intentManager.assertAllIntentsSupported(endpoint.getProperties());
        Class<?> iClass = interfaces[0];
        String address = OsgiUtils.getProperty(endpoint, RsConstants.RS_ADDRESS_PROPERTY);
        if (address == null) {
            LOG.warn("Remote address is unavailable");
            return null;
        }
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JAXRSClientFactoryBean.class.getClassLoader());
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
        addContextProperties(bean, endpoint.getProperties(), RsConstants.RS_CONTEXT_PROPS_PROP_KEY);
        bean.setServiceClass(iClass);
        intentManager.applyIntents(bean.getFeatures(), bean, endpoint.getProperties());
        return ProxyFactory.create(bean.create(), iClass);
    }

    @SuppressWarnings("rawtypes")
    public Endpoint exportService(Object serviceBean,
                                  BundleContext callingContext,
                                  Map<String, Object> endpointProps,
                                  Class[] exportedInterfaces) throws IntentUnsatisfiedException {
        if (!configTypeSupported(endpointProps, RsConstants.RS_CONFIG_TYPE)) {
            return null;
        }
        String contextRoot = OsgiUtils.getProperty(endpointProps, RsConstants.RS_HTTP_SERVICE_CONTEXT);
        String address;
        Class<?> iClass = exportedInterfaces[0];
        if (contextRoot == null) {
            address = getServerAddress(endpointProps, iClass);
        } else {
            address = OsgiUtils.getProperty(endpointProps, RsConstants.RS_ADDRESS_PROPERTY);
            if (address == null) {
                address = "/";
            }
        }
        final Long sid = (Long) endpointProps.get(RemoteConstants.ENDPOINT_SERVICE_ID);
        intentManager.assertAllIntentsSupported(endpointProps);
        Bus bus = BusFactory.newInstance().createBus();
        if (contextRoot != null) {
            httpServiceManager.registerServlet(bus, contextRoot, callingContext, sid);
        }
        LOG.info("Creating JAXRS endpoint for " + iClass.getName() + " with address " + address);

        JAXRSServerFactoryBean factory = createServerFactory(callingContext, endpointProps, 
                                                             iClass, serviceBean, address, bus);
        String[] intents = intentManager.applyIntents(factory.getFeatures(), factory, endpointProps);
        String completeEndpointAddress = httpServiceManager.getAbsoluteAddress(contextRoot, address);
        EndpointDescription epd = createEndpointDesc(endpointProps, new String[] {RsConstants.RS_CONFIG_TYPE},
                completeEndpointAddress, intents);
        return createServerFromFactory(factory, epd);
    }
    
    private boolean configTypeSupported(Map<String, Object> endpointProps, String configType) {
        Collection<String> configs = getMultiValueProperty(endpointProps.get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
        return configs == null || configs.isEmpty() || configs.contains(configType);
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
        factory.setServiceClass(iClass);
        factory.setResourceProvider(iClass, new SingletonResourceProvider(serviceBean));
        factory.setAddress(address);
        addContextProperties(factory, sd, RsConstants.RS_CONTEXT_PROPS_PROP_KEY);
        String location = OsgiUtils.getProperty(sd, RsConstants.RS_WADL_LOCATION);
        setWadlLocation(callingContext, factory, location);
        return factory;
    }

    private void setWadlLocation(BundleContext callingContext, JAXRSServerFactoryBean factory,
                                 String location) {
        if (location != null) {
            URL wadlURL = callingContext.getBundle().getResource(location);
            if (wadlURL != null) {
                factory.setDocLocation(wadlURL.toString());
            }
        }
    }

    protected EndpointDescription createEndpointDesc(Map<String, Object> props, 
                                                     String[] importedConfigs,
                                                     String address,
                                                     String[] intents) {
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, importedConfigs);
        props.put(RsConstants.RS_ADDRESS_PROPERTY, address);
        props.put(RemoteConstants.SERVICE_INTENTS, intents);
        props.put(RemoteConstants.ENDPOINT_ID, address);
        return new EndpointDescription(props);
    }
    
    protected String getServerAddress(Map<String, Object> sd, Class<?> iClass) {
        String address = OsgiUtils.getProperty(sd, RsConstants.RS_ADDRESS_PROPERTY);
        return address == null ? httpServiceManager.getDefaultAddress(iClass) : address;
    }
    
    private static void addContextProperties(AbstractEndpointFactory factory, Map<String, Object> sd, String propName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>)sd.get(propName);
        if (props != null) {
            factory.getProperties(true).putAll(props);
        }
    }
}
