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

import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_CONFIGS_SUPPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_INTENTS_SUPPORTED;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.aries.rsa.spi.DistributionProvider;
import org.apache.aries.rsa.spi.Endpoint;
import org.apache.aries.rsa.spi.IntentUnsatisfiedException;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.common.endpoint.ServerEndpoint;
import org.apache.cxf.dosgi.common.handlers.BaseDistributionProvider;
import org.apache.cxf.dosgi.common.httpservice.HttpServiceManager;
import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.dosgi.common.proxy.ProxyFactory;
import org.apache.cxf.dosgi.common.util.PropertyHelper;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.AbstractJAXRSFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.ext.ContextProvider;
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
public class RsProvider extends BaseDistributionProvider implements DistributionProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RsProvider.class);

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
        if (interfaces.length > 1) {
            throw new IllegalArgumentException("Multiple interfaces are not supported by this provider");
        }
        Set<String> intentNames = intentManager.getImported(endpoint.getProperties());
        List<Object> intents = intentManager.getRequiredIntents(intentNames);
        Class<?> iClass = interfaces[0];
        String address = PropertyHelper.getProperty(endpoint.getProperties(), RsConstants.RS_ADDRESS_PROPERTY);
        if (address == null) {
            LOG.warn("Remote address is unavailable");
            return null;
        }
        return createJaxrsProxy(address, iClass, null, endpoint, intents);
    }

    private Object createJaxrsProxy(String address,
                                      Class<?> iClass,
                                      ClassLoader loader,
                                      EndpointDescription endpoint, 
                                      List<Object> intents) {
        JAXRSClientFactoryBean factory = new JAXRSClientFactoryBean();
        factory.setAddress(address);
        if (loader != null) {
            factory.setClassLoader(loader);
        }
        addContextProperties(factory, endpoint.getProperties(), RsConstants.RS_CONTEXT_PROPS_PROP_KEY);
        factory.setServiceClass(iClass);
        applyIntents(intents, factory);
        return ProxyFactory.create(factory.create(), iClass);
    }

    @SuppressWarnings("rawtypes")
    public Endpoint exportService(Object serviceBean,
                                  BundleContext callingContext,
                                  Map<String, Object> endpointProps,
                                  Class[] exportedInterfaces) throws IntentUnsatisfiedException {
        if (!configTypeSupported(endpointProps, RsConstants.RS_CONFIG_TYPE)) {
            return null;
        }
        String contextRoot = PropertyHelper.getProperty(endpointProps, RsConstants.RS_HTTP_SERVICE_CONTEXT);
        Class<?> iClass = exportedInterfaces[0];
        String address = PropertyHelper.getProperty(endpointProps, RsConstants.RS_ADDRESS_PROPERTY);
        if (address == null) {
            address = httpServiceManager.getDefaultAddress(iClass);
        }
        final Long sid = (Long) endpointProps.get(RemoteConstants.ENDPOINT_SERVICE_ID);
        Set<String> intentNames = intentManager.getExported(endpointProps);
        List<Object> intents = intentManager.getRequiredIntents(intentNames);
        intents.addAll(intentManager.getIntentsFromService(serviceBean));

        Bus bus = createBus(sid, callingContext, contextRoot, endpointProps);
        LOG.info("Creating JAXRS endpoint for " + iClass.getName() + " with address " + address);

        JAXRSServerFactoryBean factory = createServerFactory(callingContext, endpointProps, 
                                                             iClass, serviceBean, address, bus);
        applyIntents(intents, factory);
        String completeEndpointAddress = httpServiceManager.getAbsoluteAddress(contextRoot, address);
        EndpointDescription epd = createEndpointDesc(endpointProps, //
                                                     new String[] {RsConstants.RS_CONFIG_TYPE},
                                                     completeEndpointAddress,
                                                     intentNames);
        return createServerFromFactory(factory, epd);
    }

    private void applyIntents(List<Object> intents, AbstractJAXRSFactoryBean factory) {
        List<Feature> features = intentManager.getIntents(Feature.class, intents);
        factory.setFeatures(features);
        DataBinding dataBinding = intentManager.getIntent(DataBinding.class, intents);
        if (dataBinding != null) {
            factory.setDataBinding(dataBinding);
        }
        BindingConfiguration binding = intentManager.getIntent(BindingConfiguration.class, intents);
        if (binding != null) {
            factory.setBindingConfig(binding);
        }
        
        List<Object> providers = new ArrayList<Object>();
        for (Object intent : intents) {
            if (isProvider(intent)) {
                providers.add(intent);
            }
        }
        factory.setProviders(providers);
    }
    
    private boolean isProvider(Object intent) {
        return intent.getClass().getAnnotation(Provider.class) != null //
            || (intent instanceof ExceptionMapper) // 
            || (intent instanceof MessageBodyReader) //
            || (intent instanceof MessageBodyWriter) //
            || (intent instanceof ContextResolver) //
            || (intent instanceof ContextProvider); 
    }

    private Endpoint createServerFromFactory(JAXRSServerFactoryBean factory,
                                             EndpointDescription epd) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JAXRSServerFactoryBean.class.getClassLoader());
            Server server = factory.create();
            return new ServerEndpoint(epd, server);
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
        String location = PropertyHelper.getProperty(sd, RsConstants.RS_WADL_LOCATION);
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

    protected EndpointDescription createEndpointDesc(Map<String, Object> props, String[] importedConfigs,
                                                     String address, Collection<String> intents) {
        return super.createEndpointDesc(props, importedConfigs, RsConstants.RS_ADDRESS_PROPERTY, address, intents);
    }
}
