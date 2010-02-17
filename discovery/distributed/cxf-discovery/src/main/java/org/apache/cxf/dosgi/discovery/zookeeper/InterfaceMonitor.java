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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class InterfaceMonitor implements Watcher, StatCallback {
    private static final Logger LOG = Logger.getLogger(InterfaceMonitor.class.getName());

    InterfaceDataMonitorListenerImpl listener;
    final String znode;
    final ZooKeeper zookeeper;
    
    private boolean closed = false;

    public InterfaceMonitor(ZooKeeper zk, String intf, EndpointListenerTrackerCustomizer.Interest zkd, String scope, BundleContext bctx) {
        listener = new InterfaceDataMonitorListenerImpl(zk, intf, zkd,scope,bctx);
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
        case Code.NoNode:
            break;

        case Code.SessionExpired:
        case Code.NoAuth:
        case Code.ConnectionLoss:
            return;
        
        default:
            process();
            return;
        }
        
        processDelta();
    }

    private void processDelta() {
        if(closed) return;
        
        if(zookeeper.getState() != ZooKeeper.States.CONNECTED){
            LOG.info("zookeeper connection was already closed! Not processing changed event.");
            return;
        }
        
        try {
            if (zookeeper.exists(znode, false) != null) {
                listener.change();
                zookeeper.getChildren(znode, this);
            }
        } catch (Exception ke) {
            LOG.log(Level.SEVERE, "Error getting ZooKeeper data.", ke);
        }
    }

    public void inform(ServiceReference sref) {
       listener.inform(sref);
    }

    public void close() {
        // TODO !!!     
        closed = true;
    }
}
