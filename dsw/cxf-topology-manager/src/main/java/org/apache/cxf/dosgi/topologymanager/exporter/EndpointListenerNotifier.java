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
package org.apache.cxf.dosgi.topologymanager.exporter;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks EndpointListeners and allows to notify them of endpoints.
 */
public class EndpointListenerNotifier {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointListenerNotifier.class);
    public enum NotifyType { ADDED, REMOVED };
    private Map<EndpointListener, Set<Filter>> listeners;
    private EndpointRepository endpointRepo;

    public EndpointListenerNotifier(final EndpointRepository endpointRepo) {
        this.endpointRepo = endpointRepo;
        this.listeners = new ConcurrentHashMap<EndpointListener, Set<Filter>>();
    }

    public void add(EndpointListener ep, Set<Filter> filters) {
        LOG.debug("new EndpointListener detected");
        listeners.put(ep, filters);
        notifyListener(NotifyType.ADDED, ep, filters, endpointRepo.getAllEndpoints());
    }
    
    public void remove(EndpointListener ep) {
        LOG.debug("EndpointListener modified");
        listeners.remove(ep);
    }
    
    /**
     * Notifies all endpoint listeners about endpoints being added or removed.
     *
     * @param added specifies whether endpoints were added (true) or removed (false)
     * @param endpoints the endpoints the listeners should be notified about
     */
    public void notifyListeners(NotifyType type, Collection<EndpointDescription> endpoints) {
        if (endpoints.isEmpty()) { // a little optimization to prevent unnecessary processing
            return;
        }
        for (EndpointListener listener : listeners.keySet()) {
            notifyListener(type, listener, listeners.get(listener), endpoints);
        }
    }

    /**
     * Notifies an endpoint listener about endpoints being added or removed.
     *
     * @param type specifies whether endpoints were added (true) or removed (false)
     * @param endpointListenerRef the ServiceReference of an EndpointListener to notify
     * @param endpoints the endpoints the listener should be notified about
     */
    void notifyListener(NotifyType type, EndpointListener listener, Set<Filter> filters, 
                        Collection<EndpointDescription> endpoints) {
        LOG.debug("Endpoint {}", type);
        for (EndpointDescription endpoint : endpoints) {
            Set<Filter> matchingFilters = getMatchingFilters(filters, endpoint);
            for (Filter filter : matchingFilters) {
                if (type == NotifyType.ADDED) {
                    listener.endpointAdded(endpoint, filter.toString());
                } else {
                    listener.endpointRemoved(endpoint, filter.toString());
                }
            }
        }
    }

    /**
     * Retrieves an endpoint's properties as a Dictionary.
     *
     * @param endpoint an endpoint description
     * @return endpoint properties (will never return null)
     */
    private static Dictionary<String, Object> getEndpointProperties(EndpointDescription endpoint) {
        if (endpoint == null || endpoint.getProperties() == null) {
            return new Hashtable<String, Object>();
        } else {
            return new Hashtable<String, Object>(endpoint.getProperties());
        }
    }

    public static Set<Filter> getFiltersFromEndpointListenerScope(ServiceReference<EndpointListener> sref) {
        Set<Filter> filters = new HashSet<Filter>();
        String[] scopes = StringPlus.parse(sref.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE));
        for (String scope : scopes) {
            try {
                filters.add(FrameworkUtil.createFilter(scope));
            } catch (InvalidSyntaxException e) {
                LOG.error("invalid endpoint listener scope: {}", scope, e);
            }
        }
        return filters;
    }

    private static Set<Filter> getMatchingFilters(Set<Filter> filters, EndpointDescription endpoint) {
        Set<Filter> matchingFilters = new HashSet<Filter>();
        Dictionary<String, Object> dict = EndpointListenerNotifier.getEndpointProperties(endpoint);
        for (Filter filter : filters) {
            if (filter.match(dict)) {
                LOG.debug("Filter {} matches endpoint {}", filter, dict);
                matchingFilters.add(filter);
            } else {
                LOG.trace("Filter {} does not match endpoint {}", filter, dict);
            }
        }
        return matchingFilters;
    }
}
