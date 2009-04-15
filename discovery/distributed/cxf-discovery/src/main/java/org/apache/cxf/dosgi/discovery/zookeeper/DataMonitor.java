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

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import org.osgi.service.discovery.DiscoveredServiceTracker;

public class DataMonitor implements StatCallback {
    private static final Logger LOG = Logger.getLogger(DataMonitor.class.getName());

    DataMonitorListener listener;
    final String znode;
    final ZooKeeper zookeeper;
    private byte [] prevData;

    public DataMonitor(ZooKeeper zk, String intf, DiscoveredServiceTracker dst) {
        listener = new DataMonitorListenerImpl(zk, intf, dst);
        zookeeper = zk;
        znode = Util.getZooKeeperPath(intf);
        
        zookeeper.exists(znode, true, this, null);
    }

    public void processResult(int rc, String path, Object ctx, Stat stat) {
        boolean exists;
        
        switch (rc) {
        case Code.Ok:
            exists = true;
            break;
        case Code.NoNode:
            exists = false;
            break;
        case Code.SessionExpired:
            LOG.info("ZooKeeper reports: SessionExpired on node: " + znode);
            return;
        case Code.NoAuth:
            LOG.info("ZooKeeper reports: NoAuth on node: " + znode);
            return;
        default:
            zookeeper.exists(znode, true, this, null);
            return;
        }
        
        byte [] b = null;
        if (exists) {
            try {
                b = zookeeper.getData(znode, false, null);                
            } catch (Exception ke) {
                LOG.log(Level.SEVERE, "Error getting ZooKeeper data.", ke);
            }
            
            if (!Arrays.equals(prevData, b)) {
                listener.exists();
                prevData = b;
            }
        }
    }
}
