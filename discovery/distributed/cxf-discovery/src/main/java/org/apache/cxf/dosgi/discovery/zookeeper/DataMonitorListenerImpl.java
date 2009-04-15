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

import org.apache.cxf.dosgi.discovery.local.ServiceEndpointDescriptionImpl;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.service.discovery.DiscoveredServiceNotification;
import org.osgi.service.discovery.DiscoveredServiceTracker;

public class DataMonitorListenerImpl implements DataMonitorListener {
    private static final Logger LOG = Logger.getLogger(DataMonitorListenerImpl.class.getName());

    final ZooKeeper zookeeper;
    final String znode;
    final String interFace;
    final DiscoveredServiceTracker discoveredServiceTracker;
    
    public DataMonitorListenerImpl(ZooKeeper zk, String intf, DiscoveredServiceTracker dst) {
        zookeeper = zk;
        znode = Util.getZooKeeperPath(intf);
        interFace = intf;
        discoveredServiceTracker = dst;
    }
    
    public void exists() {
        try {
            LOG.info("Zookeeper callback on node: " + znode);
            List<String> children = zookeeper.getChildren(znode, false);
            
            for (String child : children) {
                byte[] data = zookeeper.getData(znode + '/' + child, false, null);
                Properties p = new Properties();
                p.load(new ByteArrayInputStream(data));
                
                Map<String, Object> p2 = new HashMap<String, Object>();
                for (Map.Entry<Object, Object> entry : p.entrySet()) {
                    /* TODO this is probably not necessary
                    if (Constants.SERVICE_ID.equals(entry.getKey()) ||
                        Constants.SERVICE_PID.equals(entry.getKey()) ||
                        Constants.OBJECTCLASS.equals(entry.getKey())) {
                        continue;
                    } */
                    
                    p2.put(entry.getKey().toString(), entry.getValue());
                }
                
                ServiceEndpointDescriptionImpl sed = new ServiceEndpointDescriptionImpl(Collections.singletonList(interFace), p2);
                DiscoveredServiceNotification dsn = new DiscoveredServiceNotificationImpl(Collections.emptyList(),
                    Collections.singleton(interFace), DiscoveredServiceNotification.AVAILABLE, sed);
                discoveredServiceTracker.serviceChanged(dsn);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Problem processing Zookeeper callback", e);
        }                    
    }

    public void closing(int rc) {
        System.out.println("*** closing " + rc);
        // TODO do we need this callback?
    }
}
