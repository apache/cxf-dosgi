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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.aries.rsa.spi.IntentUnsatisfiedException;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.common.util.OsgiUtils;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.feature.Feature;
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
    private static final int DEFAULT_INTENT_TIMEOUT = 30000;

    private final Map<String, Object> intentMap;
    private final long maxIntentWaitTime = DEFAULT_INTENT_TIMEOUT;

    public IntentManagerImpl() {
        this.intentMap = new HashMap<String, Object>();
    }

    @Reference //
    (//
        cardinality = MULTIPLE, //
        policy = ReferencePolicy.DYNAMIC, //
        target = "(" + IntentManager.INTENT_NAME_PROP + "=*)", //
        policyOption = ReferencePolicyOption.GREEDY
    )
    public synchronized void addIntent(Object intent, Map<String, ?> props) {
        String intentName = (String)props.get(INTENT_NAME_PROP);
        LOG.info("Adding custom intent " + intentName);
        intentMap.put(intentName, intent);
    }

    public synchronized void removeIntent(Object intent, Map<String, ?> props) {
        String intentName = (String)props.get(INTENT_NAME_PROP);
        intentMap.remove(intentName);
    }

    public synchronized String[] applyIntents(List<Feature> features, AbstractEndpointFactory factory,
                                 Map<String, Object> props)
        throws IntentUnsatisfiedException {
        Set<String> requiredIntents = IntentManagerImpl.getRequestedIntents(props);
        List<String> missingIntents = getMissingIntents(requiredIntents);
        if (!missingIntents.isEmpty()) {
            throw new IntentUnsatisfiedException(missingIntents.iterator().next()); 
        }
        Set<String> requestedIntents = IntentManagerImpl.getRequestedIntents(props);
        Set<String> appliedIntents = new HashSet<String>();
        for (String intentName : requestedIntents) {
            processIntent(features, factory, intentName, intentMap.get(intentName));
            appliedIntents.add(intentName);
        }
        return appliedIntents.toArray(new String[appliedIntents.size()]);
    }

    private static Set<String> getRequestedIntents(Map<String, Object> sd) {
        Set<String> allIntents = new HashSet<String>();
        Collection<String> intents = OsgiUtils
            .getMultiValueProperty(sd.get(RemoteConstants.SERVICE_EXPORTED_INTENTS));
        if (intents != null) {
            allIntents.addAll(parseIntents(intents));
        }
        Collection<String> intents2 = OsgiUtils
            .getMultiValueProperty(sd.get(RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA));
        if (intents2 != null) {
            allIntents.addAll(parseIntents(intents2));
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
        if (intent instanceof DataBinding) {
            DataBinding dataBinding = (DataBinding) intent;
            LOG.info("Applying intent: " + intentName + " via data binding: " + dataBinding);
            factory.setDataBinding(dataBinding);
            return false;
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
    }

    public synchronized void assertAllIntentsSupported(Map<String, Object> serviceProperties) {
        long endTime = System.currentTimeMillis() + maxIntentWaitTime;
        Set<String> requiredIntents = IntentManagerImpl.getRequestedIntents(serviceProperties);

        List<String> unsupportedIntents;
        do {
            unsupportedIntents = getMissingIntents(requiredIntents);
            long remainingSeconds = (endTime - System.currentTimeMillis()) / 1000;
            if (!unsupportedIntents.isEmpty() && remainingSeconds > 0) {
                LOG.info("Waiting for custom intents " + unsupportedIntents + " timeout in "
                          + remainingSeconds);
                try {
                    wait(1000);
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

    private synchronized List<String> getMissingIntents(Set<String> requiredIntents) {
        List<String> unsupportedIntents = new ArrayList<String>();
        unsupportedIntents.clear();
        for (String ri : requiredIntents) {
            if (!intentMap.containsKey(ri)) {
                unsupportedIntents.add(ri);
            }
        }
        return unsupportedIntents;
    }

}
