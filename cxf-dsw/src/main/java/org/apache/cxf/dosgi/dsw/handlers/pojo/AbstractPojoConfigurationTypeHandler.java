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
package org.apache.cxf.dosgi.dsw.handlers.pojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.aries.rsa.spi.DistributionProvider;
import org.apache.aries.rsa.spi.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.dosgi.common.httpservice.HttpServiceManager;
import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.dosgi.common.util.OsgiUtils;
import org.apache.cxf.dosgi.common.util.ServerWrapper;
import org.apache.cxf.dosgi.common.util.StringPlus;
import org.apache.cxf.dosgi.dsw.osgi.Constants;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
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

    protected EndpointDescription createEndpointDesc(Map<String, Object> props, 
                                                     String[] importedConfigs,
                                                     String address, 
                                                     String[] intents) {
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, importedConfigs);
        for (String configurationType : importedConfigs) {
            if (Constants.WS_CONFIG_TYPE.equals(configurationType)) {
                props.put(Constants.WS_ADDRESS_PROPERTY, address);
            } else if (Constants.WS_CONFIG_TYPE_OLD.equals(configurationType)) {
                props.put(Constants.WS_ADDRESS_PROPERTY_OLD, address);
                props.put(Constants.WS_ADDRESS_PROPERTY, address);
            }
        }
        String[] sIntents = StringPlus.normalize(props.get(RemoteConstants.SERVICE_INTENTS));
        String[] allIntents = mergeArrays(intents, sIntents);
        props.put(RemoteConstants.SERVICE_INTENTS, allIntents);
        props.put(RemoteConstants.ENDPOINT_ID, address);
        return new EndpointDescription(props);
    }
    
    public static String[] mergeArrays(String[] a1, String[] a2) {
        if (a1 == null) {
            return a2;
        }
        if (a2 == null) {
            return a1;
        }

        List<String> list = new ArrayList<String>(a1.length + a2.length);
        Collections.addAll(list, a1);
        for (String s : a2) {
            if (!list.contains(s)) {
                list.add(s);
            }
        }

        return list.toArray(new String[list.size()]);
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
        InterceptorSupport.addInterceptors(factory, callingContext, sd, Constants.WS_IN_INTERCEPTORS_PROP_KEY);
        InterceptorSupport.addInterceptors(factory, callingContext, sd, Constants.WS_OUT_INTERCEPTORS_PROP_KEY);
        InterceptorSupport.addInterceptors(factory, callingContext, sd, Constants.WS_OUT_FAULT_INTERCEPTORS_PROP_KEY);
        InterceptorSupport.addInterceptors(factory, callingContext, sd, Constants.WS_IN_FAULT_INTERCEPTORS_PROP_KEY);
        InterceptorSupport.addFeatures(factory, callingContext, sd, Constants.WS_FEATURES_PROP_KEY);
        addContextProperties(factory, sd, Constants.WS_CONTEXT_PROPS_PROP_KEY);
    }

    private static void addContextProperties(AbstractEndpointFactory factory, Map<String, Object> sd, String propName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>)sd.get(propName);
        if (props != null) {
            factory.getProperties(true).putAll(props);
        }
    }
    
}
