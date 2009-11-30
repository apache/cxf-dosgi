/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cxf.dosgi.discovery.zookeeper.server;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;

public class ManagedService implements org.osgi.service.cm.ManagedService {
    private static final Logger LOG = Logger.getLogger(ManagedService.class);

    private final BundleContext bundleContext;
    ServiceRegistration serviceRegistration;
    MyZooKeeperServerMain main;
    Thread zkMainThread;
    
    public ManagedService(BundleContext ctx) {
        bundleContext = ctx;
    }
    
    public synchronized void shutdown() {
        if (main != null) {
            LOG.info("Shutting down ZooKeeper server");
            main.shutdown();
            try {
                zkMainThread.join();
            } catch (InterruptedException e) {
                // ignore
            }
            main = null;
            zkMainThread = null;
        }
    }
    
    @SuppressWarnings("unchecked")
    public void setDefaults(Dictionary dict) throws IOException {
        setDefault(dict, "tickTime", "2000");        
        setDefault(dict, "initLimit", "10");
        setDefault(dict, "syncLimit", "5");
        setDefault(dict, "dataDir", new File(bundleContext.getDataFile(""), "zkdata").getCanonicalPath());        
        setDefault(dict, Constants.SERVICE_PID, "org.apache.cxf.dosgi.discovery.zookeeper.server");    
    }
    
    @SuppressWarnings("unchecked")
    private void setDefault(Dictionary dict, String key, String value) {
        if (dict.get(key) == null) {
            dict.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void updated(Dictionary dict) throws ConfigurationException {
        if (dict == null) {
            shutdown();
            return;
        }
        
        if (main != null) {
            // stop the current instance            
            shutdown();
            // then reconfigure and start again.
        }
        
        if (dict.get("clientPort") == null) {
            LOG.info("Ignoring configuration update because required property 'clientPort' isn't set.");
            return;
        }
        
        Properties props = new Properties();
        for (Enumeration e = dict.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            props.put(key, dict.get(key));
        }
        
        try {
            setDefaults(props);
            if (serviceRegistration != null) {
                serviceRegistration.setProperties(props);
            }
            
            QuorumPeerConfig config = new QuorumPeerConfig();
            config.parseProperties(props);
            final ServerConfig serverConfig = new ServerConfig();
            serverConfig.readFrom(config);
            
            main = getZooKeeperMain();
            zkMainThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        main.runFromConfig(serverConfig);
                    } catch (IOException e) {
                        LOG.error("Problem running ZooKeeper server.", e);
                    }                    
                }                
            });
            startThread();
            
            LOG.info("Applied configuration update :" + props);
        } catch (Exception th) {
            LOG.error("Problem applying configuration update: " + props, th);            
        }
    }

    // Isolated for testing
    void startThread() {
        zkMainThread.start();
    }

    // Isolated for testing
    MyZooKeeperServerMain getZooKeeperMain() {
        return new MyZooKeeperServerMain();
    }

    public void setRegistration(ServiceRegistration reg) {
        serviceRegistration = reg;
    }
    
    static class MyZooKeeperServerMain extends ZooKeeperServerMain {
        @Override
        protected void shutdown() {
            super.shutdown();
            // Make the shutdown accessible from here.
        }        
    }
}
