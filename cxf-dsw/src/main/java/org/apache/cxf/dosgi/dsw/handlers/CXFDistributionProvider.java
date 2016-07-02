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

import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_CONFIGS_SUPPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_INTENTS_SUPPORTED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.rsa.spi.DistributionProvider;
import org.apache.aries.rsa.spi.Endpoint;
import org.apache.aries.rsa.spi.IntentUnsatisfiedException;
import org.apache.cxf.dosgi.common.httpservice.HttpServiceManager;
import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.dosgi.common.util.OsgiUtils;
import org.apache.cxf.dosgi.common.util.StringPlus;
import org.apache.cxf.dosgi.dsw.handlers.pojo.PojoConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.handlers.pojo.WsdlConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.handlers.rest.JaxRSPojoConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.osgi.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(configurationPid = "cxf-dsw", property = //
{//
 REMOTE_CONFIGS_SUPPORTED + "=" + Constants.WS_CONFIG_TYPE,
 REMOTE_CONFIGS_SUPPORTED + "=" + Constants.WSDL_CONFIG_TYPE,
 REMOTE_CONFIGS_SUPPORTED + "=" + Constants.RS_CONFIG_TYPE,
 REMOTE_CONFIGS_SUPPORTED + "=" + Constants.WS_CONFIG_TYPE_OLD,
 REMOTE_INTENTS_SUPPORTED + "=" 
})
public class CXFDistributionProvider implements DistributionProvider {
    public static final String[] SUPPORTED_CONFIGS = new String[] //
    {//
     Constants.WS_CONFIG_TYPE, Constants.WSDL_CONFIG_TYPE, Constants.RS_CONFIG_TYPE,
     Constants.WS_CONFIG_TYPE_OLD
    };

    protected static final String DEFAULT_CONFIGURATION_TYPE = Constants.WS_CONFIG_TYPE;
    private static final Logger LOG = LoggerFactory.getLogger(CXFDistributionProvider.class);


    private IntentManager intentManager;
    private PojoConfigurationTypeHandler pojoConfigurationTypeHandler;
    private JaxRSPojoConfigurationTypeHandler jaxRsPojoConfigurationTypeHandler;
    private WsdlConfigurationTypeHandler wsdlConfigurationTypeHandler;
    private Set<String> configTypesSet;
    private HttpServiceManager httpServiceManager;
    
    public CXFDistributionProvider() {
        configTypesSet = new HashSet<>(Arrays.asList(SUPPORTED_CONFIGS));
    }

    @Reference
    public void setHttpServiceManager(HttpServiceManager httpServiceManager) {
        this.httpServiceManager = httpServiceManager;
    }
    
    @Reference
    public void setIntentManager(IntentManager intentManager) {
        this.intentManager = intentManager;
    }
    
    @Activate
    public synchronized void activate(ComponentContext compContext) {
        Dictionary<String, Object> config = compContext.getProperties();
        init(compContext.getBundleContext(), config);
        // String[] supportedIntents = intentMap.keySet().toArray(new String[] {});
        // props.put(Constants.REMOTE_INTENTS_SUPPORTED, supportedIntents);
    }

    void init(BundleContext bc, Dictionary<String, Object> config) {
        // Disable the fast infoset as it's not compatible (yet) with OSGi
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        LOG.debug("RemoteServiceAdmin Implementation is starting up with {}", config);
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        this.pojoConfigurationTypeHandler = new PojoConfigurationTypeHandler(bc, intentManager, httpServiceManager);
        this.jaxRsPojoConfigurationTypeHandler = new JaxRSPojoConfigurationTypeHandler(bc,
                                                                                       intentManager,
                                                                                       httpServiceManager);
        this.wsdlConfigurationTypeHandler = new WsdlConfigurationTypeHandler(bc, intentManager, httpServiceManager);
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public Endpoint exportService(Object serviceO, 
                                  BundleContext serviceContext,
                                  Map<String, Object> effectiveProperties,
                                  Class[] exportedInterfaces) {
        List<String> configurationTypes = determineConfigurationTypes(effectiveProperties);
        DistributionProvider handler = getHandler(configurationTypes, effectiveProperties);
        return handler != null ? handler.exportService(serviceO, serviceContext, 
                                                       effectiveProperties, exportedInterfaces) : null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object importEndpoint(ClassLoader consumerLoader, BundleContext consumerContext, 
                                 Class[] iClass, EndpointDescription endpoint)
        throws IntentUnsatisfiedException {
        List<String> configurationTypes = determineConfigTypesForImport(endpoint);
        DistributionProvider handler = getHandler(configurationTypes, endpoint.getProperties());
        return handler != null ? handler.importEndpoint(consumerLoader, consumerContext, iClass, endpoint) : null;
    }

    DistributionProvider getHandler(List<String> configurationTypes,
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
        LOG.info("None of the configuration types in " + configurationTypes + " is supported.");
        return null;
    }

    private boolean isJaxrsRequested(Collection<String> types, Map<String, Object> serviceProperties) {
        if (types == null) {
            return false;
        }

        if (types.contains(Constants.RS_CONFIG_TYPE)) {
            Collection<String> intentsProperty
                = OsgiUtils.getMultiValueProperty(serviceProperties.get(RemoteConstants.SERVICE_EXPORTED_INTENTS));
            boolean hasHttpIntent = false;
            boolean hasSoapIntent = false;
            if (intentsProperty != null) {
                for (String intent : intentsProperty) {
                    if (intent.contains("SOAP")) {
                        hasSoapIntent = true;
                        break;
                    }

                    if (intent.contains("HTTP")) {
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
    List<String> determineConfigurationTypes(Map<String, Object> serviceProperties) {
        String[] requestedConfigurationTypes = StringPlus.normalize(serviceProperties
                .get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
        if (requestedConfigurationTypes == null || requestedConfigurationTypes.length == 0) {
            return Collections.singletonList(DEFAULT_CONFIGURATION_TYPE);
        }

        List<String> configurationTypes = new ArrayList<String>();
        for (String rct : requestedConfigurationTypes) {
            if (configTypesSet.contains(rct)) {
                configurationTypes.add(rct);
            }
        }
        LOG.info("Configuration types selected for export: {}.", configurationTypes);
        return configurationTypes;
    }

    private List<String> determineConfigTypesForImport(EndpointDescription endpoint) {
        List<String> remoteConfigurationTypes = endpoint.getConfigurationTypes();

        List<String> usableConfigurationTypes = new ArrayList<String>();
        for (String ct : SUPPORTED_CONFIGS) {
            if (remoteConfigurationTypes.contains(ct)) {
                usableConfigurationTypes.add(ct);
            }
        }

        LOG.info("Ignoring endpoint {} as it has no compatible configuration types: {}.", 
                 endpoint.getId(), remoteConfigurationTypes);
        return usableConfigurationTypes;
    }

    public String[] getSupportedTypes() {
        return SUPPORTED_CONFIGS;
    }

}
