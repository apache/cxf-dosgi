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
    private final ZooKeeper zooKeeper;
    // map of EndpointListeners and the scopes they are interested in
    private final Map<ServiceReference, List<String>> listenerScopes = new HashMap<ServiceReference, List<String>>();
    // map of scopes and their interest data
    private final Map<String, Interest> interests = new HashMap<String, Interest>();

    protected static class Interest {
        List<ServiceReference> listeners = new CopyOnWriteArrayList<ServiceReference>();
        InterfaceMonitor im;
    }

    public InterfaceMonitorManager(BundleContext bctx, ZooKeeper zooKeeper) {
        this.bctx = bctx;
        this.zooKeeper = zooKeeper;
    }

    public void addInterest(ServiceReference sref) {
        for (String scope : Utils.getScopes(sref)) {
            String objClass = Utils.getObjectClass(scope);
            LOG.debug("Adding interest in scope {}, objectClass {}", scope, objClass);
            addInterest(sref, scope, objClass);
        }
    }

    public synchronized void addInterest(ServiceReference sref, String scope, String objClass) {
        // get or create interest for given scope and add listener to it
        Interest interest = interests.get(scope);
        if (interest == null) {
            // create interest, add listener and start monitor
            interest = new Interest();
            interests.put(scope, interest);
            interest.listeners.add(sref); // add it before monitor starts so we don't miss events
            interest.im = createInterfaceMonitor(scope, objClass, interest);
            interest.im.start();
        } else if (!interest.listeners.contains(sref)) {
            // interest already exists, so just add listener to it
            interest.listeners.add(sref);
        }

        // add scope to listener's scopes list
        List<String> scopes = listenerScopes.get(sref);
        if (scopes == null) {
            scopes = new ArrayList<String>(1);
            listenerScopes.put(sref, scopes);
        }
        if (!scopes.contains(scope)) {
            scopes.add(scope);
        }
    }

    public synchronized void removeInterest(ServiceReference sref) {
        List<String> scopes = listenerScopes.get(sref);
        if (scopes == null) {
            return;
        }

        for (String scope : scopes) {
            Interest interest = interests.get(scope);
            if (interest != null) {
                interest.listeners.remove(sref);
                if (interest.listeners.isEmpty()) {
                    interest.im.close();
                    interests.remove(scope);
                }
            }
        }
        listenerScopes.remove(sref);
    }

    private InterfaceMonitor createInterfaceMonitor(final String scope, String objClass, final Interest interest) {
        // holding this object's lock in the callbacks can lead to a deadlock with InterfaceMonitor
        EndpointListener listener = new EndpointListener() {

            public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
                notifyListeners(endpoint, scope, false, interest.listeners);
            }

            public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
                notifyListeners(endpoint, scope, true, interest.listeners);
            }
        };
        return new InterfaceMonitor(zooKeeper, objClass, listener, scope);
    }

    private void notifyListeners(EndpointDescription epd, String currentScope, boolean isAdded,
            List<ServiceReference> listeners) {
        for (ServiceReference sref : listeners) {
            Object service = bctx.getService(sref);
            try {
                if (service instanceof EndpointListener) {
                    EndpointListener epl = (EndpointListener) service;
                    LOG.trace("matching {} against {}", epd, currentScope);
                    if (matchFilter(bctx, currentScope, epd)) {
                        LOG.debug("Matched {} against {}", epd, currentScope);
                        notifyListener(epd, currentScope, isAdded, sref.getBundle(), epl);
                    }
                }
            } finally {
                if (service != null) {
                    bctx.ungetService(sref);
                }
            }
        }
    }

    private void notifyListener(EndpointDescription epd, String currentScope, boolean isAdded,
                                Bundle eplBundle, EndpointListener epl) {
        if (eplBundle == null) {
            LOG.info("listening service was unregistered, ignoring");
        } else if (isAdded) {
            LOG.info("calling EndpointListener.endpointAdded: " + epl + " from bundle "
                    + eplBundle.getSymbolicName() + " for endpoint: " + epd);
            epl.endpointAdded(epd, currentScope);
        } else {
            LOG.info("calling EndpointListener.endpointRemoved: " + epl + " from bundle "
                    + eplBundle.getSymbolicName() + " for endpoint: " + epd);
            epl.endpointRemoved(epd, currentScope);
        }
    }

    public synchronized void close() {
        for (Interest interest : interests.values()) {
            interest.im.close();
        }
        interests.clear();
        listenerScopes.clear();
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
    protected synchronized Map<ServiceReference, List<String>> getListenerScopes() {
        return listenerScopes;
    }
}
