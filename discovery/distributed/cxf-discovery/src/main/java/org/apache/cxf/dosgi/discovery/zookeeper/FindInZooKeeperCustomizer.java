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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class FindInZooKeeperCustomizer implements ServiceTrackerCustomizer {
    private final BundleContext bundleContext;
    private final ZooKeeper zookeeper;
    final Map<String, DataMonitor> watchers = new HashMap<String, DataMonitor>();
    
    public FindInZooKeeperCustomizer(BundleContext bc, ZooKeeper zk) {
        bundleContext = bc;
        zookeeper = zk;
    }

    public Object addingService(ServiceReference sr) {
        Object svcObj = bundleContext.getService(sr);

        if (svcObj instanceof DiscoveredServiceTracker) {
            DiscoveredServiceTracker dst = (DiscoveredServiceTracker) svcObj;
            Collection<String> interfaces = Util.getMultiValueProperty(
                sr.getProperty(DiscoveredServiceTracker.PROP_KEY_MATCH_CRITERIA_INTERFACES));

            for (String intf : interfaces) {
                DataMonitor dm = new DataMonitor(zookeeper, intf, dst);
                watchers.put(intf, dm);
            }
        }
        return svcObj;
    }

    public void modifiedService(ServiceReference arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

    public void removedService(ServiceReference arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

}
