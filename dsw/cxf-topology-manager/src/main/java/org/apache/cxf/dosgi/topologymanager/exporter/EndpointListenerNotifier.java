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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;

import org.apache.cxf.dosgi.topologymanager.util.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks EndpointListeners and allows to notify them of endpoints.
 */
public class EndpointListenerNotifier {

    private static final String ENDPOINT_LISTENER_FILTER =
        "(&(" + Constants.OBJECTCLASS + "=" + EndpointListener.class.getName() + ")"
        + "(" + EndpointListener.ENDPOINT_LISTENER_SCOPE + "=*))";
    private static final Logger LOG = LoggerFactory.getLogger(EndpointListenerNotifier.class);
    private BundleContext bctx;
    private ServiceTracker stEndpointListeners;

    public EndpointListenerNotifier(BundleContext bctx, final EndpointRepository endpointRepository) {
        this.bctx = bctx;
        Filter filter;
        try {
            filter = bctx.createFilter(ENDPOINT_LISTENER_FILTER);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException("Unexpected exception creating filter", e);
        }
        this.stEndpointListeners = new ServiceTracker(bctx, filter, null) {
            @Override
            public Object addingService(ServiceReference epListenerRef) {
                LOG.debug("new EndpointListener detected");
                // the super.addingService call must come before notifyListener, since we need
                // to keep at least one service reference alive when ungetService is called
                Object service = super.addingService(epListenerRef);
                notifyListener(true, epListenerRef, endpointRepository.getAllEndpoints());
                return service;
            }

            @Override
            public void modifiedService(ServiceReference epListenerRef, Object service) {
                LOG.debug("EndpointListener modified");
                notifyListener(true, epListenerRef, endpointRepository.getAllEndpoints());
                super.modifiedService(epListenerRef, service);
            }
        };
    }

    public void start() {
        stEndpointListeners.open();
    }

    public void stop() {
        stEndpointListeners.close();
    }

    /**
     * Notifies all endpoint listeners about endpoints being added or removed.
     *
     * @param added specifies whether endpoints were added (true) or removed (false)
     * @param endpoints the endpoints the listeners should be notified about
     */
    void notifyListeners(boolean added, Collection<EndpointDescription> endpoints) {
        ServiceReference[] listeners = stEndpointListeners.getServiceReferences();
        if (listeners != null) {
            for (ServiceReference eplReference : listeners) {
                notifyListener(added, eplReference, endpoints);
            }
        }
    }

    /**
     * Notifies an endpoint listener about endpoints being added or removed.
     *
     * @param added specifies whether endpoints were added (true) or removed (false)
     * @param eplReference the ServiceReference of an EndpointListener to notify
     * @param endpoints the endpoints the listener should be notified about
     */
    void notifyListener(boolean added, ServiceReference eplReference,
                                Collection<EndpointDescription> endpoints) {
        List<Filter> filters = getFiltersFromEndpointListenerScope(eplReference, bctx);
        EndpointListener epl = (EndpointListener)bctx.getService(eplReference);
        try {
            LOG.debug("notifyListener (added={})", added);
            for (EndpointDescription endpoint : endpoints) {
                List<Filter> matchingFilters = getMatchingFilters(filters, endpoint);
                for (Filter filter : matchingFilters) {
                    if (added) {
                        epl.endpointAdded(endpoint, filter.toString());
                    } else {
                        epl.endpointRemoved(endpoint, filter.toString());
                    }
                }
            }
        } finally {
            if (epl != null) {
                bctx.ungetService(eplReference);
            }
        }
    }

    static List<Filter> getFiltersFromEndpointListenerScope(ServiceReference sref, BundleContext bctx) {
        List<Filter> filters = new ArrayList<Filter>();
        String[] scopes = Utils.getStringPlusProperty(sref.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE));
        for (String scope : scopes) {
            try {
                filters.add(bctx.createFilter(scope));
            } catch (InvalidSyntaxException e) {
                LOG.error("invalid endpoint listener scope: {}", scope, e);
            }
        }
        return filters;
    }

    private static List<Filter> getMatchingFilters(List<Filter> filters, EndpointDescription endpoint) {
        List<Filter> matchingFilters = new ArrayList<Filter>();
        Dictionary<String, Object> dict = Utils.getEndpointProperties(endpoint);

        for (Filter filter : filters) {
            if (filter.match(dict)) {
                LOG.debug("Filter {} matches endpoint {}", filter, dict);
                matchingFilters.add(filter);
            } else {
                LOG.debug("Filter {} does not match endpoint {}", filter, dict);
            }
        }
        return matchingFilters;
    }
}
