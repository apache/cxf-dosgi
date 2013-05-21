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
import java.util.Hashtable;
import java.util.List;

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
 * Tracks EndpointListeners and allows to notify them of endpoints
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
                notifyListenerOfAdding(epListenerRef, endpointRepository.getAllEndpoints());
                return super.addingService(epListenerRef);
            }

            @Override
            public void modifiedService(ServiceReference epListenerRef, Object service) {
                LOG.debug("EndpointListener modified");
                notifyListenerOfAdding(epListenerRef, endpointRepository.getAllEndpoints());
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
    
    void nofifyEndpointListenersOfAdding(Collection<EndpointDescription> endpoints) {
        for (ServiceReference eplistener : stEndpointListeners.getServiceReferences()) {
            notifyListenerOfAdding(eplistener, endpoints);
        }
    }
    
    void notifyListenersOfRemoval(Collection<EndpointDescription> endpoints) {
        for (ServiceReference epListenerReference : stEndpointListeners.getServiceReferences()) {
            notifyListenerOfRemoval(epListenerReference, endpoints);
        }
    }
    
    /**
     * Notifies the listener if he is interested in the provided registrations
     * 
     * @param sref The ServiceReference for an EndpointListener
     * @param endpoints the registrations, the listener should be informed about
     */
    private void notifyListenerOfAdding(ServiceReference epListenerReference,
                                        Collection<EndpointDescription> endpoints) {
        EndpointListener epl = (EndpointListener)bctx.getService(epListenerReference);
        List<Filter> filters = getFiltersFromEndpointListenerScope(epListenerReference, bctx);

        LOG.debug("notifyListenerOfAdding");
        for (EndpointDescription endpoint : endpoints) {
            List<Filter> matchingFilters = getMatchingFilters(filters, endpoint);
            for (Filter filter : matchingFilters) {
                epl.endpointAdded(endpoint, filter.toString());
            }
        }

    }

    void notifyListenerOfRemoval(ServiceReference epListenerReference,
                                          Collection<EndpointDescription> endpoints) {
        EndpointListener epl = (EndpointListener)bctx.getService(epListenerReference);
        List<Filter> filters = getFiltersFromEndpointListenerScope(epListenerReference, bctx);
        for (EndpointDescription endpoint : endpoints) {
            List<Filter> matchingFilters = getMatchingFilters(filters, endpoint);
            for (Filter filter : matchingFilters) {
                epl.endpointRemoved(endpoint, filter.toString());
            }
        }
    }
    
    static List<Filter> getFiltersFromEndpointListenerScope(ServiceReference sref, BundleContext bctx) {
        List<Filter> filters = new ArrayList<Filter>();
        try {
            Object fo = sref.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE);
            if (fo instanceof String) {
                filters.add(bctx.createFilter((String) fo));
            } else if (fo instanceof String[]) {
                String[] foArray = (String[]) fo;
                for (String f : foArray) {
                    filters.add(bctx.createFilter(f));
                }
            } else if (fo instanceof Collection) {
                @SuppressWarnings("rawtypes")
                Collection c = (Collection) fo;
                for (Object o : c) {
                    if (o instanceof String) {
                        filters.add(bctx.createFilter((String) o));
                    } else {
                        LOG.warn("Component of a EndpointListener filter is not a string -> skipped !");
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            LOG.error(e.getMessage(), e);
        }
        return filters;
    }
    
    private List<Filter> getMatchingFilters(List<Filter> filters,
            EndpointDescription endpoint) {
        List<Filter> matchingFilters = new ArrayList<Filter>();
        Dictionary<String, Object> d = getEndpointProperties(endpoint);

        for (Filter filter : filters) {
            if (filter.match(d)) {
                LOG.debug("Filter {} matches endpoint {}", filter, d);
                matchingFilters.add(filter);
            } else {
                LOG.debug("Filter {} does not match endpoint {}", filter, d);
            }
        }
        return matchingFilters;
    }
   
    /**
     * Retrieve endpoint properties as Dictionary
     * 
     * @param ep
     * @return endpoint properties (will never return null) 
     */
    private Dictionary<String, Object> getEndpointProperties(EndpointDescription ep) {
        if (ep == null || ep.getProperties() == null) {
            return new Hashtable<String, Object>();
        } else {
            return new Hashtable<String, Object>(ep.getProperties());
        }
    }
}
