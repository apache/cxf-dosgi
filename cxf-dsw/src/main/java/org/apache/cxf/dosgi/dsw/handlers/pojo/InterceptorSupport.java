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

import java.util.List;
import java.util.Map;

import org.apache.cxf.dosgi.common.util.ClassUtils;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.osgi.framework.BundleContext;

public final class InterceptorSupport {
    private InterceptorSupport() {
    }

    public static void addInterceptors(AbstractEndpointFactory factory, BundleContext callingContext,
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

    public static void addFeatures(AbstractEndpointFactory factory, BundleContext callingContext,
                                    Map<String, Object> sd, String propName) {
        List<Object> providers = ClassUtils.loadProviderClasses(callingContext, sd, propName);
        if (!providers.isEmpty()) {
            factory.getFeatures().addAll(CastUtils.cast(providers, AbstractFeature.class));
        }
    }
}
