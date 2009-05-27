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
package org.apache.cxf.dosgi.discovery.local;


import static org.osgi.service.discovery.DiscoveredServiceNotification.AVAILABLE;
import static org.osgi.service.discovery.DiscoveredServiceNotification.UNAVAILABLE;
import static org.osgi.service.discovery.DiscoveredServiceTracker.FILTER_MATCH_CRITERIA;
import static org.osgi.service.discovery.DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.Discovery;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.util.tracker.ServiceTracker;

public class LocalDiscoveryService implements Discovery, BundleListener {
    
    private static final Logger LOG = Logger.getLogger(LocalDiscoveryService.class.getName());
        
    // this is effectively a set which allows for multiple service descriptions with the
    // same interface name but different properties and takes care of itself with respect to concurrency 
    ConcurrentHashMap<ServiceEndpointDescription, Bundle> servicesInfo = 
        new ConcurrentHashMap<ServiceEndpointDescription, Bundle>();
    Map<String, List<DiscoveredServiceTracker>> interfacesToTrackers = 
        new HashMap<String, List<DiscoveredServiceTracker>>();
    Map<String, List<DiscoveredServiceTracker>> filtersToTrackers = 
        new HashMap<String, List<DiscoveredServiceTracker>>();
    Map<DiscoveredServiceTracker, Collection<String>> trackersToInterfaces = 
        new HashMap<DiscoveredServiceTracker, Collection<String>>();
    Map<DiscoveredServiceTracker, Collection<String>> trackersToFilters = 
        new HashMap<DiscoveredServiceTracker, Collection<String>>();
    private BundleContext bc;
        
    ServiceTracker trackerTracker;
    
    public LocalDiscoveryService(BundleContext bc) {
        this.bc = bc;

        // track the registration of DiscoveredServiceTrackers
        trackerTracker = 
            new ServiceTracker(bc,
                               DiscoveredServiceTracker.class.getName(),
                               null) {
                public Object addingService(ServiceReference reference) {
                    Object result = super.addingService(reference);
                    cacheTracker(reference, result);
                    return result;
                }

                public void modifiedService(ServiceReference reference,
                                            Object service) {
                    super.modifiedService(reference, service);
                    updateTracker(reference, service);
                }

                public void removedService(ServiceReference reference,
                                           Object service) {
                    super.removedService(reference, service);
                    clearTracker(service);
                }
            };

        trackerTracker.open();
        
        bc.addBundleListener(this);
        // look at currently registered bundles.
        processExistingBundles();
    }

    public void bundleChanged(BundleEvent be) {
        LOG.fine("bundle changed: " + be.getBundle().getSymbolicName());
        switch (be.getType()) {
        case BundleEvent.STARTED:
            findDeclaredRemoteServices(be.getBundle());
            break;
        case BundleEvent.STOPPING:
            removeServicesDeclaredInBundle(be.getBundle());
            break;
        }
    }
    
    private void processExistingBundles() {
        Bundle[] bundles = bc.getBundles();
        if (bundles == null) {
            return;
        }
        
        for (Bundle b : bundles) {
            if (b.getState() == Bundle.ACTIVE) {
                findDeclaredRemoteServices(b);
            }
        }
    }


    private void findDeclaredRemoteServices(Bundle b) {
        List<ServiceEndpointDescription> refs = LocalDiscoveryUtils.getAllRemoteReferences(b);
        for(ServiceEndpointDescription sed : refs) {
            servicesInfo.put(sed, b);
            addedServiceDescription(sed);
        }        
    }    

    private void removeServicesDeclaredInBundle(Bundle bundle) {
        for(Iterator<Map.Entry<ServiceEndpointDescription, Bundle>> i = servicesInfo.entrySet().iterator(); i.hasNext(); ) {
            Entry<ServiceEndpointDescription, Bundle> entry = i.next();
            if (entry.getValue().equals(bundle)) {
                removedServiceDescription(entry.getKey());
                i.remove();
            }            
        }
    }

    private void addedServiceDescription(ServiceEndpointDescription sd) {
        triggerCallbacks(sd, AVAILABLE);
    }

    private void removedServiceDescription(ServiceEndpointDescription sd) {
        triggerCallbacks(sd, UNAVAILABLE);
    }

    private synchronized void cacheTracker(ServiceReference reference, 
                                           Object service) {
        if (service instanceof DiscoveredServiceTracker) {
            DiscoveredServiceTracker tracker = 
                (DiscoveredServiceTracker)service;
            Collection<String> interfaces =            
                addTracker(reference, 
                           tracker, 
                           INTERFACE_MATCH_CRITERIA, 
                           interfacesToTrackers,
                           trackersToInterfaces);
            Collection<String> filters = 
                addTracker(reference,
                           tracker, 
                           FILTER_MATCH_CRITERIA, 
                           filtersToTrackers,
                           trackersToFilters);

           triggerCallbacks(null, interfaces, tracker, false);
           triggerCallbacks(null, filters, tracker, true);
        }        
    }

    private synchronized void updateTracker(ServiceReference reference, 
                                            Object service) {
        if (service instanceof DiscoveredServiceTracker) {
            DiscoveredServiceTracker tracker = 
                (DiscoveredServiceTracker)service;
            LOG.info("updating tracker: " + tracker);
            Collection<String> oldInterfaces = removeTracker(tracker, 
                                                     interfacesToTrackers,
                                                     trackersToInterfaces);
            Collection<String> oldFilters = removeTracker(tracker, 
                                                  filtersToTrackers,
                                                  trackersToFilters);

            Collection<String> newInterfaces = 
                addTracker(reference, 
                           tracker, 
                           INTERFACE_MATCH_CRITERIA, 
                           interfacesToTrackers,
                           trackersToInterfaces);
            Collection<String> newFilters = 
                addTracker(reference,
                           tracker, 
                           FILTER_MATCH_CRITERIA, 
                           filtersToTrackers,
                           trackersToFilters);

            triggerCallbacks(oldInterfaces, newInterfaces, tracker, false);
            triggerCallbacks(oldFilters, newFilters, tracker, true);
        }
    }

    private synchronized void clearTracker(Object service) {
        if (service instanceof DiscoveredServiceTracker) {
            removeTracker((DiscoveredServiceTracker)service, 
                          interfacesToTrackers,
                          trackersToInterfaces);
            removeTracker((DiscoveredServiceTracker)service, 
                          filtersToTrackers,
                          trackersToFilters);
        }
    }

    @SuppressWarnings("unchecked")
    static Collection<String> addTracker(
                      ServiceReference reference, 
                      DiscoveredServiceTracker tracker,
                      String property,
                      Map<String, List<DiscoveredServiceTracker>> forwardMap,
                      Map<DiscoveredServiceTracker, Collection<String>> reverseMap) {
        Collection<String> collection = 
            (Collection<String>) reference.getProperty(property);
        LOG.info("adding tracker: " + tracker + " collection: " + collection + " registered against prop: " + property);
        if (nonEmpty(collection)) {
            reverseMap.put(tracker, new ArrayList<String>(collection));
            Iterator<String> i = collection.iterator();
            while (i.hasNext()) {
                String element = i.next();
                if (forwardMap.containsKey(element)) {
                    forwardMap.get(element).add(tracker);
                } else {
                    List<DiscoveredServiceTracker> trackerList = 
                        new ArrayList<DiscoveredServiceTracker>();
                    trackerList.add(tracker);
                    forwardMap.put(element, trackerList);
                }
            }
        }
        return collection;
    }

    static Collection<String> removeTracker(
                      DiscoveredServiceTracker tracker,
                      Map<String, List<DiscoveredServiceTracker>> forwardMap,
                      Map<DiscoveredServiceTracker, Collection<String>> reverseMap) {
        Collection<String> collection = reverseMap.get(tracker);
        if (nonEmpty(collection)) {
            reverseMap.remove(tracker);
            Iterator<String> i = collection.iterator();
            while (i.hasNext()) {
                String element = i.next();
                if (forwardMap.containsKey(element)) {
                    forwardMap.get(element).remove(tracker);
                } else {
                    // if the element wasn't on the forwardmap, its a new element and 
                    // shouldn't be returned as part of the collection of old ones
                    i.remove();                    
                }
            }
        }
        return collection;
    }
    
    private void triggerCallbacks(Collection<String> oldInterest, 
                                  Collection<String> newInterest, 
                                  DiscoveredServiceTracker tracker,
                                  boolean isFilter) {
        // compute delta between old & new interfaces/filters and
        // trigger callbacks for any entries in servicesInfo that
        // match any *additional* interface/filters  
        Collection<String> deltaInterest = new ArrayList<String>();
        if (nonEmpty(newInterest)) {
            if (isEmpty(oldInterest)) {
                deltaInterest.addAll(newInterest);
            } else {
                Iterator<String> i = newInterest.iterator();
                while (i.hasNext()) {
                    String next = (String)i.next();
                    if (!oldInterest.contains(next)) {
                        deltaInterest.add(next);
                    }
                }
            }
        }

        if (servicesInfo.size() > 0) {
            LOG.info("search for matches to trigger callbacks with delta: " + deltaInterest);
        } else {
            LOG.info("nothing to search for matches to trigger callbacks with delta: " + deltaInterest);
        }
        Iterator<String> i = deltaInterest.iterator();
        while (i.hasNext()) {
            String next = i.next();
            for (ServiceEndpointDescription sd : servicesInfo.keySet()) {
                triggerCallbacks(tracker, next, isFilter, sd, AVAILABLE);
            }
        }        
    }    

    private void triggerCallbacks(ServiceEndpointDescription sd, int type) {
        for (Map.Entry<DiscoveredServiceTracker, Collection<String>> entry : trackersToInterfaces.entrySet()) {
            for (String match : entry.getValue()) {
                triggerCallbacks(entry.getKey(), match, false, sd, type);
            }
        }
        for (Map.Entry<DiscoveredServiceTracker, Collection<String>> entry : trackersToFilters.entrySet()) {
            for (String match : entry.getValue()) {
                triggerCallbacks(entry.getKey(), match, true, sd, type);
            }
        }
    }

    void triggerCallbacks(DiscoveredServiceTracker tracker,
                          String toMatch, 
                          boolean isFilter, 
                          ServiceEndpointDescription sd,
                          int type) {
        LOG.fine("check if string: " + toMatch + (isFilter ? " matches " : " contained by ") +  sd.getProvidedInterfaces());

        TrackerNotification notification = 
            isFilter
            ? (filterMatches(toMatch, sd)
               ? new TrackerNotification(sd, true, toMatch, type)
               : null)
            : (sd.getProvidedInterfaces().contains(toMatch)
               ? new TrackerNotification(sd, false, toMatch, type)
               : null);

        if (notification != null) {
            tracker.serviceChanged(notification);
        }
    }    

    private static boolean nonEmpty(Collection<?> c) {
        return c != null && c.size() > 0;
    }

    private static boolean isEmpty(Collection<?> c) {
        return c == null || c.size() == 0;
    }

    private static class TrackerNotification 
        implements DiscoveredServiceNotification {

        private ServiceEndpointDescription sd;
        private Collection interfaces;
        private Collection filters;
        private int type;

        TrackerNotification(ServiceEndpointDescription sd, 
                            boolean isFilter, 
                            String match, 
                            int type) {
            this.sd = sd;
            if (isFilter) {
                filters = new ArrayList();
                filters.add(match);
                interfaces = Collections.EMPTY_SET;
            } else {
                interfaces = new ArrayList();
                interfaces.add(match);
                filters = Collections.EMPTY_SET;
            }

            this.type = type;
        }

        public ServiceEndpointDescription getServiceEndpointDescription() {
            return sd;
        }

        public int getType() {
            return type;
        }

        public Collection getInterfaces() {
            return interfaces; 
        }

        public Collection getFilters() {
            return filters; 
        }
    }
                
    public void shutdown() {
        bc.removeBundleListener(this);
        trackerTracker.close();
    }
        
    private Filter createFilter(String filterValue) {
        
        if (filterValue == null) {
            return null;
        }
        
        try {
            return bc.createFilter(filterValue); 
        } catch (InvalidSyntaxException ex) {
            System.out.println("Invalid filter expression " + filterValue);
        } catch (Exception ex) {
            System.out.println("Problem creating a Filter from " + filterValue); 
        }
        return null;
    }

    private boolean filterMatches(String filterValue, 
                                  ServiceEndpointDescription sd) {
        Filter filter = createFilter(filterValue);
        return filter != null
               ? filter.match(getServiceProperties(null, sd))
               : false;
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, Object> getServiceProperties(String interfaceName,
                                                            ServiceEndpointDescription sd) {
        Dictionary<String, Object> d = new Hashtable<String, Object>(sd.getProperties());
        
        String[] interfaceNames = getProvidedInterfaces(sd, interfaceName);
        if (interfaceNames != null) {
            d.put(INTERFACE_MATCH_CRITERIA, interfaceNames);
        }
        return d;
    }
    
    @SuppressWarnings("unchecked")
    private static String[] getProvidedInterfaces(ServiceEndpointDescription sd, String interfaceName) {
        
        Collection<String> interfaceNames = sd.getProvidedInterfaces();
        if (interfaceName == null) {
            return null;
        }
        
        Iterator<String> iNames = interfaceNames.iterator();
        while (iNames.hasNext()) {
            if (iNames.next().equals(interfaceName)) {
                return new String[]{interfaceName};
            }
        }
        return null;
    }
}
    
