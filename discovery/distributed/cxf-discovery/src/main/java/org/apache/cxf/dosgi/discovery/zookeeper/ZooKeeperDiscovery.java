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

import java.io.IOException;
import java.util.Dictionary;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.ServiceTracker;

public class ZooKeeperDiscovery implements Watcher, ManagedService {

    private static final Logger LOG = Logger.getLogger(ZooKeeperDiscovery.class.getName());

    private final BundleContext bctx;

    private PublishingEndpointListenerFactory endpointListenerFactory;
    private ServiceTracker endpointListenerTracker;

    private InterfaceMonitorManager imManager;

    private ZooKeeper zooKeeper;

    @SuppressWarnings("rawtypes")
    private Dictionary curConfiguration;

    public ZooKeeperDiscovery(BundleContext bctx) {
        this.bctx = bctx;
        this.curConfiguration = null;
    }

    @SuppressWarnings("rawtypes")
    public synchronized void updated(Dictionary configuration) throws ConfigurationException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Received configuration update for Zookeeper Discovery: " + configuration);
        }

        synchronized (this) {
            stop();
        }

        if (configuration == null) {
            return;
        }
        curConfiguration = configuration;
        try {
            zooKeeper = createZooKeeper(configuration);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to start the Zookeeper Discovery component.", e);
        }
    }

    private void startModules() {
        endpointListenerFactory = new PublishingEndpointListenerFactory(zooKeeper, bctx);
        endpointListenerFactory.start();
        imManager = new InterfaceMonitorManager(bctx, zooKeeper);
        EndpointListenerTrackerCustomizer customizer = new EndpointListenerTrackerCustomizer(bctx, imManager);
        endpointListenerTracker = new ServiceTracker(bctx, EndpointListener.class.getName(), customizer);
        endpointListenerTracker.open();
    }

    public synchronized void stop() {
        if (endpointListenerFactory != null) {
            endpointListenerFactory.stop();
        }
        if (imManager != null) {
            imManager.close();
        }
        if (endpointListenerTracker != null) {
            endpointListenerTracker.close();
        }
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                LOG.log(Level.SEVERE, "Error closing zookeeper", e);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private ZooKeeper createZooKeeper(Dictionary props) throws IOException {
        String zkHost = getProp(props, "zookeeper.host", "localhost");
        String zkPort = getProp(props, "zookeeper.port", "2181");
        int zkTimeout = Integer.parseInt(getProp(props, "zookeeper.timeout", "3000"));
        return new ZooKeeper(zkHost + ":" + zkPort, zkTimeout, this);
    }

    @SuppressWarnings("rawtypes")
    private static String getProp(Dictionary props, String key, String def) {
        Object val = props.get(key);
        String rv;
        if (val == null) {
            rv = def;
        } else {
            rv = val.toString();
        }

        LOG.fine("Reading Config Admin property: " + key + " value returned: " + rv);
        return rv;
    }

    /* Callback for ZooKeeper */
    public void process(WatchedEvent event) {
        KeeperState state = event.getState();
        if (state == KeeperState.SyncConnected) {
            LOG.info("Connection to zookeeper established");
            startModules();
        }
        if (state == KeeperState.Expired) {
            LOG.info("Connection to zookeeper expired. Trying to create a new connection");
            stop();
            try {
                zooKeeper = createZooKeeper(curConfiguration);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to start the Zookeeper Discovery component.", e);
            }
        }
    }
}
