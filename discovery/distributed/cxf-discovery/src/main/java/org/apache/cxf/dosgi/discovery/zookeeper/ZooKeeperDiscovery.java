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
import java.util.logging.Logger;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.ServiceTracker;

public class ZooKeeperDiscovery implements Watcher {

    private static final Logger LOG = Logger.getLogger(ZooKeeperDiscovery.class.getName());

    private boolean started = false;
    
    private BundleContext bctx;
    private ZooKeeper zooKeeper;
    private Dictionary properties = null;

    private EndpointListenerFactory endpointListenerFactory;
    private ServiceTracker endpointListenerTracker;

    private String zkHost;
    private String zkPort;
    private int zkTimeout;

    public ZooKeeperDiscovery(BundleContext bctx, Dictionary initialProps) {
        this.bctx = bctx;
        endpointListenerFactory = new EndpointListenerFactory(this, bctx);
        properties = initialProps;

        endpointListenerTracker = new ServiceTracker(bctx, EndpointListener.class.getName(),
                                                     new EndpointListenerTrackerCustomizer(this, bctx));
    }

    public synchronized void start() throws IOException, ConfigurationException {
        if(started) return;
        started = true;
        createZooKeeper(properties);
        endpointListenerFactory.start();
        endpointListenerTracker.open();
    }

    public synchronized void stop() {
        if(!started) return;
        started = false;
        endpointListenerFactory.stop();
        endpointListenerTracker.close();
    }


    private void createZooKeeper(Dictionary props) throws IOException, ConfigurationException {
        zkHost = getProp(props, "zookeeper.host");
        zkPort = getProp(props, "zookeeper.port");
        zkTimeout = Integer.parseInt(getProp(props, "zookeeper.timeout", "3000"));

        zooKeeper = createZooKeeper();

    }

    // separated for testing
    ZooKeeper createZooKeeper() throws IOException {
        return new ZooKeeper(zkHost + ":" + zkPort, zkTimeout, this);
    }

    private <T> boolean hasChanged(T orig, T nw) {
        if (orig == nw) {
            return false;
        }

        if (orig == null) {
            return true;
        }

        return !orig.equals(nw);
    }

    private static String getProp(Dictionary props, String key) throws ConfigurationException {
        String val = getProp(props, key, null);
        if (val != null) {
            return val;
        } else {
            throw new ConfigurationException(key, "The property " + key + " requires a value");
        }
    }

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
    public void process(WatchedEvent arg0) {
        // TODO Auto-generated method stub

    }

    protected ZooKeeper getZookeeper() {
        return zooKeeper;
    }

}
