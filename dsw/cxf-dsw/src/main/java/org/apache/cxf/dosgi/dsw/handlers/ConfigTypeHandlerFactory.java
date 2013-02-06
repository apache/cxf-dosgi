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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.dosgi.dsw.util.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigTypeHandlerFactory {
    protected static final String DEFAULT_CONFIGURATION_TYPE = Constants.WS_CONFIG_TYPE;
    private static final Logger LOG = LoggerFactory.getLogger(ConfigTypeHandlerFactory.class);

    // protected because of tests
    protected final List<String> supportedConfigurationTypes;

    private IntentManager intentManager;
    private PojoConfigurationTypeHandler pojoConfigurationTypeHandler;
    private JaxRSPojoConfigurationTypeHandler jaxRsPojoConfigurationTypeHandler;
    private WsdlConfigurationTypeHandler wsdlConfigurationTypeHandler;

    public ConfigTypeHandlerFactory(BundleContext bc, IntentManager intentManager,
                                    HttpServiceManager httpServiceManager) {
        this.intentManager = intentManager;
        this.pojoConfigurationTypeHandler = new PojoConfigurationTypeHandler(bc, intentManager, httpServiceManager);
        this.jaxRsPojoConfigurationTypeHandler = new JaxRSPojoConfigurationTypeHandler(bc,
                                                                                       intentManager,
                                                                                       httpServiceManager);
        this.wsdlConfigurationTypeHandler = new WsdlConfigurationTypeHandler(bc, intentManager, httpServiceManager);
        supportedConfigurationTypes = new ArrayList<String>();
        supportedConfigurationTypes.add(Constants.WSDL_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.RS_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.WS_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.WS_CONFIG_TYPE_OLD);
    }

    public ConfigurationTypeHandler getHandler(BundleContext dswBC,
            Map<String, Object> serviceProperties) {
        List<String> configurationTypes = determineConfigurationTypes(serviceProperties);
        return getHandler(dswBC, configurationTypes, serviceProperties);
    }

    public ConfigurationTypeHandler getHandler(BundleContext dswBC, EndpointDescription endpoint) {
        List<String> configurationTypes = determineConfigTypesForImport(endpoint);
        return getHandler(dswBC, configurationTypes, endpoint.getProperties());
    }

    private ConfigurationTypeHandler getHandler(BundleContext dswBC,
                                               List<String> configurationTypes,
                                               Map<String, Object> serviceProperties) {
        intentManager.assertAllIntentsSupported(serviceProperties);
        if (configurationTypes.contains(Constants.WS_CONFIG_TYPE)
            || configurationTypes.contains(Constants.WS_CONFIG_TYPE_OLD)
            || configurationTypes.contains(Constants.RS_CONFIG_TYPE)) {

            boolean jaxrs = isJaxrsRequested(configurationTypes, serviceProperties);

            return jaxrs ? jaxRsPojoConfigurationTypeHandler : pojoConfigurationTypeHandler;
        } else if (configurationTypes.contains(Constants.WSDL_CONFIG_TYPE)) {
            return wsdlConfigurationTypeHandler;
        }
        throw new RuntimeException("None of the configuration types in " + configurationTypes + " is supported.");
    }

    private boolean isJaxrsRequested(Collection<String> types,  Map<String, Object> serviceProperties) {

        if (types == null) {
            return false;
        }

        if (types.contains(Constants.RS_CONFIG_TYPE)) {
            Collection<String> intentsProperty = OsgiUtils.getMultiValueProperty(serviceProperties.get(RemoteConstants.SERVICE_EXPORTED_INTENTS));
            boolean hasHttpIntent = false;
            boolean hasSoapIntent = false;
            if (intentsProperty != null) {
                for (String intent : intentsProperty) {
                    if (intent.indexOf("SOAP") > -1) {
                        hasSoapIntent = true;
                        break;
                    }

                    if (intent.indexOf("HTTP") > -1) {
                        hasHttpIntent = true;
                    }
                }
            }
            if ((hasHttpIntent && !hasSoapIntent) || intentsProperty == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * determine which configuration types should be used / if the requested are
     * supported
     */
    private List<String> determineConfigurationTypes(Map<String, Object> serviceProperties) {
        String[] requestedConfigurationTypes = Utils.normalizeStringPlus(serviceProperties
                .get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
        if (requestedConfigurationTypes == null || requestedConfigurationTypes.length == 0) {
            return Collections.singletonList(DEFAULT_CONFIGURATION_TYPE);
        }

        List<String> configurationTypes = new ArrayList<String>();
        for (String rct : requestedConfigurationTypes) {
            if (supportedConfigurationTypes.contains(rct)) {
                configurationTypes.add(rct);
            }
        }
        LOG.info("configuration types selected for export: " + configurationTypes);
        if (configurationTypes.size() == 0) {
            throw new RuntimeException("the requested configuration types are not supported");
        }
        return configurationTypes;
    }

    private List<String> determineConfigTypesForImport(EndpointDescription endpoint) {
        List<String> remoteConfigurationTypes = endpoint.getConfigurationTypes();

        if (remoteConfigurationTypes == null) {
            throw new RuntimeException("The supplied endpoint has no configuration type");
        }

        List<String> usableConfigurationTypes = new ArrayList<String>();
        for (String ct : supportedConfigurationTypes) {
            if (remoteConfigurationTypes.contains(ct)) {
                usableConfigurationTypes.add(ct);
            }
        }

        if (usableConfigurationTypes.size() == 0) {
            throw new RuntimeException("The supplied endpoint has no compatible configuration type. "
                                       + "Supported types are: "
                                       + supportedConfigurationTypes
                                       + "    Types needed by the endpoint: "
                                       + remoteConfigurationTypes);
        }
        return usableConfigurationTypes;
    }

    public List<String> getSupportedConfigurationTypes() {
        return supportedConfigurationTypes;
    }

}
