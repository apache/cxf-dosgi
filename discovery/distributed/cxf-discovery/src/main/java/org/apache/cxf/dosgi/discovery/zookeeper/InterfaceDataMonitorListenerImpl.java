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

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.zookeeper.ZooKeeper;
import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServicePublication;

public class InterfaceDataMonitorListenerImpl implements DataMonitorListener {
    private static final Logger LOG = Logger.getLogger(InterfaceDataMonitorListenerImpl.class.getName());

    final ZooKeeper zookeeper;
    final String znode;
    final String interFace;
    final DiscoveredServiceTracker discoveredServiceTracker;

    // This map is *only* accessed in the change() method
    private Map<String, Map<String, Object>> nodes = new HashMap<String, Map<String,Object>>();
    
    public InterfaceDataMonitorListenerImpl(ZooKeeper zk, String intf, DiscoveredServiceTracker dst) {
        zookeeper = zk;
        znode = Util.getZooKeeperPath(intf);
        interFace = intf;
        discoveredServiceTracker = dst;
    }
    
    public synchronized void change() {
        Map<String, Map<String, Object>> newNodes = new HashMap<String, Map<String,Object>>();
        Map<String, Map<String, Object>> prevNodes = nodes;
        try {
            LOG.info("Zookeeper callback on node: " + znode);
            List<String> children = zookeeper.getChildren(znode, false);
            
            for (String child : children) {
                byte[] data = zookeeper.getData(znode + '/' + child, false, null);
                Properties p = new Properties();
                p.load(new ByteArrayInputStream(data));
                
                Map<String, Object> m = new HashMap<String, Object>();
                for (Map.Entry<Object, Object> entry : p.entrySet()) {                    
                    m.put(entry.getKey().toString(), entry.getValue());
                }
                
                // Put in some reasonable defaults, if not specified
                if (!m.containsKey("service.exported.configs")) {
                    m.put("service.exported.configs", "org.apache.cxf.ws");
                }
                if (Util.getMultiValueProperty(m.get("service.exported.configs")).contains("org.apache.cxf.ws") &&
                    !m.containsKey("org.apache.cxf.ws.address")) {
                    m.put("org.apache.cxf.ws.address", m.get(ServicePublication.ENDPOINT_LOCATION));                    
                }
                
                newNodes.put(child, m);
                Map<String, Object> prevVal = prevNodes.remove(child);
                if (prevVal == null) {
                    // This guy is new
                    ServiceEndpointDescriptionImpl sed = new ServiceEndpointDescriptionImpl(Collections.singletonList(interFace), m);
                    DiscoveredServiceNotification dsn = new DiscoveredServiceNotificationImpl(Collections.emptyList(),
                        Collections.singleton(interFace), DiscoveredServiceNotification.AVAILABLE, sed);
                    discoveredServiceTracker.serviceChanged(dsn);                    
                } else if (!prevVal.equals(m)){
                    // There's been a modification
                    ServiceEndpointDescriptionImpl sed = new ServiceEndpointDescriptionImpl(Collections.singletonList(interFace), m);
                    DiscoveredServiceNotification dsn = new DiscoveredServiceNotificationImpl(Collections.emptyList(),
                        Collections.singleton(interFace), DiscoveredServiceNotification.MODIFIED, sed);
                    discoveredServiceTracker.serviceChanged(dsn);                    
                }                
            }

            for (Map<String, Object> props : prevNodes.values()) {
                // whatever's left in prevNodes now has been removed from Discovery
                ServiceEndpointDescriptionImpl sed = new ServiceEndpointDescriptionImpl(Collections.singletonList(interFace), props);
                DiscoveredServiceNotification dsn = new DiscoveredServiceNotificationImpl(Collections.emptyList(),
                        Collections.singleton(interFace), DiscoveredServiceNotification.UNAVAILABLE, sed);
                discoveredServiceTracker.serviceChanged(dsn);                
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Problem processing Zookeeper callback", e);
        } finally {
            nodes = newNodes;
        }
    }
}
