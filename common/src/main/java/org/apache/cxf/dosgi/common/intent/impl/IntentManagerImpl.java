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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.aries.rsa.spi.IntentUnsatisfiedException;
import org.apache.cxf.dosgi.common.intent.IntentHandler;
import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.dosgi.common.intent.IntentProvider;
import org.apache.cxf.dosgi.common.util.OsgiUtils;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IntentManager.class)
public class IntentManagerImpl implements IntentManager {

    static final Logger LOG = LoggerFactory.getLogger(IntentManagerImpl.class);
    private static final int DEFAULT_INTENT_TIMEOUT = 30000;

    private final Map<String, Object> intentMap = new HashMap<String, Object>();
    private final long maxIntentWaitTime = DEFAULT_INTENT_TIMEOUT;
    private ServiceTracker<Object, Object> tracker;

    @Activate
    public void activate(BundleContext context) throws InvalidSyntaxException {
        Filter filter = FrameworkUtil.createFilter("(" + IntentManager.INTENT_NAME_PROP + "=*)");
        tracker = new ServiceTracker<Object, Object>(context, filter, null) {
            @Override
            public Object addingService(ServiceReference<Object> reference) {
                Object intent = super.addingService(reference);
                addIntent(intent, (String)reference.getProperty(INTENT_NAME_PROP));
                return intent;
            }
            
            @Override
            public void removedService(ServiceReference<Object> reference, Object intent) {
                removeIntent(intent, (String)reference.getProperty(INTENT_NAME_PROP));
                super.removedService(reference, intent);
            }
        };
        tracker.open();
    }
    
    @Deactivate
    public void deactivate() {
        tracker.close();
    }

    public synchronized void addIntent(Object intent, String intentName) {
        LOG.info("Adding custom intent " + intentName);
        intentMap.put(intentName, intent);
    }

    public synchronized void removeIntent(Object intent, String intentName) {
        intentMap.remove(intentName);
    }

    @SuppressWarnings("unchecked")
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
            if (intent instanceof Callable<?>) {
                try {
                    List<Object> intents = ((Callable<List<Object>>)intent).call();
                    applyIntents(factory, intentName, intents, allHandlers);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (intent instanceof IntentProvider) {
                applyIntents(factory, intentName, ((IntentProvider)intent).getIntents(), allHandlers);
            } else {
                applyIntent(factory, intentName, intent, allHandlers);
            }
        }
    }

    private void applyIntents(AbstractEndpointFactory factory, 
                                     String intentName, 
                                     List<Object> intents,
                                     List<IntentHandler> handlers) {
        for (Object intent : intents) {
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
        boolean first = true;
        do {
            unsupportedIntents = getMissingIntents(requiredIntents);
            long remainingSeconds = (endTime - System.currentTimeMillis()) / 1000;
            if (!unsupportedIntents.isEmpty() && remainingSeconds > 0) {
                String msg = "Waiting for custom intents {} timeout in {} seconds";
                if (first) {
                    LOG.info(msg, Arrays.toString(unsupportedIntents.toArray()), remainingSeconds);
                    first = false;
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(msg, Arrays.toString(unsupportedIntents.toArray()), remainingSeconds);
                    }
                }
                
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
    
    public Set<String> getExported(Map<String, Object> sd) {
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
    
    public Set<String> getImported(Map<String, Object> sd) {
        Collection<String> intents = OsgiUtils.getMultiValueProperty(sd.get(RemoteConstants.SERVICE_INTENTS));
        return intents == null ? new HashSet<String>() : new HashSet<String>(intents);
    }
    
    private static Collection<String> parseIntents(Collection<String> intents) {
        List<String> parsed = new ArrayList<String>();
        for (String intent : intents) {
            parsed.addAll(Arrays.asList(intent.split("[ ]")));
        }
        return parsed;
    }

}
