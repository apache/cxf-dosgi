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

import java.util.List;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.util.ClassUtils;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.osgi.framework.BundleContext;

public class InterceptorUtils {

    protected static void addWsInterceptorsFeaturesProps(
            AbstractEndpointFactory factory, BundleContext callingContext, Map<String, Object> sd) {
        addInterceptors(factory, callingContext, sd, Constants.WS_IN_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.WS_OUT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.WS_OUT_FAULT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.WS_IN_FAULT_INTERCEPTORS_PROP_KEY);
        addFeatures(factory, callingContext, sd, Constants.WS_FEATURES_PROP_KEY);
        addContextProperties(factory, callingContext, sd, Constants.WS_CONTEXT_PROPS_PROP_KEY);
    }
    
    static void addRsInterceptorsFeaturesProps(
            AbstractEndpointFactory factory, BundleContext callingContext, Map<String, Object> sd) {
        addInterceptors(factory, callingContext, sd, Constants.RS_IN_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.RS_OUT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.RS_OUT_FAULT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.RS_IN_FAULT_INTERCEPTORS_PROP_KEY);
        addFeatures(factory, callingContext, sd, Constants.RS_FEATURES_PROP_KEY);
        addContextProperties(factory, callingContext, sd, Constants.RS_CONTEXT_PROPS_PROP_KEY);
    }

    private static void addInterceptors(AbstractEndpointFactory factory, BundleContext callingContext, 
            Map<String, Object> sd, String propName) {

        List<Object> providers = ClassUtils.loadProviderClasses(callingContext, sd, propName); 
        boolean in = propName.contains("in.interceptors");
        boolean out = propName.contains("out.interceptors");
        boolean in_fault = propName.contains("in.fault.interceptors");
        boolean out_fault = propName.contains("out.fault.interceptors");
        for (int i = 0; i < providers.size(); i++) {
            Interceptor<?> interceptor = (Interceptor<?>)providers.get(i);  
            if (in) {
                factory.getInInterceptors().add(interceptor);
            } else if (out) {
                factory.getOutInterceptors().add(interceptor);
            } else if (in_fault) {
                factory.getInFaultInterceptors().add(interceptor);
            } else if (out_fault) {
                factory.getOutFaultInterceptors().add(interceptor);
            }
        }
    }
       
    private static void addFeatures(AbstractEndpointFactory factory, BundleContext callingContext, 
            Map<String, Object> sd, String propName) {

        List<Object> providers = ClassUtils.loadProviderClasses(callingContext, sd, propName); 
        if (providers.size() > 0) {
            factory.getFeatures().addAll(CastUtils.cast(providers, AbstractFeature.class));
        }
    }
    
    private static void addContextProperties(AbstractEndpointFactory factory, BundleContext callingContext, 
            Map<String, Object> sd, String propName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>)sd.get(propName);
        if (props != null) {
            factory.getProperties(true).putAll(props);
        }
    }
}
