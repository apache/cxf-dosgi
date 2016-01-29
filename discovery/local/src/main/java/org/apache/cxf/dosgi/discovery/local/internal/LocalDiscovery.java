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
package org.apache.cxf.dosgi.discovery.local.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.dosgi.discovery.local.util.Utils;
import org.apache.cxf.dosgi.endpointdesc.EndpointDescriptionBundleParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.ServiceTracker;

public class LocalDiscovery implements BundleListener {

    // this is effectively a set which allows for multiple service descriptions with the
    // same interface name but different properties and takes care of itself with respect to concurrency
    ConcurrentHashMap<EndpointDescription, Bundle> endpointDescriptions =
        new ConcurrentHashMap<EndpointDescription, Bundle>();
    Map<EndpointListener, Collection<String>> listenerToFilters =
        new HashMap<EndpointListener, Collection<String>>();
    Map<String, Collection<EndpointListener>> filterToListeners =
        new HashMap<String, Collection<EndpointListener>>();
    final BundleContext bundleContext;

    EndpointDescriptionBundleParser bundleParser;
    ServiceTracker<EndpointListener, EndpointListener> listenerTracker;

    public LocalDiscovery(BundleContext bc) {
        this.bundleParser = new EndpointDescriptionBundleParser();
        bundleContext = bc;

        listenerTracker = new ServiceTracker<EndpointListener, EndpointListener>(bundleContext, 
            EndpointListener.class, null) {

            @Override
            public EndpointListener addingService(ServiceReference<EndpointListener> reference) {
                EndpointListener service = super.addingService(reference);
                addListener(reference, service);
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<EndpointListener> reference, EndpointListener service) {
                super.modifiedService(reference, service);
                removeListener(service);

                // This may cause duplicate registrations of remote services,
                // but that's fine and should be filtered out on another level.
                // See Remote Service Admin spec section 122.6.3
                addListener(reference, service);
            }

            @Override
            public void removedService(ServiceReference<EndpointListener> reference, EndpointListener service) {
                super.removedService(reference, service);
                removeListener(service);
            }
        };
        listenerTracker.open();

        bundleContext.addBundleListener(this);
        processExistingBundles();
    }

    private void processExistingBundles() {
        Bundle[] bundles = bundleContext.getBundles();
        if (bundles == null) {
            return;
        }

        for (Bundle b : bundles) {
            if (b.getState() == Bundle.ACTIVE) {
                findDeclaredRemoteServices(b);
            }
        }
    }

    void addListener(ServiceReference<EndpointListener> endpointListenerRef, EndpointListener endpointListener) {
        List<String> filters = Utils.getStringPlusProperty(endpointListenerRef,
                EndpointListener.ENDPOINT_LISTENER_SCOPE);
        if (filters.isEmpty()) {
            return;
        }

        synchronized (listenerToFilters) {
            listenerToFilters.put(endpointListener, filters);
            for (String filter : filters) {
                Collection<EndpointListener> listeners = filterToListeners.get(filter);
                if (listeners == null) {
                    listeners = new ArrayList<EndpointListener>();
                    filterToListeners.put(filter, listeners);
                }
                listeners.add(endpointListener);
            }
        }

        triggerCallbacks(filters, endpointListener);
    }

    /**
     * If the tracker was removed or the scope was changed this doesn't require
     * additional callbacks on the tracker. Its the responsibility of the tracker
     * itself to clean up any orphans. See Remote Service Admin spec 122.6.3
     * @param endpointListener
     */
    void removeListener(EndpointListener endpointListener) {
        synchronized (listenerToFilters) {
            Collection<String> filters = listenerToFilters.remove(endpointListener);
            if (filters == null) {
                return;
            }

            for (String filter : filters) {
                Collection<EndpointListener> listeners = filterToListeners.get(filter);
                if (listeners != null) {
                    listeners.remove(endpointListener);
                    if (listeners.isEmpty()) {
                        filterToListeners.remove(filter);
                    }
                }
            }
        }
    }

    private Map<String, Collection<EndpointListener>> getMatchingListeners(EndpointDescription endpoint) {
        // return a copy of matched filters/listeners so that caller doesn't need to hold locks while triggering events
        Map<String, Collection<EndpointListener>> matched = new HashMap<String, Collection<EndpointListener>>();
        synchronized (listenerToFilters) {
            for (Entry<String, Collection<EndpointListener>> entry : filterToListeners.entrySet()) {
                String filter = entry.getKey();
                if (Utils.matchFilter(bundleContext, filter, endpoint)) {
                    matched.put(filter, new ArrayList<EndpointListener>(entry.getValue()));
                }
            }
        }
        return matched;
    }

    public void shutDown() {
        bundleContext.removeBundleListener(this);
        listenerTracker.close();
    }

    // BundleListener method
    public void bundleChanged(BundleEvent be) {
        switch (be.getType()) {
        case BundleEvent.STARTED:
            findDeclaredRemoteServices(be.getBundle());
            break;
        case BundleEvent.STOPPED:
            removeServicesDeclaredInBundle(be.getBundle());
            break;
        default:
        }
    }

    private void findDeclaredRemoteServices(Bundle bundle) {
        List<EndpointDescription> endpoints = bundleParser.getAllEndpointDescriptions(bundle);
        for (EndpointDescription endpoint : endpoints) {
            endpointDescriptions.put(endpoint, bundle);
            addedEndpointDescription(endpoint);
        }
    }

    private void removeServicesDeclaredInBundle(Bundle bundle) {
        for (Iterator<Entry<EndpointDescription, Bundle>> i = endpointDescriptions.entrySet().iterator();
            i.hasNext();) {
            Entry<EndpointDescription, Bundle> entry = i.next();
            if (bundle.equals(entry.getValue())) {
                removedEndpointDescription(entry.getKey());
                i.remove();
            }
        }
    }

    private void addedEndpointDescription(EndpointDescription endpoint) {
        triggerCallbacks(endpoint, true);
    }

    private void removedEndpointDescription(EndpointDescription endpoint) {
        triggerCallbacks(endpoint, false);
    }

    private void triggerCallbacks(EndpointDescription endpoint, boolean added) {
        for (Map.Entry<String, Collection<EndpointListener>> entry : getMatchingListeners(endpoint).entrySet()) {
            String filter = entry.getKey();
            for (EndpointListener listener : entry.getValue()) {
                triggerCallbacks(listener, filter, endpoint, added);
            }
        }
    }

    private void triggerCallbacks(EndpointListener endpointListener, String filter,
            EndpointDescription endpoint, boolean added) {
        if (!Utils.matchFilter(bundleContext, filter, endpoint)) {
            return;
        }

        if (added) {
            endpointListener.endpointAdded(endpoint, filter);
        } else {
            endpointListener.endpointRemoved(endpoint, filter);
        }
    }

    private void triggerCallbacks(Collection<String> filters, EndpointListener endpointListener) {
        for (String filter : filters) {
            for (EndpointDescription endpoint : endpointDescriptions.keySet()) {
                triggerCallbacks(endpointListener, filter, endpoint, true);
            }
        }
    }

}
