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

import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.dosgi.common.util.OsgiUtils;
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
    public synchronized List<Object> getRequiredIntents(Set<String> requiredIntents) {
        String[] intentNames = assertAllIntentsSupported(requiredIntents);
        List<Object> intents = new ArrayList<Object>();
        for (String intentName : intentNames) {
            Object intent = intentMap.get(intentName);
            if (intent instanceof Callable<?>) {
                try {
                    List<Object> curIntents = ((Callable<List<Object>>)intent).call();
                    intents.addAll(curIntents);
                } catch (Exception e) {
                    throw new RuntimeException(e); 
                }
                
            } else {
                intents.add(intent);
            }
        }
        return intents;
    }
    
    public <T> T getIntent(Class<? extends T> type, List<Object> intents) {
        List<T> selectedIntents = getIntents(type, intents);
        if (selectedIntents.isEmpty()) {
            return null;
        }
        if (selectedIntents.size() > 1) {
            LOG.warn("More than one intent of type " + type + " present. Using only the first one.");
        }
        return (T)selectedIntents.iterator().next();
    }
    
    public <T> List<T> getIntents(Class<? extends T> type, List<Object> intents) {
        List<T> result = new ArrayList<T>();
        for (Object intent : intents) {
            if (type.isInstance(intent)) {
                result.add(type.cast(intent));
            }
        }
        return result;
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
