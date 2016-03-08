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

import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.api.DistributionProvider;
import org.apache.cxf.dosgi.dsw.api.Endpoint;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentUtils;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.dosgi.dsw.util.StringPlus;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public abstract class AbstractPojoConfigurationTypeHandler implements DistributionProvider {
    protected BundleContext bundleContext;
    protected IntentManager intentManager;
    protected HttpServiceManager httpServiceManager;

    public AbstractPojoConfigurationTypeHandler(BundleContext dswBC, IntentManager intentManager,
                                                HttpServiceManager httpServiceManager) {
        this.bundleContext = dswBC;
        this.intentManager = intentManager;
        this.httpServiceManager = httpServiceManager;
    }

    protected Object getProxy(Object serviceProxy, Class<?> iType) {
        return Proxy.newProxyInstance(iType.getClassLoader(), new Class[] {
            iType
        }, new ServiceInvocationHandler(serviceProxy, iType));
    }

    protected EndpointDescription createEndpointDesc(Map<String, Object> props, 
                                                     String[] importedConfigs,
                                                     String address, 
                                                     String[] intents) {
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, importedConfigs);
        for (String configurationType : importedConfigs) {
            if (Constants.WS_CONFIG_TYPE.equals(configurationType)) {
                props.put(Constants.WS_ADDRESS_PROPERTY, address);
            } else if (Constants.RS_CONFIG_TYPE.equals(configurationType)) {
                props.put(Constants.RS_ADDRESS_PROPERTY, address);
            } else if (Constants.WS_CONFIG_TYPE_OLD.equals(configurationType)) {
                props.put(Constants.WS_ADDRESS_PROPERTY_OLD, address);
                props.put(Constants.WS_ADDRESS_PROPERTY, address);
            }
        }
        String[] sIntents = StringPlus.normalize(props.get(RemoteConstants.SERVICE_INTENTS));
        String[] allIntents = IntentUtils.mergeArrays(intents, sIntents);
        props.put(RemoteConstants.SERVICE_INTENTS, allIntents);
        props.put(RemoteConstants.ENDPOINT_ID, address);
        return new EndpointDescription(props);
    }

    protected void setCommonWsdlProperties(AbstractWSDLBasedEndpointFactory factory, BundleContext context,
                                           Map<String, Object> sd, boolean wsdlType) {
        String location = OsgiUtils.getProperty(sd, wsdlType ? Constants.WSDL_LOCATION : Constants.WS_WSDL_LOCATION);
        if (location != null) {
            URL wsdlURL = context.getBundle().getResource(location);
            if (wsdlURL != null) {
                factory.setWsdlURL(wsdlURL.toString());
            }
            QName serviceName = getServiceQName(null, sd,
                    wsdlType ? Constants.WSDL_SERVICE_NAMESPACE : Constants.WS_WSDL_SERVICE_NAMESPACE,
                    wsdlType ? Constants.WSDL_SERVICE_NAME : Constants.WS_WSDL_SERVICE_NAME);
            if (serviceName != null) {
                factory.setServiceName(serviceName);
                QName portName = getPortQName(serviceName.getNamespaceURI(), sd,
                        wsdlType ? Constants.WSDL_PORT_NAME : Constants.WS_WSDL_PORT_NAME);
                if (portName != null) {
                    factory.setEndpointName(portName);
                }
            }
        }
    }

    protected void setWsdlProperties(ServerFactoryBean factory, BundleContext callingContext, Map<String, Object> sd,
                                     boolean wsdlType) {
        setCommonWsdlProperties(factory, callingContext, sd, wsdlType);
    }

    protected void setClientWsdlProperties(ClientFactoryBean factory, BundleContext dswContext, Map<String, Object> sd,
                                           boolean wsdlType) {
        setCommonWsdlProperties(factory, dswContext, sd, wsdlType);
    }

    protected static QName getServiceQName(Class<?> iClass, Map<String, Object> sd, String nsPropName,
                                           String namePropName) {
        String serviceNs = OsgiUtils.getProperty(sd, nsPropName);
        String serviceName = OsgiUtils.getProperty(sd, namePropName);
        if (iClass == null && (serviceNs == null || serviceName == null)) {
            return null;
        }
        if (serviceNs == null) {
            serviceNs = PackageUtils.getNamespace(PackageUtils.getPackageName(iClass));
        }
        if (serviceName == null) {
            serviceName = iClass.getSimpleName();
        }
        return new QName(serviceNs, serviceName);
    }

    protected static QName getPortQName(String ns, Map<String, Object> sd, String propName) {
        String portName = OsgiUtils.getProperty(sd, propName);
        if (portName == null) {
            return null;
        }
        return new QName(ns, portName);
    }

    protected String getClientAddress(Map<String, Object> sd) {
        return OsgiUtils.getFirstNonEmptyStringProperty(sd, RemoteConstants.ENDPOINT_ID,
                                                        Constants.WS_ADDRESS_PROPERTY,
                                                        Constants.WS_ADDRESS_PROPERTY_OLD,
                                                        Constants.RS_ADDRESS_PROPERTY);
    }

    protected String getServerAddress(Map<String, Object> sd, Class<?> iClass) {
        String address = getClientAddress(sd);
        return address == null ? httpServiceManager.getDefaultAddress(iClass) : address;
    }
    
    public String getServletContextRoot(Map<String, Object> sd) {
        return OsgiUtils.getFirstNonEmptyStringProperty(sd,
                Constants.WS_HTTP_SERVICE_CONTEXT,
                Constants.WS_HTTP_SERVICE_CONTEXT_OLD,
                Constants.WSDL_HTTP_SERVICE_CONTEXT,
                Constants.RS_HTTP_SERVICE_CONTEXT);
    }

    
    protected Bus createBus(Long sid, BundleContext callingContext, String contextRoot) {
        Bus bus = BusFactory.newInstance().createBus();
        if (contextRoot != null) {
            httpServiceManager.registerServlet(bus, contextRoot, callingContext, sid);
        }
        return bus;
    }

    protected Endpoint createServerFromFactory(ServerFactoryBean factory, EndpointDescription epd) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ServerFactoryBean.class.getClassLoader());
            Server server = factory.create();
            return new ServerWrapper(epd, server);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    protected static void addWsInterceptorsFeaturesProps(AbstractEndpointFactory factory, BundleContext callingContext,
                                                         Map<String, Object> sd) {
        addInterceptors(factory, callingContext, sd, Constants.WS_IN_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.WS_OUT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.WS_OUT_FAULT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.WS_IN_FAULT_INTERCEPTORS_PROP_KEY);
        addFeatures(factory, callingContext, sd, Constants.WS_FEATURES_PROP_KEY);
        addContextProperties(factory, sd, Constants.WS_CONTEXT_PROPS_PROP_KEY);
    }

    static void addRsInterceptorsFeaturesProps(AbstractEndpointFactory factory, BundleContext callingContext,
                                               Map<String, Object> sd) {
        addInterceptors(factory, callingContext, sd, Constants.RS_IN_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.RS_OUT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.RS_OUT_FAULT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.RS_IN_FAULT_INTERCEPTORS_PROP_KEY);
        addFeatures(factory, callingContext, sd, Constants.RS_FEATURES_PROP_KEY);
        addContextProperties(factory, sd, Constants.RS_CONTEXT_PROPS_PROP_KEY);
    }

    private static void addInterceptors(AbstractEndpointFactory factory, BundleContext callingContext,
                                        Map<String, Object> sd, String propName) {
        List<Object> providers = ClassUtils.loadProviderClasses(callingContext, sd, propName);
        boolean in = propName.contains("in.interceptors");
        boolean out = propName.contains("out.interceptors");
        boolean inFault = propName.contains("in.fault.interceptors");
        boolean outFault = propName.contains("out.fault.interceptors");
        for (Object provider : providers) {
            Interceptor<?> interceptor = (Interceptor<?>) provider;
            if (in) {
                factory.getInInterceptors().add(interceptor);
            } else if (out) {
                factory.getOutInterceptors().add(interceptor);
            } else if (inFault) {
                factory.getInFaultInterceptors().add(interceptor);
            } else if (outFault) {
                factory.getOutFaultInterceptors().add(interceptor);
            }
        }
    }

    private static void addFeatures(AbstractEndpointFactory factory, BundleContext callingContext,
                                    Map<String, Object> sd, String propName) {
        List<Object> providers = ClassUtils.loadProviderClasses(callingContext, sd, propName);
        if (!providers.isEmpty()) {
            factory.getFeatures().addAll(CastUtils.cast(providers, AbstractFeature.class));
        }
    }

    private static void addContextProperties(AbstractEndpointFactory factory, Map<String, Object> sd, String propName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>)sd.get(propName);
        if (props != null) {
            factory.getProperties(true).putAll(props);
        }
    }
    
}
