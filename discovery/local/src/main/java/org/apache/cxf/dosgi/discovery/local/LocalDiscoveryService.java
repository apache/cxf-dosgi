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


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.discovery.Discovery;
import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.util.tracker.ServiceTracker;

import static org.osgi.service.discovery.DiscoveredServiceNotification.AVAILABLE;
import static org.osgi.service.discovery.DiscoveredServiceNotification.UNAVAILABLE;
import static org.osgi.service.discovery.DiscoveredServiceTracker.PROP_KEY_MATCH_CRITERIA_FILTERS;
import static org.osgi.service.discovery.DiscoveredServiceTracker.PROP_KEY_MATCH_CRITERIA_INTERFACES;

public class LocalDiscoveryService implements Discovery {
    
    private static final Logger LOG = Logger.getLogger(LocalDiscoveryService.class.getName());
    

    private static final String DEFAULT_SERVICES_LOCATION = 
        "/discovery/conf/remote-services.xml";
    private static final String REMOTE_SERVICES_ENTRY = 
        "/META-INF/remote-service/remote-services.xml";
    private static final String REMOTE_SERVICES_NS =
        "http://www.osgi.org/xmlns/sd/v1.0.0";    
    
    private static final String SERVICE_DESCRIPTION_ELEMENT = "service-description";
    
    private static final String PROVIDE_INTERFACE_ELEMENT = "provide";
    private static final String PROVIDE_INTERFACE_NAME_ATTRIBUTE = "interface";

    private static final String PROPERTY_ELEMENT = "property";
    private static final String PROPERTY_KEY_ATTRIBUTE = "name";
    
    // TODO : this property should be configurable
    private static final long CONFIG_CHECK_TIME = 5000L;
    
    // this is effectively a set which allows for multiple service descriptions with the
    // same interface name but different properties and takes care of itself with respect to concurrency 
    private ConcurrentHashMap<ServiceEndpointDescription, ServiceEndpointDescription> servicesInfo = 
        new ConcurrentHashMap<ServiceEndpointDescription, ServiceEndpointDescription>();
    private Map<ServiceEndpointDescription, ServiceEndpointDescription> oldServicesInfo =
        new HashMap<ServiceEndpointDescription, ServiceEndpointDescription>();
    private Map<ServiceEndpointDescription, ServiceEndpointDescription> newServicesInfo =
        new HashMap<ServiceEndpointDescription, ServiceEndpointDescription>();
    private Map<String, List<DiscoveredServiceTracker>> interfacesToTrackers = 
        new HashMap<String, List<DiscoveredServiceTracker>>();
    private Map<String, List<DiscoveredServiceTracker>> filtersToTrackers = 
        new HashMap<String, List<DiscoveredServiceTracker>>();
    private Map<DiscoveredServiceTracker, Collection> trackersToInterfaces = 
        new HashMap<DiscoveredServiceTracker, Collection>();
    private Map<DiscoveredServiceTracker, Collection> trackersToFilters = 
        new HashMap<DiscoveredServiceTracker, Collection>();
    private BundleContext bc;
    
    private boolean attachedToBundle;
    private boolean isShutdown;
    
    private Thread configChecksThread;
    private long lastConfigModification;
    private ServiceTracker trackerTracker;
    
    public LocalDiscoveryService(BundleContext bc) {
        this.bc = bc;
        readMetadata();

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

        if (!attachedToBundle) {
            configChecksThread = new Thread(new ConfigCheckRunnable());
            configChecksThread.start();
        }
    }

    public void updateProperties(Dictionary props) {
    }

    private synchronized void cacheTracker(ServiceReference reference, 
                                           Object service) {
        if (service instanceof DiscoveredServiceTracker) {
            DiscoveredServiceTracker tracker = 
                (DiscoveredServiceTracker)service;
            Collection interfaces =            
                addTracker(reference, 
                           tracker, 
                           PROP_KEY_MATCH_CRITERIA_INTERFACES, 
                           interfacesToTrackers,
                           trackersToInterfaces);
            Collection filters = 
                addTracker(reference,
                           tracker, 
                           PROP_KEY_MATCH_CRITERIA_FILTERS, 
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
            Collection oldInterfaces = removeTracker(tracker, 
                                                     interfacesToTrackers,
                                                     trackersToInterfaces);
            Collection oldFilters = removeTracker(tracker, 
                                                  filtersToTrackers,
                                                  trackersToFilters);

            Collection newInterfaces = 
                addTracker(reference, 
                           tracker, 
                           PROP_KEY_MATCH_CRITERIA_INTERFACES, 
                           interfacesToTrackers,
                           trackersToInterfaces);
            Collection newFilters = 
                addTracker(reference,
                           tracker, 
                           PROP_KEY_MATCH_CRITERIA_FILTERS, 
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

    private Collection addTracker(
                      ServiceReference reference, 
                      DiscoveredServiceTracker tracker,
                      String property,
                      Map<String, List<DiscoveredServiceTracker>> forwardMap,
                      Map<DiscoveredServiceTracker, Collection> reverseMap) {
        Collection collection = 
            (Collection)reference.getProperty(property);
        LOG.info("adding tracker: " + tracker + " collection: " + collection + " registered against prop: " + property);
        if (nonEmpty(collection)) {
            reverseMap.put(tracker, collection);
            Iterator i = collection.iterator();
            while (i.hasNext()) {
                String element = (String)i.next();
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

    private Collection removeTracker(
                      DiscoveredServiceTracker tracker,
                      Map<String, List<DiscoveredServiceTracker>> forwardMap,
                      Map<DiscoveredServiceTracker, Collection> reverseMap) {
        Collection collection = reverseMap.get(tracker);
        if (nonEmpty(collection)) {
            reverseMap.remove(tracker);
            Iterator i = collection.iterator();
            while (i.hasNext()) {
                String element = (String)i.next();
                if (forwardMap.containsKey(element)) {
                    forwardMap.get(element).remove(tracker);
                }
            }
        }
        return collection;
    }

    private void triggerCallbacks(Collection oldInterest, 
                                  Collection newInterest, 
                                  DiscoveredServiceTracker tracker,
                                  boolean isFilter) {
        // compute delta between old & new interfaces/filters and
        // trigger callbacks for any entries in servicesInfo that
        // match any *additional* interface/filters  
        Collection deltaInterest = new ArrayList();
        if (nonEmpty(newInterest)) {
            if (isEmpty(oldInterest)) {
                deltaInterest.addAll(newInterest);
            } else {
                Iterator i = newInterest.iterator();
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
        Iterator i = deltaInterest.iterator();
        while (i.hasNext()) {
            String next = (String)i.next();
            for (ServiceEndpointDescription sd : servicesInfo.values()) {
                LOG.info("check if next: " + next + (isFilter ? " matches " : " contained by ") +  sd.getProvidedInterfaces());
                if ((isFilter && filterMatches(next, sd))
                    || sd.getProvidedInterfaces().contains(next)) {
                    tracker.serviceChanged(new TrackerNotification(sd, 
                                                                   AVAILABLE));
                }
            }
        }
    }

    private static boolean nonEmpty(Collection c) {
        return c != null && c.size() > 0;
    }

    private static boolean isEmpty(Collection c) {
        return c == null || c.size() == 0;
    }

    private static class TrackerNotification 
        implements DiscoveredServiceNotification {

        private ServiceEndpointDescription sd;
        private int type;

        TrackerNotification(ServiceEndpointDescription sd, int type) {
            this.sd = sd;
            this.type = type;
        }

        public ServiceEndpointDescription getServiceEndpointDescription() {
            return sd;
        }

        public int getType() {
            return type;
        }
    }
    
    private void readMetadata() {
        
        InputStream is = getRemoteServicesStream();
        if (is != null) {
            try {
                readRemoteServices(is);    
            } catch (Exception ex) {
                LOG.warning("Problem parsing remote-services.xml :" 
                                   + ex.getMessage());
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                   // ignore
                }
            }
        }
    }
    
    private InputStream getRemoteServicesStream() {
   
        // check default location
        File f = new File(System.getProperty("user.dir") + DEFAULT_SERVICES_LOCATION);
        if (f.exists()) {
            long lastModified = f.lastModified();
            if (lastModified == lastConfigModification) {
                return null;
            }
            lastConfigModification = lastModified;
            
            try {
                LOG.info("Found remote-services.xml at " + f.getAbsolutePath());
                return f.toURL().openStream();
            } catch (Exception ex) {
                LOG.warning("remote-services.xml at " + f.getAbsolutePath() 
                                   + " can not be accessed");                
            }
        }
        
        // check if remoteservices.xml are attached to this bundle
        URL resourceURL = bc.getBundle().getEntry(REMOTE_SERVICES_ENTRY);
        if (resourceURL != null) {
            try {
                InputStream is = resourceURL.openStream();
                attachedToBundle = true;
                return is; 
            } catch (IOException ex) {
                LOG.warning("Problem accessing remote-services.xml class resource");                
            }    
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private void readRemoteServices(InputStream is) throws Exception {

        Document d = new SAXBuilder().build(is);
        Namespace ns = Namespace.getNamespace(REMOTE_SERVICES_NS);
        List<Element> references = d.getRootElement().getChildren(SERVICE_DESCRIPTION_ELEMENT, ns);
        
        Set<ServiceEndpointDescription> keys = servicesInfo.keySet();
        for (ServiceEndpointDescription sd : keys) {
            oldServicesInfo.put(sd, sd);
        }
        servicesInfo.clear();
        
        for (Element ref : references) {
            List<String> iNames = getProvidedInterfaces(ref.getChildren(PROVIDE_INTERFACE_ELEMENT, ns));
            if (iNames.isEmpty()) {
                continue;
            }
            
            ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(iNames,  
                          getProperties(ref.getChildren(PROPERTY_ELEMENT, ns)));
            LOG.info("retrieved remote-service info for: " + sd.getProvidedInterfaces());
            servicesInfo.putIfAbsent(sd, sd);
            if (!oldServicesInfo.containsKey(sd)) {
                newServicesInfo.put(sd, sd);
            } else {
                oldServicesInfo.remove(sd);
            }
        }
        
        triggerCallbacks();
    }

    private void triggerCallbacks() {
        // REVISIT: generating modified callbacks for interface elements with
        // modified properties is problematic since multiple interface elements
        // with the same provides attribute are currently considered to 
        // represent distinct service instances

        // iterate over oldServicesInfo generating unavailable callbacks for
        // completely removed service instances
        triggerNotificationsFor(oldServicesInfo, UNAVAILABLE);


        // iterate over newServicesInfo generating added callbacks for
        // new services
        triggerNotificationsFor(newServicesInfo, AVAILABLE);
    }

    private void triggerNotificationsFor(Map<ServiceEndpointDescription, ServiceEndpointDescription> sds, int type) {
        Set<ServiceEndpointDescription> keys = sds.keySet();
        for (ServiceEndpointDescription sd : keys) {
            Iterator interfaces = interfacesToTrackers.keySet().iterator();
            while (interfaces.hasNext()) {
                String next = (String)interfaces.next();
                // REVISIT: shouldn't the notification include some indication
                // of which interface name (or filter) fired the match?
                if (sd.getProvidedInterfaces().contains(next)) {
                    List<DiscoveredServiceTracker> trackers = 
                        interfacesToTrackers.get(next);
                    for (DiscoveredServiceTracker tracker : trackers) {
                        tracker.serviceChanged(new TrackerNotification(sd, 
                                                                       type));
                    }
                }
            }

            Iterator filters = filtersToTrackers.keySet().iterator();
            while (filters.hasNext()) {
                String next = (String)filters.next();
                if (filterMatches(next, sd)) {
                    List<DiscoveredServiceTracker> trackers = 
                        filtersToTrackers.get(next);
                    for (DiscoveredServiceTracker tracker : trackers) {
                        tracker.serviceChanged(new TrackerNotification(sd, 
                                                                       type));
                    }
                }
            }
        }
        sds.clear();
   }
    
    private Map<String, Object> getProperties(List<Element> elementProps) {
        
        Map<String, Object> props = new HashMap<String, Object>();
        
        for (Element p : elementProps) {
            String key = p.getAttributeValue(PROPERTY_KEY_ATTRIBUTE);
            if (key != null) {
                props.put(key, p.getTextTrim());
            }
        }
        
        return props;
    }
 
    private static List<String> getProvidedInterfaces(List<Element> elements) {
        
        List<String> names = new ArrayList<String>();
        
        for (Element p : elements) {
            String name = p.getAttributeValue(PROVIDE_INTERFACE_NAME_ATTRIBUTE);
            if (name != null) {
                names.add(name);
            }
        }
        
        return names;
    }
    
    public void shutdown() {
        synchronized (this) {
            isShutdown = true;
        }    
        if (configChecksThread != null) {
            configChecksThread.interrupt();
        }
    }
    
    private class ConfigCheckRunnable implements Runnable {

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(CONFIG_CHECK_TIME);
                    readMetadata();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            
        }
        
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
            d.put(Constants.OBJECTCLASS, interfaceNames);
        }
        return d;
    }
    
    private static String[] getProvidedInterfaces(ServiceEndpointDescription sd, String interfaceName) {
        
        Collection interfaceNames = sd.getProvidedInterfaces();
        if (interfaceName == null) {
            return null;
        }
        
        Iterator iNames = interfaceNames.iterator();
        while (iNames.hasNext()) {
            if (iNames.next().equals(interfaceName)) {
                return new String[]{interfaceName};
            }
        }
        return null;
    }
    
}
    
