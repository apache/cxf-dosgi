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
package org.apache.cxf.dosgi.dsw.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.distribution.DistributionProvider;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class DistributionProviderImpl implements DistributionProvider, CxfDistributionProvider {
    private Set<EventAdmin> eventAdmins = new HashSet<EventAdmin>(); 
    
    private Map<ServiceReference, Map<String, String>> publicationProperties = 
        new HashMap<ServiceReference, Map<String,String>>();
    private Set<ServiceReference> exposedServices = new HashSet<ServiceReference>(); 
    private Set<ServiceReference> remoteServices = new HashSet<ServiceReference>();

    private final ServiceTracker tracker;
            
    public DistributionProviderImpl(BundleContext bc) {
        tracker = new ServiceTracker(bc, EventAdmin.class.getName(), new MyTrackerCustomizer());
        tracker.open();
    }
    
    public void shutdown() {
        tracker.close();
    }

    public Map<String, String> getExposedProperties(ServiceReference sr) {
        synchronized (publicationProperties) {
            return publicationProperties.get(sr);            
        }
    }
    
    public Collection /*<? extends ServiceReference>*/ getExposedServices() {
        synchronized (exposedServices) {
            // Defensive copy
            return new HashSet<ServiceReference>(exposedServices);            
        }
    }

    public Collection /*<? extends ServiceReference>*/ getRemoteServices() {
        synchronized (remoteServices) {
            // Defensive copy
            return new HashSet<ServiceReference>(remoteServices);
        }
    }

    public void addExposedService(ServiceReference sr, Map<String, String> pp) {
        synchronized (exposedServices) {
            exposedServices.add(sr);
        }
        synchronized (publicationProperties) {
            if (pp == null) {
                pp = Collections.emptyMap();
            }
            publicationProperties.put(sr, pp);
        }
        postAdminEvent(DistributionProvider.class.getName().replace('.', '/') + "/service/exposed", sr);
    }
            
    public void intentsUnsatisfied(ServiceReference sr) {
        postAdminEvent(DistributionProvider.class.getName().replace('.', '/') + "/service/unsatisfied", sr);
    }

    @SuppressWarnings("unchecked")
    private void postAdminEvent(String topic, ServiceReference serviceReference) {        
        Set<EventAdmin> eas = null;
        synchronized (eventAdmins) {
            eas = new HashSet<EventAdmin>(eventAdmins);
        }
        
        for (EventAdmin eventAdmin : eas) {
            Dictionary ht = new Hashtable();
            addEventEntry(ht, EventConstants.SERVICE, serviceReference);
            addEventEntry(ht, EventConstants.SERVICE_ID, serviceReference.getProperty(org.osgi.framework.Constants.SERVICE_ID));
            addEventEntry(ht, EventConstants.SERVICE_OBJECTCLASS, serviceReference.getProperty(org.osgi.framework.Constants.OBJECTCLASS));
            addEventEntry(ht, EventConstants.SERVICE_PID, serviceReference.getProperty(org.osgi.framework.Constants.SERVICE_PID));
            eventAdmin.postEvent(new Event(topic, ht));
        }
    }

    @SuppressWarnings("unchecked")
    private void addEventEntry(Dictionary ht, String key, Object value) {
        if (value != null) {
            ht.put(key, value);
        }
    }    

    public void addEventAdmin(EventAdmin ea) {
        synchronized(eventAdmins) {
            eventAdmins.add(ea);
        }
    }

    public void addRemoteService(ServiceReference sr) {
        synchronized (remoteServices) {
            remoteServices.add(sr);
        }
    }

    public void removeEventAdmin(EventAdmin ea) {
        synchronized(eventAdmins) {
            eventAdmins.remove(ea);
        }
    }

    private class MyTrackerCustomizer implements ServiceTrackerCustomizer {
        public Object addingService(ServiceReference sr) {
            Object svc = sr.getBundle().getBundleContext().getService(sr);
            if (svc instanceof EventAdmin) {
                addEventAdmin((EventAdmin) svc);
            }
            return svc;
        }

        public void modifiedService(ServiceReference sr, Object svc) {}
        public void removedService(ServiceReference sr, Object svc) {
            if (svc != null) {
                removeEventAdmin((EventAdmin) svc);
            }
        }
    }
}
