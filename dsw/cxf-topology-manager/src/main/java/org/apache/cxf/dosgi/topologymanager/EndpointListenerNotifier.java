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
package org.apache.cxf.dosgi.topologymanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks EndpointListeners and allows to notify them of endpoints
 */
public class EndpointListenerNotifier {
    private final static Logger LOG = Logger.getLogger(EndpointListenerNotifier.class.getName());
    private BundleContext bctx;
    private ServiceTracker stEndpointListeners;
    private ExportRepository exportRepository;

    public EndpointListenerNotifier(BundleContext bctx, ExportRepository exportRepository) {
        this.bctx = bctx;
        this.exportRepository = exportRepository;
        this.stEndpointListeners = new ServiceTracker(bctx, EndpointListener.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.fine("TopologyManager: new EndpointListener that wants to be informed about whats going on ... ");
                notifyListenerOfAllExistingExports(reference);
                return super.addingService(reference);
            }

            @Override
            public void modifiedService(ServiceReference reference, Object service) {
                LOG.fine("TopologyManager: EndpointListener changed ... ");
                notifyListenerOfAllExistingExports(reference);
                super.modifiedService(reference, service);
            }

        };

    }
    
    public void start() {
        stEndpointListeners.open();
    }

    public void stop() {
        stEndpointListeners.close();
    }
    
    private void notifyListenerOfAllExistingExports(
            ServiceReference reference) {
        Collection<ExportRegistration> registrations = exportRepository.getAllExportRegistrations();
        notifyListenerOfAdding(reference, registrations );
    }
    
    void nofifyEndpointListenersOfAdding(Collection<ExportRegistration> exportRegistrations) {
        ServiceReference[] epListeners = getEndpointListeners(bctx);
        for (ServiceReference sref : epListeners) {
            notifyListenerOfAdding(sref, exportRegistrations);
        }
    }
    
    void notifyAllListenersOfRemoval(Collection<ExportRegistration> endpoints) {
        ServiceReference[] refs = getEndpointListeners(bctx);
        for (ServiceReference epListenerReference : refs) {
            notifyListenersOfRemoval(epListenerReference, endpoints);
        }
    }
    
    void notifyListenersOfRemoval(Collection<ExportRegistration> registrations) {
        for (ServiceReference epListenerReference : stEndpointListeners.getServiceReferences()) {
            notifyListenersOfRemoval(epListenerReference, registrations);
        }
    }
    
    /**
     * Notifies the listener if he is interested in the provided registrations
     * 
     * @param sref The ServiceReference for an EndpointListener
     * @param exportRegistrations the registrations, the listener should be informed about
     */
    private void notifyListenerOfAdding(ServiceReference epListenerReference,
                                        Collection<ExportRegistration> exportRegistrations) {
        EndpointListener epl = (EndpointListener)bctx.getService(epListenerReference);
        List<Filter> filters = getFiltersFromEndpointListenerScope(epListenerReference, bctx);

        LOG.finer("TopologyManager: notifyListenerOfAddingIfAppropriate() ");
        for (ExportRegistration exReg : exportRegistrations) {
            EndpointDescription endpoint = getExportedEndpoint(exReg);
            List<Filter> matchingFilters = getMatchingFilters(filters, endpoint);
            for (Filter filter : matchingFilters) {
                epl.endpointAdded(endpoint, filter.toString());
            }
        }

    }

    void notifyListenersOfRemoval(ServiceReference epListenerReference,
                                          Collection<ExportRegistration> exportRegistrations) {
        EndpointListener epl = (EndpointListener)bctx.getService(epListenerReference);
        List<Filter> filters = getFiltersFromEndpointListenerScope(epListenerReference, bctx);
        LOG.finer("TopologyManager: notifyListenerOfREMOVALIfAppropriate() ");
        for (ExportRegistration exReg : exportRegistrations) {
            EndpointDescription endpoint = getExportedEndpoint(exReg);
            List<Filter> matchingFilters = getMatchingFilters(filters, endpoint);
            for (Filter filter : matchingFilters) {
                epl.endpointRemoved(endpoint, filter.toString());
            }
        }
    }
    
    static List<Filter> getFiltersFromEndpointListenerScope(ServiceReference sref,BundleContext bctx) {
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
                        LOG.warning("Component of a filter is not a string -> skipped !");
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        return filters;
    }
    
    private List<Filter> getMatchingFilters(List<Filter> filters,
            EndpointDescription endpoint) {
        List<Filter> matchingFilters = new ArrayList<Filter>();
        Dictionary<String, Object> d = getEndpointProperties(endpoint);

        for (Filter filter : filters) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Matching: " + filter + "  against " + d);
            }
            if (filter.match(d)) {
                LOG.fine("Listener matched one of the Endpoints !!!! --> calling removed() ...");
                matchingFilters.add(filter);
            }
        }
        return matchingFilters;
    }

    /** 
     * Find all EndpointListeners; They must have the Scope property otherwise they have to be ignored
     * @param bctx
     * @return
     * @throws InvalidSyntaxException
     */
   private static ServiceReference[] getEndpointListeners(BundleContext bctx) {
       ServiceReference[] result = null;
       try {
           String filter = "(" + EndpointListener.ENDPOINT_LISTENER_SCOPE + "=*)";
           result = bctx.getServiceReferences(EndpointListener.class.getName(), filter);
       } catch (InvalidSyntaxException e) {
           LOG.log(Level.SEVERE, e.getMessage(), e);
       }
       return (result == null) ? new ServiceReference[]{} : result;
   }
   
   /**
    * Retrieve exported Endpoint while handling null
    * @param exReg
    * @return exported Endpoint or null if not present
    */
   private EndpointDescription getExportedEndpoint(ExportRegistration exReg) {
       ExportReference ref = (exReg == null) ? null : exReg.getExportReference();
       return (ref == null) ? null : ref.getExportedEndpoint(); 
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
