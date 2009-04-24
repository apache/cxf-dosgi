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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import org.osgi.service.discovery.DiscoveredServiceTracker;

public class InterfaceMonitor implements Watcher, StatCallback {
    private static final Logger LOG = Logger.getLogger(InterfaceMonitor.class.getName());

    DataMonitorListener listener;
    final String znode;
    final ZooKeeper zookeeper;

    public InterfaceMonitor(ZooKeeper zk, String intf, DiscoveredServiceTracker dst) {
        listener = new InterfaceDataMonitorListenerImpl(zk, intf, dst);
        zookeeper = zk;
        znode = Util.getZooKeeperPath(intf);
    }
    
    public void process() {
        LOG.finest("Kicking off a zookeeper.exists() on node: " + znode);
        zookeeper.exists(znode, this, this, null);
    }

    public void process(WatchedEvent event) {
        LOG.finer("ZooKeeper watcher callback " + event);
        processDelta();
    }
    
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        LOG.finer("ZooKeeper callback on node: " + znode + " code: " + rc);
        
        switch (rc) {
        case Code.Ok:
            break;
        case Code.NoNode:
            break;
        case Code.SessionExpired:
            return;
        case Code.NoAuth:
            return;
        default:
            process();
            return;
        }
        
        processDelta();
    }

    private void processDelta() {
        try {
            if (zookeeper.exists(znode, false) != null) {
                listener.change();
                zookeeper.getChildren(znode, this);
            }
        } catch (Exception ke) {
            LOG.log(Level.SEVERE, "Error getting ZooKeeper data.", ke);
        }
    }
}
