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
package org.apache.cxf.dosgi.common.handlers;

import static org.apache.cxf.dosgi.common.util.PropertyHelper.getMultiValueProperty;

import java.util.Collection;
import java.util.Map;

import org.apache.aries.rsa.spi.DistributionProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.dosgi.common.httpservice.HttpServiceManager;
import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public abstract class BaseDistributionProvider implements DistributionProvider {

    protected IntentManager intentManager;
    protected HttpServiceManager httpServiceManager;

    protected boolean configTypeSupported(Map<String, Object> endpointProps, String configType) {
        Collection<String> configs = getMultiValueProperty(endpointProps.get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
        return configs == null || configs.isEmpty() || configs.contains(configType);
    }

    protected EndpointDescription createEndpointDesc(Map<String, Object> props,
                                                     String[] importedConfigs,
                                                     String addressPropName,
                                                     String address,
                                                     Collection<String> intents) {
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, importedConfigs);
        props.put(addressPropName, address);
        props.put(RemoteConstants.SERVICE_INTENTS, intents);
        props.put(RemoteConstants.ENDPOINT_ID, address);
        return new EndpointDescription(props);
    }

    protected Bus createBus(Long sid, BundleContext callingContext, String contextRoot,
                            Map<String, Object> endpointProps) {
        Bus bus = BusFactory.newInstance().createBus();
        for (Map.Entry<String, Object> prop : endpointProps.entrySet()) {
            if (prop.getKey().startsWith("cxf.bus.prop.")) {
                bus.setProperty(prop.getKey().substring("cxf.bus.prop.".length()), prop.getValue());
            }
        }
        if (contextRoot != null) {
            httpServiceManager.registerServlet(bus, contextRoot, callingContext, sid);
        }
        return bus;
    }

    protected void addContextProperties(AbstractEndpointFactory factory, Map<String, Object> sd, String propName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>)sd.get(propName);
        if (props != null) {
            factory.getProperties(true).putAll(props);
        }
    }
}
