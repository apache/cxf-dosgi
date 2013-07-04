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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cxf.dosgi.discovery.local.util.Utils.matchFilter;

/**
 * Manages the EndpointListeners and the scopes they are interested in.
 * For each scope with interested EndpointListeners an InterfaceMonitor is created.
 * The InterfaceMonitor calls back when it detects added or removed external Endpoints.
 * These events are then forwarded to all interested EndpointListeners.
 */
public class InterfaceMonitorManager {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMonitorManager.class);

    private final BundleContext bctx;
    private final ZooKeeper zk;
    // map of EndpointListeners and the scopes they are interested in
    private final Map<ServiceReference, List<String>> endpointListenerScopes =
            new HashMap<ServiceReference, List<String>>();
    // map of scopes and their interest data
    private final Map<String, Interest> interests = new HashMap<String, Interest>();

    protected static class Interest {
        List<ServiceReference> endpointListeners = new CopyOnWriteArrayList<ServiceReference>();
        InterfaceMonitor monitor;
    }

    public InterfaceMonitorManager(BundleContext bctx, ZooKeeper zk) {
        this.bctx = bctx;
        this.zk = zk;
    }

    public void addInterest(ServiceReference endpointListener) {
        for (String scope : Utils.getScopes(endpointListener)) {
            String objClass = Utils.getObjectClass(scope);
            LOG.debug("Adding interest in scope {}, objectClass {}", scope, objClass);
            addInterest(endpointListener, scope, objClass);
        }
    }

    public synchronized void addInterest(ServiceReference endpointListener, String scope, String objClass) {
        // get or create interest for given scope and add listener to it
        Interest interest = interests.get(scope);
        if (interest == null) {
            // create interest, add listener and start monitor
            interest = new Interest();
            interests.put(scope, interest);
            interest.endpointListeners.add(endpointListener); // add it before monitor starts so we don't miss events
            interest.monitor = createInterfaceMonitor(scope, objClass, interest);
            interest.monitor.start();
        } else {
            // interest already exists, so just add listener to it
            if (!interest.endpointListeners.contains(endpointListener)) {
                interest.endpointListeners.add(endpointListener);
            }
            // notify listener of all known endpoints for given scope
            // (as EndpointListener contract requires of all added/modified listeners)
            for (EndpointDescription endpoint : interest.monitor.getEndpoints()) {
                notifyListeners(endpoint, scope, true, Arrays.asList(endpointListener));
            }
        }

        // add scope to listener's scopes list
        List<String> scopes = endpointListenerScopes.get(endpointListener);
        if (scopes == null) {
            scopes = new ArrayList<String>(1);
            endpointListenerScopes.put(endpointListener, scopes);
        }
        if (!scopes.contains(scope)) {
            scopes.add(scope);
        }
    }

    public synchronized void removeInterest(ServiceReference endpointListener) {
        List<String> scopes = endpointListenerScopes.get(endpointListener);
        if (scopes == null) {
            return;
        }

        for (String scope : scopes) {
            Interest interest = interests.get(scope);
            if (interest != null) {
                interest.endpointListeners.remove(endpointListener);
                if (interest.endpointListeners.isEmpty()) {
                    interest.monitor.close();
                    interests.remove(scope);
                }
            }
        }
        endpointListenerScopes.remove(endpointListener);
    }

    private InterfaceMonitor createInterfaceMonitor(final String scope, String objClass, final Interest interest) {
        // holding this object's lock in the callbacks can lead to a deadlock with InterfaceMonitor
        EndpointListener endpointListener = new EndpointListener() {

            public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
                notifyListeners(endpoint, scope, false, interest.endpointListeners);
            }

            public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
                notifyListeners(endpoint, scope, true, interest.endpointListeners);
            }
        };
        return new InterfaceMonitor(zk, objClass, endpointListener, scope);
    }

    private void notifyListeners(EndpointDescription endpoint, String currentScope, boolean isAdded,
            List<ServiceReference> endpointListeners) {
        for (ServiceReference endpointListenerRef : endpointListeners) {
            Object service = bctx.getService(endpointListenerRef);
            try {
                if (service instanceof EndpointListener) {
                    EndpointListener endpointListener = (EndpointListener) service;
                    LOG.trace("matching {} against {}", endpoint, currentScope);
                    if (matchFilter(bctx, currentScope, endpoint)) {
                        LOG.debug("Matched {} against {}", endpoint, currentScope);
                        notifyListener(endpoint, currentScope, isAdded,
                                endpointListenerRef.getBundle(), endpointListener);
                    }
                }
            } finally {
                if (service != null) {
                    bctx.ungetService(endpointListenerRef);
                }
            }
        }
    }

    private void notifyListener(EndpointDescription endpoint, String currentScope, boolean isAdded,
                                Bundle endpointListenerBundle, EndpointListener endpointListener) {
        if (endpointListenerBundle == null) {
            LOG.info("listening service was unregistered, ignoring");
        } else if (isAdded) {
            LOG.info("calling EndpointListener.endpointAdded: " + endpointListener + " from bundle "
                    + endpointListenerBundle.getSymbolicName() + " for endpoint: " + endpoint);
            endpointListener.endpointAdded(endpoint, currentScope);
        } else {
            LOG.info("calling EndpointListener.endpointRemoved: " + endpointListener + " from bundle "
                    + endpointListenerBundle.getSymbolicName() + " for endpoint: " + endpoint);
            endpointListener.endpointRemoved(endpoint, currentScope);
        }
    }

    public synchronized void close() {
        for (Interest interest : interests.values()) {
            interest.monitor.close();
        }
        interests.clear();
        endpointListenerScopes.clear();
    }

    /**
     * Only for test case!
     */
    protected synchronized Map<String, Interest> getInterests() {
        return interests;
    }

    /**
     * Only for test case!
     */
    protected synchronized Map<ServiceReference, List<String>> getEndpointListenerScopes() {
        return endpointListenerScopes;
    }
}
