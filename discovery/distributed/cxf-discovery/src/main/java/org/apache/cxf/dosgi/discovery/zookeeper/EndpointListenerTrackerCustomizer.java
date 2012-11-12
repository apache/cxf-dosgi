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
package org.apache.cxf.dosgi.discovery.zookeeper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks interest in EndpointListeners. Delegates to InterfaceMonitorManager to manage 
 * interest in the scopes of each EndpointListener
 */
public class EndpointListenerTrackerCustomizer implements ServiceTrackerCustomizer {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointListenerTrackerCustomizer.class);
    private static final Pattern OBJECTCLASS_PATTERN = Pattern.compile(".*\\(objectClass=([^)]+)\\).*");

    private InterfaceMonitorManager imManager;

    public EndpointListenerTrackerCustomizer(BundleContext bc, InterfaceMonitorManager imManager) {
        this.imManager = imManager;
    }

    public Object addingService(ServiceReference sref) {
        handleEndpointListener(sref);
        return sref;
    }

    public void modifiedService(ServiceReference sref, Object service) {
        handleEndpointListener(sref);
    }

    private void handleEndpointListener(ServiceReference sref) {
        if (isOurOwnEndpointListener(sref)) {
            LOG.debug("Skipping our own endpointListener");
            return;
        }

        if (LOG.isDebugEnabled()) {
            for (String key : sref.getPropertyKeys()) {
                LOG.debug("modifiedService: property: " + key + " => " + sref.getProperty(key));
            }
        }
        
        String[] scopes = Util.getScopes(sref);
        for (String scope : scopes) {
            String objClass = getObjectClass(scope);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding interest in scope: " + scope + " objectClass: " + objClass);
            }
            imManager.addInterest(sref, scope, objClass);
        }
    }

    private boolean isOurOwnEndpointListener(ServiceReference sref) {
        return Boolean.parseBoolean(String.valueOf(
                sref.getProperty(PublishingEndpointListenerFactory.DISCOVERY_ZOOKEEPER_ID)));
    }

    private String getObjectClass(String scope) {
        if (scope == null) {
            return null;
        }
        Matcher m = OBJECTCLASS_PATTERN.matcher(scope);
        return m.matches() ? m.group(1) : null;
    }

    public void removedService(ServiceReference sref, Object service) {
        LOG.info("removedService: " + sref);
        imManager.removeInterest(sref);
    }

}
