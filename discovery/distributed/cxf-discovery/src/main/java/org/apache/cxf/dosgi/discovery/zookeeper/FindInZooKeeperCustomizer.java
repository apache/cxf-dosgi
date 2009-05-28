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
package org.apache.cxf.dosgi.discovery.zookeeper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class FindInZooKeeperCustomizer implements ServiceTrackerCustomizer {
    private final BundleContext bundleContext;
    private final ZooKeeper zookeeper;
    final Map<DiscoveredServiceTracker, List<InterfaceMonitor>> watchers = 
        new ConcurrentHashMap<DiscoveredServiceTracker, List<InterfaceMonitor>>();
    
    public FindInZooKeeperCustomizer(BundleContext bc, ZooKeeper zk) {
        bundleContext = bc;
        zookeeper = zk;
    }

    public Object addingService(ServiceReference sr) {
        Object svcObj = bundleContext.getService(sr);

        if (svcObj instanceof DiscoveredServiceTracker) {
            addingService(sr, (DiscoveredServiceTracker) svcObj);
        }
        return svcObj;
    }

    private void addingService(ServiceReference sr, DiscoveredServiceTracker dst) { 
        removedService(sr, dst);
        
        Set<String> interfaces = new HashSet<String>(Util.getMultiValueProperty(
            sr.getProperty(DiscoveredServiceTracker.INTERFACE_MATCH_CRITERIA)));
        interfaces.addAll(getInterfacesFromFilter(Util.getMultiValueProperty(
            sr.getProperty(DiscoveredServiceTracker.FILTER_MATCH_CRITERIA))));

        List<InterfaceMonitor> dmList = new ArrayList<InterfaceMonitor>(interfaces.size());
        for (String intf : interfaces) {
            InterfaceMonitor dm = new InterfaceMonitor(zookeeper, intf, dst);
            dmList.add(dm);
            dm.process();
        }
        watchers.put(dst, Collections.unmodifiableList(dmList));        
    }
    
    static Collection<String> getInterfacesFromFilter(Collection<String> filters) {
        String oc = "objectclass";

        Set<String> interfaces = new HashSet<String>(); 
        for (String filter : filters) {
            String lcFilter = filter.toLowerCase();
            int idx = lcFilter.indexOf(oc);
            if (idx < 0) {
                continue;
            }
            
            String s1 = filter.substring(idx + oc.length()).trim();
            if (!s1.startsWith("=")) {
                continue;
            }
            String s2 = s1.substring(1).trim();
            
            int idx2 = 0;
            while ((s2.charAt(idx2) == '.' || Character.isJavaIdentifierPart(s2.charAt(idx2))) && 
                    idx2 < s2.length()) {
                idx2++;
            }
            
            interfaces.add(s2.substring(0, idx2));
        }
        
        return interfaces;
    }

    public void modifiedService(ServiceReference sr, Object svcObj) {
        if (svcObj instanceof DiscoveredServiceTracker) {
            addingService(sr, (DiscoveredServiceTracker) svcObj);
        }
    }

    public void removedService(ServiceReference sr, Object svcObj) {
        List<InterfaceMonitor> oldVal = watchers.remove(svcObj);
        if (oldVal != null) {
            // TODO unregister any listeners directly registered with ZooKeeper
        }
    }

    public void processGlobalEvent(WatchedEvent event) {
        for (List<InterfaceMonitor> dmList : watchers.values()) {
            for (InterfaceMonitor dm : dmList) {
                dm.process();
            }
        }
    }
}
