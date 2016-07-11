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
package org.apache.cxf.dosgi.common.intent.impl;

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
import org.apache.cxf.dosgi.common.intent.IntentHandler;
import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.dosgi.common.intent.IntentProvider;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
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

    @Override
    public synchronized void applyIntents(AbstractEndpointFactory factory,
                                              Set<String> requiredIntents,
                                              IntentHandler... handlers)
        throws IntentUnsatisfiedException {
        Set<String> missingIntents = getMissingIntents(requiredIntents);
        if (!missingIntents.isEmpty()) {
            throw new IntentUnsatisfiedException(missingIntents.iterator().next()); 
        }
        List<IntentHandler> allHandlers = new ArrayList<IntentHandler>();
        allHandlers.add(new DefaultIntentsHandler());
        allHandlers.addAll(Arrays.asList(handlers));
        for (String intentName : requiredIntents) {
            Object intent = intentMap.get(intentName);
            if (intent instanceof IntentProvider) {
                applyIntentProvider(factory, intentName, (IntentProvider)intent, allHandlers);
            } else {
                applyIntent(factory, intentName, intent, allHandlers);
            }
        }
    }

    private void applyIntentProvider(AbstractEndpointFactory factory, 
                                     String intentName, 
                                     IntentProvider intentProvider,
                                     List<IntentHandler> handlers) {
        for (Object intent : intentProvider.getIntents()) {
            applyIntent(factory, intentName, intent, handlers);
        }
        
    }

    private void applyIntent(AbstractEndpointFactory factory, String intentName, Object intent, 
                             List<IntentHandler> handlers) {
        for (IntentHandler handler : handlers) {
            if (handler.apply(factory, intentName, intent)) {
                return;
            }
        }
        LOG.info("No mapping for intent: " + intentName);
        throw new IntentUnsatisfiedException(intentName);
    }

    public synchronized String[] assertAllIntentsSupported(Set<String> requiredIntents) {
        long endTime = System.currentTimeMillis() + maxIntentWaitTime;
        Set<String> unsupportedIntents;
        do {
            unsupportedIntents = getMissingIntents(requiredIntents);
            long remainingSeconds = (endTime - System.currentTimeMillis()) / 1000;
            if (!unsupportedIntents.isEmpty() && remainingSeconds > 0) {
                LOG.info("Waiting for custom intents " + Arrays.toString(unsupportedIntents.toArray()) + " timeout in "
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
        return requiredIntents.toArray(new String[]{});
    }

    private synchronized Set<String> getMissingIntents(Collection<String> requiredIntents) {
        Set<String> unsupportedIntents = new HashSet<String>();
        unsupportedIntents.clear();
        for (String ri : requiredIntents) {
            if (!intentMap.containsKey(ri)) {
                unsupportedIntents.add(ri);
            }
        }
        return unsupportedIntents;
    }

}
