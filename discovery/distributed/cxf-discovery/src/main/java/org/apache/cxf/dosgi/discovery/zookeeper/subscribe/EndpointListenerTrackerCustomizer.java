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
package org.apache.cxf.dosgi.discovery.zookeeper.subscribe;

import org.apache.cxf.dosgi.discovery.zookeeper.ZooKeeperDiscovery;
import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks interest in EndpointListeners. Delegates to InterfaceMonitorManager to manage
 * interest in the scopes of each EndpointListener.
 */
public class EndpointListenerTrackerCustomizer implements ServiceTrackerCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointListenerTrackerCustomizer.class);

    private final InterfaceMonitorManager imManager;

    public EndpointListenerTrackerCustomizer(InterfaceMonitorManager imManager) {
        this.imManager = imManager;
    }

    public Object addingService(ServiceReference endpointListener) {
        updateListenerScopes(endpointListener);
        return endpointListener;
    }

    public void modifiedService(ServiceReference endpointListener, Object service) {
        // called when an EndpointListener updates its service properties,
        // e.g. when its interest scope is expanded/reduced
        updateListenerScopes(endpointListener);
    }

    public void removedService(ServiceReference endpointListener, Object service) {
        LOG.info("removing EndpointListener interests: {}", endpointListener);
        imManager.removeInterest(endpointListener);
    }

    private void updateListenerScopes(ServiceReference endpointListener) {
        if (isOurOwnEndpointListener(endpointListener)) {
            LOG.debug("Skipping our own EndpointListener");
            return;
        }

        LOG.info("updating EndpointListener interests: {}", endpointListener);
        if (LOG.isDebugEnabled()) {
            LOG.debug("updated EndpointListener properties: {}", Utils.getProperties(endpointListener));
        }

        imManager.addInterest(endpointListener);
    }

    private static boolean isOurOwnEndpointListener(ServiceReference endpointListener) {
        return Boolean.parseBoolean(String.valueOf(
                endpointListener.getProperty(ZooKeeperDiscovery.DISCOVERY_ZOOKEEPER_ID)));
    }
}
