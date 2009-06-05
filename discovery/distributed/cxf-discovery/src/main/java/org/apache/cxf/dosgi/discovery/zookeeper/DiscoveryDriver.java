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
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServicePublication;
import org.osgi.util.tracker.ServiceTracker;

public class DiscoveryDriver implements Watcher {
    private static final Logger LOG = Logger.getLogger(DiscoveryDriver.class.getName());

    private final BundleContext bundleContext;

    FindInZooKeeperCustomizer finderCustomizer;
    ServiceTracker lookupTracker;
    ServiceTracker publicationTracker;
    ZooKeeper zooKeeper;

    String zkHost;
    String zkPort;
    int zkTimeout;
        
//    public static DiscoveryDriver getDriver(BundleContext bc, Dictionary props) {
//        LOG.info("Obtaining ZooKeeper Discovery driver.");
//        
//        DiscoveryDriver dd = new DiscoveryDriver(bc);
//        try {
//            dd.createZooKeeper(props);
//            dd.init();
//            return dd;
//        } catch (ConfigurationException e) {
//            LOG.log(Level.INFO, "Insufficient configuration information to create ZooKeeper client.", e);
//            return null;
//        } catch (Exception e) {
//            LOG.log(Level.WARNING, "Problem creating ZooKeeper based discovery driver.", e);
//            return null;
//        }
//    }    

    DiscoveryDriver(BundleContext bc, Dictionary props) throws IOException, ConfigurationException {
        bundleContext = bc;        
        createZooKeeper(props);
        init();
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

    private void init() {        
        publicationTracker = new ServiceTracker(bundleContext, ServicePublication.class.getName(), 
                new PublishToZooKeeperCustomizer(bundleContext, zooKeeper));
        publicationTracker.open();
        
        finderCustomizer = new FindInZooKeeperCustomizer(bundleContext, zooKeeper);
        lookupTracker = new ServiceTracker(bundleContext, DiscoveredServiceTracker.class.getName(),
                finderCustomizer);
        lookupTracker.open();        
    }
    
    public void destroy() throws Exception {
        lookupTracker.close();
        publicationTracker.close();
        zooKeeper.close();
    }

    public void process(WatchedEvent event) {
        finderCustomizer.processGlobalEvent(event);
    }
    
    public void updateConfiguration(Dictionary props) {
        LOG.warning("updateConfiguration not yet implemented");
        /* TODO not yet finished
        String host, port;
        int timeout;
        try {
            host = getProp(props, "zookeeper.host");
            port = getProp(props, "zookeeper.port");
            timeout = Integer.parseInt(getProp(props, "zookeeper.timeout", "3000"));
        } catch (Exception e) {
            LOG.log(Level.INFO, "Insufficient configuration information to update ZooKeeper client.", e);
            return;
        }
        
        if (hasChanged(zkHost, host) ||
            hasChanged(zkPort, port) ||
            hasChanged(zkTimeout, timeout)) {
            synchronized(this) {
                try {
                    zooKeeper.close();
                } catch (InterruptedException e) {}

                @@@ TODO need to close the trackers/customizers
                try {
                    zooKeeper = createZooKeeper(props);
                    
                    @@@ TODO need to recreate the trackers/customizers 
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Problem recreating zookeeper client after update.", e);
                }
            }
        } else {
            LOG.info("No configuration changes for ZooKeeper client.");
        }
        */
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
}
