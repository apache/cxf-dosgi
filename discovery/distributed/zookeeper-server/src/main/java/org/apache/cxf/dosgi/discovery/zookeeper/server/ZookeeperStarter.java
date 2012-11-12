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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;

public class ZookeeperStarter implements org.osgi.service.cm.ManagedService {
    private static final Logger LOG = Logger.getLogger(ZookeeperStarter.class);

    private final BundleContext bundleContext;
    protected ZookeeperServer main;
    private Thread zkMainThread;

    public ZookeeperStarter(BundleContext ctx) {
        bundleContext = ctx;
    }

    synchronized void shutdown() {
        if (main != null) {
            LOG.info("Shutting down ZooKeeper server");
            try {
                main.shutdown();
                if (zkMainThread != null) {
                    zkMainThread.join();
                }
            } catch (Throwable e) {
                LOG.log(Level.ERROR, e.getMessage(), e);
            }
            main = null;
            zkMainThread = null;
        }
    }

    public void setDefaults(Dictionary<String, Object> dict) throws IOException {
        removeEmptyValues(dict);
        setDefault(dict, "tickTime", "2000");
        setDefault(dict, "initLimit", "10");
        setDefault(dict, "syncLimit", "5");
        setDefault(dict, "clientPort", "2181");
        setDefault(dict, "dataDir", new File(bundleContext.getDataFile(""), "zkdata").getCanonicalPath());
    }

    /**
     * Remove empty values to avoid NumberFormatExceptions
     * 
     * @param dict
     */
    private void removeEmptyValues(Dictionary<String, Object> dict) {
        List<String> keysToRemove = new ArrayList<String>();
        Enumeration<String> keys = dict.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Object value = dict.get(key);
            if (value != null && value instanceof String && "".equals(value)) {
                keysToRemove.add(key);
            }
        }
        for (String key : keysToRemove) {
            dict.remove(key);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void setDefault(Dictionary dict, String key, String value) {
        if (dict.get(key) == null) {
            dict.put(key, value);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized void updated(Dictionary dict) throws ConfigurationException {
        shutdown();
        if (dict == null) {
            return;
        }
        try {
            setDefaults(dict);
            QuorumPeerConfig config = parseConfig(dict);
            startFromConfig(config);
            LOG.info("Applied configuration update :" + dict);
        } catch (Exception th) {
            LOG.error("Problem applying configuration update: " + dict, th);
        }
    }

    @SuppressWarnings("rawtypes")
    private QuorumPeerConfig parseConfig(Dictionary dict) throws IOException, ConfigException {
        Properties props = new Properties();
        for (Enumeration e = dict.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            props.put(key, dict.get(key));
        }
        QuorumPeerConfig config = new QuorumPeerConfig();
        config.parseProperties(props);
        return config;
    }

    protected void startFromConfig(final QuorumPeerConfig config) throws IOException, InterruptedException {
        int numServers = config.getServers().size();
        if (numServers > 1) {
            main = new MyQuorumPeerMain(config);
        } else {
            main = new MyZooKeeperServerMain(config);
        }
        zkMainThread = new Thread(new Runnable() {
            public void run() {
                try {
                    main.startup();
                } catch (Throwable e) {
                    LOG.error("Problem running ZooKeeper server.", e);
                }
            }
        });
        zkMainThread.start();
    }
    
    interface ZookeeperServer {
            void startup() throws IOException;
            void shutdown();
    }

    static class MyQuorumPeerMain extends QuorumPeerMain implements ZookeeperServer {
        private QuorumPeerConfig config;

        public MyQuorumPeerMain(QuorumPeerConfig config) {
            this.config = config;
        }

        public void startup() throws IOException {
            runFromConfig(config);
        }

        public void shutdown() {
            if (null != quorumPeer) {
                quorumPeer.shutdown();
            }
        }
    }

    static class MyZooKeeperServerMain extends ZooKeeperServerMain implements ZookeeperServer {
        private QuorumPeerConfig config;

        public MyZooKeeperServerMain(QuorumPeerConfig config) {
            this.config = config;
        }
        
        public void startup() throws IOException {
            ServerConfig serverConfig = new ServerConfig();
            serverConfig.readFrom(config);
            runFromConfig(serverConfig );
        }

        public void shutdown() {
            super.shutdown();
        }
    }

}
