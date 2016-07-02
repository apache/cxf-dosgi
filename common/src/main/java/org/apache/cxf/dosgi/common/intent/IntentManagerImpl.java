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
package org.apache.cxf.dosgi.common.intent;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.rsa.spi.IntentUnsatisfiedException;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.dosgi.common.util.OsgiUtils;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.feature.LoggingFeature;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IntentManager.class)
public class IntentManagerImpl implements IntentManager {

    static final Logger LOG = LoggerFactory.getLogger(IntentManagerImpl.class);
    private static final String PROVIDED_INTENT_VALUE = "PROVIDED";
    private static final int DEFAULT_INTENT_TIMEOUT = 30000;

    private final IntentMap intentMap;
    private final long maxIntentWaitTime = DEFAULT_INTENT_TIMEOUT;

    public IntentManagerImpl() {
        this(new IntentMap(create()));
    }

    public IntentManagerImpl(IntentMap intentMap) {
        this.intentMap = intentMap;
    }
    
    public static Map<String, Object> create() {
        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put("logging", getLoggingFeature());
        Object soap11 = getSoapBinding(Soap11.getInstance());
        defaults.put("SOAP", soap11);
        defaults.put("SOAP.1_1", soap11);
        defaults.put("SOAP.1_2", getSoapBinding(Soap12.getInstance()));
        defaults.put("HTTP", "PROVIDED");
        return defaults;
    }

    private static Object getLoggingFeature() {
        return new LoggingFeature();
    }

    private static Object getSoapBinding(SoapVersion soapVersion) {
        SoapBindingConfiguration soapBindingConfig = new SoapBindingConfiguration();
        soapBindingConfig.setVersion(soapVersion);
        return soapBindingConfig;
    }

    @Reference //
    (//
        cardinality = MULTIPLE, //
        policy = ReferencePolicy.DYNAMIC, //
        target = "(" + IntentManager.INTENT_NAME_PROP + "=*)", //
        policyOption = ReferencePolicyOption.GREEDY
    )
    public void addIntent(Object intent, Map<String, ?> props) {
        String intentName = (String)props.get(INTENT_NAME_PROP);
        LOG.info("Adding custom intent " + intentName);
        intentMap.put(intentName, intent);
    }

    public void removeIntent(Object intent, Map<String, ?> props) {
        String intentName = (String)props.get(INTENT_NAME_PROP);
        intentMap.remove(intentName);
    }

    public String[] applyIntents(List<Feature> features, AbstractEndpointFactory factory,
                                 Map<String, Object> props)
        throws IntentUnsatisfiedException {
        Set<String> requestedIntents = IntentManagerImpl.getRequestedIntents(props);
        Set<String> appliedIntents = new HashSet<String>();
        appliedIntents.addAll(reverseLookup(intentMap, PROVIDED_INTENT_VALUE));
        boolean bindingApplied = false;
        for (String intentName : requestedIntents) {
            bindingApplied |= processIntent(features, factory, intentName, intentMap.get(intentName));
            appliedIntents.add(intentName);
        }
        if (!bindingApplied) {
            String defaultBindingName = "SOAP";
            processIntent(features, factory, defaultBindingName, intentMap.get(defaultBindingName));
            appliedIntents.add(defaultBindingName);
        }
        appliedIntents.addAll(addSynonymIntents(appliedIntents, intentMap));
        return appliedIntents.toArray(new String[appliedIntents.size()]);
    }

    private static Set<String> getRequestedIntents(Map<String, Object> sd) {
        Collection<String> intents = OsgiUtils
            .getMultiValueProperty(sd.get(RemoteConstants.SERVICE_EXPORTED_INTENTS));
        Collection<String> intents2 = OsgiUtils
            .getMultiValueProperty(sd.get(RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA));
        @SuppressWarnings("deprecation")
        Collection<String> oldIntents = OsgiUtils
            .getMultiValueProperty(sd.get(IntentManager.EXPORTED_INTENTS_OLD));
        Set<String> allIntents = new HashSet<String>();
        if (intents != null) {
            allIntents.addAll(parseIntents(intents));
        }
        if (intents2 != null) {
            allIntents.addAll(parseIntents(intents2));
        }
        if (oldIntents != null) {
            allIntents.addAll(parseIntents(oldIntents));
        }

        return allIntents;
    }

    private static Collection<String> parseIntents(Collection<String> intents) {
        List<String> parsed = new ArrayList<String>();
        for (String intent : intents) {
            parsed.addAll(Arrays.asList(intent.split("[ ]")));
        }
        return parsed;
    }

    private boolean processIntent(List<Feature> features, AbstractEndpointFactory factory, String intentName,
                                  Object intent)
        throws IntentUnsatisfiedException {
        if (intent instanceof String) {
            if (PROVIDED_INTENT_VALUE.equalsIgnoreCase((String)intent)) {
                return false;
            }
        } else if (intent instanceof BindingConfiguration) {
            BindingConfiguration bindingCfg = (BindingConfiguration)intent;
            LOG.info("Applying intent: " + intentName + " via binding config: " + bindingCfg);
            factory.setBindingConfig(bindingCfg);
            return true;
        } else if (intent instanceof Feature) {
            Feature feature = (Feature)intent;
            LOG.info("Applying intent: " + intentName + " via feature: " + feature);
            features.add(feature);
            return false;
        } else {
            LOG.info("No mapping for intent: " + intentName);
            throw new IntentUnsatisfiedException(intentName);
        }
        return false;
    }

    private static Collection<String> addSynonymIntents(Collection<String> appliedIntents, IntentMap map) {
        // E.g. SOAP and SOAP.1_1 are synonyms
        List<Object> values = new ArrayList<Object>();
        for (String key : appliedIntents) {
            values.add(map.get(key));
        }
        return reverseLookup(map, values);
    }

    private static Collection<String> reverseLookup(IntentMap im, Object obj) {
        return reverseLookup(im, Collections.singleton(obj));
    }

    /**
     * Retrieves all keys whose mapped values are found in the given collection.
     *
     * @param im an intent map
     * @param values a collection of potential values
     * @return all keys whose mapped values are found in the given collection
     */
    private static Collection<String> reverseLookup(IntentMap im, Collection<?> values) {
        Set<String> intentsFound = new HashSet<String>();
        for (Map.Entry<String, Object> entry : im.entrySet()) {
            if (values.contains(entry.getValue())) {
                intentsFound.add(entry.getKey());
            }
        }
        return intentsFound;
    }

    public void assertAllIntentsSupported(Map<String, Object> serviceProperties) {
        long endTime = System.currentTimeMillis() + maxIntentWaitTime;
        Set<String> requiredIntents = IntentManagerImpl.getRequestedIntents(serviceProperties);
        List<String> unsupportedIntents = new ArrayList<String>();
        do {
            unsupportedIntents.clear();
            for (String ri : requiredIntents) {
                if (!intentMap.containsKey(ri)) {
                    unsupportedIntents.add(ri);
                }
            }
            long remainingSeconds = (endTime - System.currentTimeMillis()) / 1000;
            if (!unsupportedIntents.isEmpty() && remainingSeconds > 0) {
                LOG.debug("Waiting for custom intents " + unsupportedIntents + " timeout in "
                          + remainingSeconds);
                try {
                    synchronized (intentMap) {
                        intentMap.wait(1000);
                    }
                } catch (InterruptedException e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        } while (!unsupportedIntents.isEmpty() && System.currentTimeMillis() < endTime);

        if (!unsupportedIntents.isEmpty()) {
            throw new RuntimeException("service cannot be exported because the following "
                                       + "intents are not supported by this RSA: " + unsupportedIntents);
        }
    }
}
