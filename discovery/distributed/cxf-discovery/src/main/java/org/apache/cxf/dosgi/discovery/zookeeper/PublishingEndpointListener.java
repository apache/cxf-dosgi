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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.discovery.local.LocalDiscoveryUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Listens for local Endpoints and publishes them to Zookeeper
 */
public class PublishingEndpointListener implements EndpointListener {
    private final Logger LOG = Logger.getLogger(PublishingEndpointListener.class.getName());

    private final ZooKeeper zookeeper;
    private final List<DiscoveryPlugin> discoveryPlugins = new CopyOnWriteArrayList<DiscoveryPlugin>();
    private final ServiceTracker discoveryPluginTracker;
    private final List<EndpointDescription> endpoints = new ArrayList<EndpointDescription>();
    private boolean closed = false;

    public PublishingEndpointListener(ZooKeeper zooKeeper, BundleContext bctx) {
        this.zookeeper = zooKeeper;

        discoveryPluginTracker = new ServiceTracker(bctx, DiscoveryPlugin.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                Object svc = super.addingService(reference);
                if (svc instanceof DiscoveryPlugin) {
                    discoveryPlugins.add((DiscoveryPlugin) svc);
                }
                return svc;
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                discoveryPlugins.remove(service);
                super.removedService(reference, service);
            }
        };
        discoveryPluginTracker.open();
    }

    public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
        LOG.info("Local endpointDescription added: " + endpoint);

        if (closed)
            return;

        synchronized (endpoints) {

            if (endpoints.contains(endpoint)) {
                // TODO -> Should the published endpoint be updated here ?
                return;
            }

            try {

                Collection<String> interfaces = endpoint.getInterfaces();
                String endpointKey = getKey(endpoint.getId());

                for (String name : interfaces) {
                    Map<String, Object> props = new HashMap<String, Object>(endpoint.getProperties());
                    for (DiscoveryPlugin plugin : discoveryPlugins) {
                        endpointKey = plugin.process(props, endpointKey);
                    }

                    String path = Util.getZooKeeperPath(name);
                    ensurePath(path, zookeeper);

                    String fullPath = path + '/' + endpointKey;
                    LOG.fine("Creating ZooKeeper node: " + fullPath);
                    zookeeper.create(fullPath, getData(props), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                }

                endpoints.add(endpoint);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Exception while processing the addition of a ServicePublication.", ex);
            }
        }

    }

    public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
        LOG.info("Local endpointDescription removed: " + endpoint);

        if (closed)
            return;

        synchronized (endpoints) {
            if (!endpoints.contains(endpoint)) {
                return;
            }

            try {
                removeEndpoint(endpoint);

                endpoints.remove(endpoint);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Exception while processing the removal of a ServicePublication.", ex);
            }
        }

    }

    private void removeEndpoint(EndpointDescription endpoint) throws UnknownHostException,
        URISyntaxException, InterruptedException, KeeperException {
        Collection<String> interfaces = endpoint.getInterfaces();
        String endpointKey = getKey(endpoint.getId());

        for (String name : interfaces) {
            String path = Util.getZooKeeperPath(name);
            String fullPath = path + '/' + endpointKey;
            LOG.fine("Removing ZooKeeper node: " + fullPath);
            zookeeper.delete(fullPath, -1);
        }
    }

    private static void ensurePath(String path, ZooKeeper zk) throws KeeperException, InterruptedException {
        StringBuilder current = new StringBuilder();

        String[] tree = path.split("/");
        for (int i = 0; i < tree.length; i++) {
            if (tree[i].length() == 0) {
                continue;
            }

            current.append('/');
            current.append(tree[i]);
            if (zk.exists(current.toString(), false) == null) {
                zk.create(current.toString(), new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }
    }

    static byte[] getData(Map<String, Object> props) throws IOException {
        String s = LocalDiscoveryUtils.getEndpointDescriptionXML(props);
        return s.getBytes();
    }

    static String getKey(String endpoint) throws UnknownHostException, URISyntaxException {
        URI uri = new URI(endpoint);

        StringBuilder sb = new StringBuilder();
        sb.append(uri.getHost());
        sb.append("#");
        sb.append(uri.getPort());
        sb.append("#");
        sb.append(uri.getPath().replace('/', '#'));
        return sb.toString();
    }

    public void close() {
        LOG.fine("removing all service publications");
        synchronized (endpoints) {
            for (EndpointDescription ed : endpoints) {
                try {
                    removeEndpoint(ed);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Exception while processing the removal of a ServicePublication.",
                            ex);
                }
            }
            endpoints.clear();
        }
        discoveryPluginTracker.close();
    }
}
