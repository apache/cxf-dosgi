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
package org.apache.cxf.dosgi.dsw.handlers.ws;

import static org.apache.cxf.dosgi.common.util.OsgiUtils.getMultiValueProperty;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_CONFIGS_SUPPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_INTENTS_SUPPORTED;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jws.WebService;

import org.apache.aries.rsa.spi.DistributionProvider;
import org.apache.aries.rsa.spi.Endpoint;
import org.apache.aries.rsa.spi.IntentUnsatisfiedException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.common.httpservice.HttpServiceManager;
import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.dosgi.common.proxy.ProxyFactory;
import org.apache.cxf.dosgi.common.util.OsgiUtils;
import org.apache.cxf.dosgi.common.util.ServerEndpoint;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "cxf-dsw", property = //
{//
 REMOTE_CONFIGS_SUPPORTED + "=" + WsConstants.WS_CONFIG_TYPE,
 REMOTE_INTENTS_SUPPORTED + "=" 
})
public class WsProvider implements DistributionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(WsProvider.class);
    protected BundleContext bundleContext;
    protected IntentManager intentManager;
    protected HttpServiceManager httpServiceManager;
    
    @Reference
    public void setHttpServiceManager(HttpServiceManager httpServiceManager) {
        this.httpServiceManager = httpServiceManager;
    }
    
    @Reference
    public void setIntentManager(IntentManager intentManager) {
        this.intentManager = intentManager;
    }
    
    @Activate
    public void activate(BundleContext context) {
        this.bundleContext = context;
    }

    public String[] getSupportedTypes() {
        return new String[] {WsConstants.WS_CONFIG_TYPE};
    }

    @SuppressWarnings("rawtypes")
    public Object importEndpoint(ClassLoader consumerLoader,
                                 BundleContext consumerContext,
                                 Class[] interfaces,
                                 EndpointDescription endpoint) throws IntentUnsatisfiedException {
        Class<?> iClass = interfaces[0];
        Map<String, Object> sd = endpoint.getProperties();
        String address = getClientAddress(sd);
        LOG.info("Creating a " + iClass.getName() + " client, endpoint address is " + address);

        try {
            ClientProxyFactoryBean factory = createClientProxyFactoryBean(sd, iClass);
            factory.setDataBinding(getDataBinding(sd, iClass));
            factory.setBindingConfig(new SoapBindingConfiguration());
            factory.setServiceClass(iClass);
            factory.setAddress(address);
            addContextProperties(factory.getClientFactoryBean(), sd, WsConstants.WS_CONTEXT_PROPS_PROP_KEY);
            WsdlSupport.setWsdlProperties(factory.getClientFactoryBean(), bundleContext, sd);
            applyIntents(sd, factory);
            return ProxyFactory.create(factory.create(), iClass);
        } catch (Exception e) {
            throw new RuntimeException("proxy creation failed", e);
        }
    }

    private void applyIntents(Map<String, Object> sd, ClientProxyFactoryBean factory) {
        Set<String> intentNames = intentManager.getImported(sd);
        List<Object> intents = intentManager.getIntents(intentNames);
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
    }

    @SuppressWarnings("rawtypes")
    public Endpoint exportService(Object serviceO,
                                  BundleContext serviceContext,
                                  Map<String, Object> endpointProps,
                                  Class[] exportedInterfaces) throws IntentUnsatisfiedException {
        if (!configTypeSupported(endpointProps, WsConstants.WS_CONFIG_TYPE)) {
            return null;
        }
        Class<?> iClass = exportedInterfaces[0];
        String address = getPojoAddress(endpointProps, iClass);
        ServerFactoryBean factory = createServerFactoryBean(endpointProps, iClass);
        String contextRoot = OsgiUtils.getProperty(endpointProps, WsConstants.WS_HTTP_SERVICE_CONTEXT);

        final Long sid = (Long) endpointProps.get(RemoteConstants.ENDPOINT_SERVICE_ID);
        Set<String> intents = intentManager.getExported(endpointProps);
        intentManager.assertAllIntentsSupported(intents);
        Bus bus = createBus(sid, serviceContext, contextRoot);
        factory.setDataBinding(getDataBinding(endpointProps, iClass));
        factory.setBindingConfig(new SoapBindingConfiguration());
        factory.setBus(bus);
        factory.setServiceClass(iClass);
        factory.setServiceBean(serviceO);
        factory.setAddress(address);
        addContextProperties(factory, endpointProps, WsConstants.WS_CONTEXT_PROPS_PROP_KEY);
        WsdlSupport.setWsdlProperties(factory, serviceContext, endpointProps);
        applyIntents(endpointProps, factory);
        intentManager.applyIntents(factory, intents);

        String completeEndpointAddress = httpServiceManager.getAbsoluteAddress(contextRoot, address);
        try {
            EndpointDescription epd = createEndpointDesc(endpointProps,
                                                         new String[]{WsConstants.WS_CONFIG_TYPE},
                                                         completeEndpointAddress, intents);
            return createServerFromFactory(factory, epd);
        } catch (Exception e) {
            throw new RuntimeException("Error exporting service with adress " + completeEndpointAddress, e);
        }
    }
    
    private void applyIntents(Map<String, Object> sd, AbstractEndpointFactory factory) {
        Set<String> intentNames = intentManager.getExported(sd);
        List<Object> intents = intentManager.getIntents(intentNames);
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
    }

    private boolean configTypeSupported(Map<String, Object> endpointProps, String configType) {
        Collection<String> configs = getMultiValueProperty(endpointProps.get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
        return configs == null || configs.isEmpty() || configs.contains(configType);
    }
    
    protected EndpointDescription createEndpointDesc(Map<String, Object> props, 
                                                     String[] importedConfigs,
                                                     String address, 
                                                     Collection<String> intents) {
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, importedConfigs);
        props.put(WsConstants.WS_ADDRESS_PROPERTY, address);
        props.put(RemoteConstants.SERVICE_INTENTS, intents);
        props.put(RemoteConstants.ENDPOINT_ID, address);
        return new EndpointDescription(props);
    }

    private String getPojoAddress(Map<String, Object> sd, Class<?> iClass) {
        String address = getClientAddress(sd);
        if (address != null) {
            return address;
        }

        // If the property is not of type string this will cause an ClassCastException which
        // will be propagated to the ExportRegistration exception property.
        Object port = sd.get(WsConstants.WS_PORT_PROPERTY);
        if (port == null) {
            port = "9000";
        }

        address = "http://localhost:" + port + "/" + iClass.getName().replace('.', '/');
        LOG.info("Using a default address: " + address);
        return address;
    }
    
    protected String getClientAddress(Map<String, Object> sd) {
        return OsgiUtils.getFirstNonEmptyStringProperty(sd, WsConstants.WS_ADDRESS_PROPERTY,
                                                        RemoteConstants.ENDPOINT_ID);
    }

    protected String getServerAddress(Map<String, Object> sd, Class<?> iClass) {
        String address = getClientAddress(sd);
        return address == null ? httpServiceManager.getDefaultAddress(iClass) : address;
    }
    
    protected Bus createBus(Long sid, BundleContext callingContext, String contextRoot) {
        Bus bus = BusFactory.newInstance().createBus();
        if (contextRoot != null) {
            httpServiceManager.registerServlet(bus, contextRoot, callingContext, sid);
        }
        return bus;
    }

    protected Endpoint createServerFromFactory(ServerFactoryBean factory, EndpointDescription epd) {
        Server server = factory.create();
        return new ServerEndpoint(epd, server);
    }

    protected static void addContextProperties(AbstractEndpointFactory factory, 
                                               Map<String, Object> sd, String propName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>)sd.get(propName);
        if (props != null) {
            factory.getProperties(true).putAll(props);
        }
    }
    
    private DataBinding getDataBinding(Map<String, Object> sd, Class<?> iClass) {
        return isJAXWS(sd, iClass) ? new JAXBDataBinding() : new AegisDatabinding();
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
        return iClass.getAnnotation(WebService.class) != null;
    }
}
