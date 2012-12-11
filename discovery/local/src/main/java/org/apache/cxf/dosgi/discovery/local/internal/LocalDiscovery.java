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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.dosgi.discovery.local.LocalDiscoveryUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalDiscovery implements BundleListener {   
    private static final Logger LOG = LoggerFactory.getLogger(LocalDiscovery.class);
    
    // this is effectively a set which allows for multiple service descriptions with the
    // same interface name but different properties and takes care of itself with respect to concurrency 
    ConcurrentHashMap<EndpointDescription, Bundle> endpointDescriptions = 
        new ConcurrentHashMap<EndpointDescription, Bundle>();
    Map<EndpointListener, Collection<String>> listenerToFilters = 
        new HashMap<EndpointListener, Collection<String>>();
    Map<String, Collection<EndpointListener>> filterToListeners = 
        new HashMap<String, Collection<EndpointListener>>();
    final BundleContext bundleContext;

    ServiceTracker listenerTracker;

    public LocalDiscovery(BundleContext bc) {
        bundleContext = bc;
        
        listenerTracker = new ServiceTracker(bundleContext, EndpointListener.class.getName(), null) {

            @Override
            public Object addingService(ServiceReference reference) {
                Object svc = super.addingService(reference);
                registerTracker(reference, svc);
                return svc;
            }

            @Override
            public void modifiedService(ServiceReference reference, Object service) {
                super.modifiedService(reference, service);
                clearTracker(service);
                
                // This may cause duplicate registrations of remote services,
                // but that's fine and should be filtered out on another level.
                // See Remove Service Admin spec section 122.6.3
                registerTracker(reference, service);
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                super.removedService(reference, service);
                clearTracker(service);
            }
            
        };
        listenerTracker.open();
        
        bundleContext.addBundleListener(this);
        processExistingBundles();
    }

    private void processExistingBundles() {
        Bundle [] bundles = bundleContext.getBundles();
        if (bundles == null) {
            return;
        }
        
        for (Bundle b : bundles) {
            if (b.getState() == Bundle.ACTIVE) {
                findDeclaredRemoteServices(b);
            }
        }
    }

    void registerTracker(ServiceReference reference, Object svc) {
        if (svc instanceof EndpointListener) {
            EndpointListener listener = (EndpointListener) svc;
            Collection<String> filters = addListener(reference, listener);
            triggerCallbacks(filters, listener);
        }
    }

    void clearTracker(Object svc) {
        if (svc instanceof EndpointListener) {
            EndpointListener listener = (EndpointListener) svc;
            removeListener(listener);
            // If the tracker was removed or the scope was changed this doesn't require 
            // additional callbacks on the tracker. Its the responsibility of the tracker
            // itself to clean up any orphans. See Remote Service Admin spec 122.6.3
        }
    }

    private Collection<String> addListener(ServiceReference reference,
            EndpointListener listener) {
        List<String> filters = 
            LocalDiscoveryUtils.getStringPlusProperty(reference, EndpointListener.ENDPOINT_LISTENER_SCOPE);
        if (filters.size() == 0) {
            return filters;
        }
        
        listenerToFilters.put(listener, filters);
        for (String filter : filters) {
            if (filterToListeners.containsKey(filter)) {
                filterToListeners.get(filter).add(listener);
            } else {
                List<EndpointListener> list = new ArrayList<EndpointListener>();
                list.add(listener);
                filterToListeners.put(filter, list);
            }
        }
        
        return filters;
    }
    
    private void removeListener(EndpointListener listener) {
        Collection<String> filters = listenerToFilters.remove(listener);
        if (filters == null) {
            return;
        }
        
        for (String filter : filters) {
            Collection<EndpointListener> listeners = filterToListeners.get(filter);
            if (listeners == null) {
                continue;
            }
            listeners.remove(listener);
        }        
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
        List<EndpointDescription> eds = LocalDiscoveryUtils.getAllEndpointDescriptions(bundle);
        for (EndpointDescription ed : eds) {
            endpointDescriptions.put(ed, bundle);
            addedEndpointDescription(ed);
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

    private void addedEndpointDescription(EndpointDescription ed) {
        triggerCallbacks(ed, true);
    }

    private void removedEndpointDescription(EndpointDescription ed) {
        triggerCallbacks(ed, false);
    }

    private void triggerCallbacks(EndpointDescription ed, boolean added) {
        for (Map.Entry<EndpointListener, Collection<String>> entry : listenerToFilters.entrySet()) {
            for (String match : entry.getValue()) {
                triggerCallbacks(entry.getKey(), match, ed, added);
            }
        }
    }

    private void triggerCallbacks(EndpointListener listener, String toMatch,
            EndpointDescription ed, boolean added) {
        if (!filterMatches(toMatch, ed)) {
            return;
        }
        
        if (added) {
            listener.endpointAdded(ed, toMatch);
        } else {
            listener.endpointRemoved(ed, toMatch);
        }
    }
    
    private void triggerCallbacks(Collection<String> filters, EndpointListener listener) {
        for (String filter : filters) {
            for (EndpointDescription ed : endpointDescriptions.keySet()) {
                triggerCallbacks(listener, filter, ed, true);
            }
        }
    }    

    private boolean filterMatches(String match, EndpointDescription ed) {
        Filter filter = createFilter(match);
        
        Dictionary<String, Object> props = 
            new Hashtable<String, Object>(ed.getProperties());
        
        return filter != null
            ? filter.match(props)
            : false;
    } 
    
    private Filter createFilter(String filterValue) {        
        if (filterValue == null) {
            return null;
        }
        
        try {
            return bundleContext.createFilter(filterValue); 
        } catch (Exception ex) {
            LOG.error("Problem creating a Filter from " + filterValue, ex); 
        }
        return null;
    }    
}
