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
package org.apache.cxf.dosgi.dsw.qos;

import org.apache.cxf.dosgi.dsw.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class IntentTracker extends ServiceTracker {
    private final IntentMap intentMap;

    public IntentTracker(BundleContext context, IntentMap intentMap) {
        super(context, getFilter(context), null);
        this.intentMap = intentMap;
    }
    
    static Filter getFilter(BundleContext context) {
        try {
            return context.createFilter("(" + Constants.INTENT_NAME_PROP + "=*)");
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Object addingService(ServiceReference reference) {
        String intentName = (String) reference.getProperty(Constants.INTENT_NAME_PROP);
        Object intent = context.getService(reference);
        IntentManagerImpl.LOG.info("Adding custom intent " + intentName);
        intentMap.put(intentName, intent);
        return super.addingService(reference);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        String intentName = (String) reference.getProperty(Constants.INTENT_NAME_PROP);
        intentMap.remove(intentName);
        super.removedService(reference, service);
    }
}