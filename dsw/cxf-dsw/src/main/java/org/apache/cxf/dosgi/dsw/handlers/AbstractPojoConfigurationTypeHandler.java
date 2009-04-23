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

import static org.osgi.service.discovery.ServicePublication.PROP_KEY_ENDPOINT_LOCATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.distribution.DistributionConstants;

public abstract class AbstractPojoConfigurationTypeHandler extends AbstractConfigurationHandler {
    private static final Logger LOG = Logger.getLogger(AbstractPojoConfigurationTypeHandler.class.getName());
    private static final String PROVIDED_INTENT_VALUE = "PROVIDED";
    
    private IntentMap masterMap;
    
    public AbstractPojoConfigurationTypeHandler(BundleContext dswBC, 
                                                CxfDistributionProvider dp, 
                                                Map<String, Object> handlerProps) {
        super(dswBC, dp, handlerProps);
    }

    // Isolated so that it can be substituted for testing
    ClientProxyFactoryBean createClientProxyFactoryBean() {
        return new ClientProxyFactoryBean();
    }

    // Isolated so that it can be substituted for testing
    ServerFactoryBean createServerFactoryBean() {
        return new ServerFactoryBean();
    }

    Map<String, String> registerPublication(Server server, String[] intents) {
        Map<String, String> publicationProperties = new HashMap<String, String>();
        publicationProperties.put(DistributionConstants.PROP_KEY_SERVICE_REMOTE_CONFIG_TYPE,
                Constants.POJO_CONFIG_TYPE);

        String intentsValue = OsgiUtils.formatIntents(intents);
        if (intentsValue.length() > 0) {
            publicationProperties.put(DistributionConstants.PROP_KEY_DEPLOYMENT_INTENTS, intentsValue);
        }
        return publicationProperties;
    }

    String [] applyIntents(BundleContext dswContext,
                           BundleContext callingContext,
                           List<AbstractFeature> features,
                           AbstractEndpointFactory factory,
                           ServiceEndpointDescription sd) throws IntentUnsatifiedException {
        String[] requestedIntents = getRequestedIntents(sd);
        Set<String> appliedIntents = new HashSet<String>(Arrays.asList(requestedIntents));
        
        IntentMap intentMap = getIntentMap(callingContext);        
        if (useMasterMap()) {
            intentMap = mergeWithMaster(dswContext, intentMap);
        }
        appliedIntents.addAll(reverseLookup(intentMap, PROVIDED_INTENT_VALUE));
        
        boolean bindingConfigAdded = false;
        for (String requestedName : requestedIntents) {
            bindingConfigAdded 
                |= processIntent(appliedIntents, features, factory, requestedName, intentMap);
        }
        
        if (!bindingConfigAdded && getDefaultBindingIntent() != null) {
            // If no binding config was specified, add SOAP
            processIntent(appliedIntents, features, factory, getDefaultBindingIntent(), intentMap);
        }

        appliedIntents.addAll(addSynonymIntents(appliedIntents, intentMap));        
        return appliedIntents.toArray(new String[0]);
    }

    private boolean processIntent(Set<String> appliedIntents,
                                  List<AbstractFeature> features,
                                  AbstractEndpointFactory factory, String intentName,
                                  IntentMap intentMap) throws IntentUnsatifiedException {
        boolean rc = processIntent(features, factory, intentName, intentMap);
        appliedIntents.add(intentName);
        return rc;
    }
    
    private boolean processIntent(List<AbstractFeature> features,
                                  AbstractEndpointFactory factory, String intentName,
                                  IntentMap intentMap) throws IntentUnsatifiedException {
        Object intent = intentMap.get(intentName);
        if (intent instanceof String) {
            if (PROVIDED_INTENT_VALUE.equalsIgnoreCase((String) intent)) {
                return false;
            }
        } else if (intent instanceof AbstractFeature) {
            AbstractFeature feature = (AbstractFeature)intent;
            LOG.info("Applying intent: " + intentName
                     + " via feature: " + feature);
            features.add(feature);
            return false;
        } else if (intent instanceof BindingConfiguration) {
            BindingConfiguration bindingCfg = (BindingConfiguration)intent;
            LOG.info("Applying intent: " + intentName
                     + " via binding config: " + bindingCfg);
            factory.setBindingConfig(bindingCfg);
            return true;
        } else {
            LOG.info("No mapping for intent: " + intentName);
            throw new IntentUnsatifiedException(intentName);
        }
        return false;
    }
    

    private Collection<String> addSynonymIntents(Collection<String> appliedIntents, IntentMap intentMap) {
        // E.g. SOAP and SOAP.1_1 are synonyms
        List<Object> values = new ArrayList<Object>();
        for (String key : appliedIntents) {
            values.add(intentMap.get(key));
        }
        return reverseLookup(intentMap, values);
    }

    private Collection<String> reverseLookup(IntentMap intentMap, Object obj) {
        return reverseLookup(intentMap, Collections.singleton(obj));
    }
    
    private Collection<String> reverseLookup(IntentMap intentMap, Collection<? extends Object> objs) {
        Set<String> intentsFound = new HashSet<String>();
        for (Map.Entry<String, Object> entry : intentMap.getIntents().entrySet()) {
            if (objs.contains(entry.getValue())) {
                intentsFound.add(entry.getKey());
            }
        }
        return intentsFound;
    }
    
    String getDefaultBindingIntent() {
        return "SOAP";
    }

    IntentMap getIntentMap(BundleContext callingContext) {
        return OsgiUtils.getIntentMap(callingContext);
    }

    private static String[] getRequestedIntents(ServiceEndpointDescription sd) {
        String property = OsgiUtils.getProperty(sd, 
                DistributionConstants.PROP_KEY_SERVICE_REMOTE_REQUIRES_INTENTS);

        String[] intents = OsgiUtils.parseIntents(property);
        for (int i = 0; i < intents.length; i++) {
            LOG.fine("Intent asserted: " + intents[i]);
        } 
        return intents;
    }
    
    private IntentMap mergeWithMaster(BundleContext dswContext, IntentMap intentMap) {
        synchronized (this) {
            if (masterMap == null) {
                LOG.fine("Loading master intent map");
                masterMap = getIntentMap(dswContext);
            }
        }
        if (masterMap != null) {
            Iterator<String> masterKeys = masterMap.getIntents().keySet().iterator();
            while (masterKeys.hasNext()) {
                String masterKey = masterKeys.next();
                if (intentMap.get(masterKey) == null) {
                    LOG.fine("Merging in master intent map entry: " + masterKey);
                    intentMap.getIntents().put(masterKey, masterMap.get(masterKey));
                } else {
                    LOG.fine("Overridden master intent map entry: " + masterKey);
                }
            }
        }
        return intentMap;
    }    

    protected void addAddressProperty(Map props, String address) {
        if (props != null) {
            props.put(PROP_KEY_ENDPOINT_LOCATION, address);
        }
    }
}
