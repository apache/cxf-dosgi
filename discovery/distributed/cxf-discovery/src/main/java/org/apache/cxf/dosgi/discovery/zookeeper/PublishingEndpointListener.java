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

import org.apache.cxf.dosgi.discovery.local.util.EndpointUtils;
import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.zookeeper.KeeperException.NoNodeException;
import static org.apache.zookeeper.KeeperException.NodeExistsException;

/**
 * Listens for local Endpoints and publishes them to Zookeeper
 */
public class PublishingEndpointListener implements EndpointListener {

    private static final Logger LOG = LoggerFactory.getLogger(PublishingEndpointListener.class);

    private final ZooKeeper zookeeper;
    private final ServiceTracker discoveryPluginTracker;
    private final List<EndpointDescription> endpoints = new ArrayList<EndpointDescription>();
    private boolean closed;

    public PublishingEndpointListener(ZooKeeper zooKeeper, BundleContext bctx) {
        this.zookeeper = zooKeeper;
        discoveryPluginTracker = new ServiceTracker(bctx, DiscoveryPlugin.class.getName(), null);
        discoveryPluginTracker.open();
    }

    public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
        LOG.info("Local EndpointDescription added: {}", endpoint);

        synchronized (endpoints) {
            if (closed) {
                return;
            }
            if (endpoints.contains(endpoint)) {
                // TODO -> Should the published endpoint be updated here?
                return;
            }

            try {
                addEndpoint(endpoint);
                endpoints.add(endpoint);
            } catch (Exception ex) {
                LOG.error("Exception while processing the addition of an endpoint.", ex);
            }
        }
    }

    private void addEndpoint(EndpointDescription endpoint) throws URISyntaxException, KeeperException,
                                                                  InterruptedException, IOException {
        Collection<String> interfaces = endpoint.getInterfaces();
        String endpointKey = getKey(endpoint.getId());
        Map<String, Object> props = new HashMap<String, Object>(endpoint.getProperties());

        // process plugins
        Object[] plugins = discoveryPluginTracker.getServices();
        if (plugins != null) {
            for (Object plugin : plugins) {
                if (plugin instanceof DiscoveryPlugin) {
                    endpointKey = ((DiscoveryPlugin)plugin).process(props, endpointKey);
                }
            }
        }

        for (String name : interfaces) {
            String path = Utils.getZooKeeperPath(name);
            String fullPath = path + '/' + endpointKey;
            LOG.debug("Creating ZooKeeper node: {}", fullPath);
            ensurePath(path, zookeeper);
            createEphemeralNode(fullPath, getData(props));
        }
    }

    private void createEphemeralNode(String fullPath, byte[] data) throws KeeperException, InterruptedException {
        try {
            zookeeper.create(fullPath, data, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (NodeExistsException nee) {
            // this sometimes happens after a ZooKeeper node dies and the ephemeral node
            // that belonged to the old session was not yet deleted. We need to make our
            // session the owner of the node so it won't get deleted automatically -
            // we do this by deleting and recreating it ourselves.
            LOG.info("node for endpoint already exists, recreating: {}", fullPath);
            try {
                zookeeper.delete(fullPath, -1);
            } catch (NoNodeException nne) {
                // it's a race condition, but as long as it got deleted - it's ok
            }
            zookeeper.create(fullPath, data, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        }
    }

    public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
        LOG.info("Local EndpointDescription removed: {}", endpoint);

        synchronized (endpoints) {
            if (closed) {
                return;
            }
            if (!endpoints.contains(endpoint)) {
                return;
            }

            try {
                removeEndpoint(endpoint);
                endpoints.remove(endpoint);
            } catch (Exception ex) {
                LOG.error("Exception while processing the removal of an endpoint", ex);
            }
        }
    }

    private void removeEndpoint(EndpointDescription endpoint) throws UnknownHostException, URISyntaxException {
        Collection<String> interfaces = endpoint.getInterfaces();
        String endpointKey = getKey(endpoint.getId());

        for (String name : interfaces) {
            String path = Utils.getZooKeeperPath(name);
            String fullPath = path + '/' + endpointKey;
            LOG.debug("Removing ZooKeeper node: {}", fullPath);
            try {
                zookeeper.delete(fullPath, -1);
            } catch (Exception e) {
                LOG.debug("Error while removing endpoint");
            }
        }
    }

    private static void ensurePath(String path, ZooKeeper zk) throws KeeperException, InterruptedException {
        StringBuilder current = new StringBuilder();
        String[] parts = Utils.removeEmpty(path.split("/"));
        for (String part : parts) {
            current.append('/');
            current.append(part);
            try {
                zk.create(current.toString(), new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (NodeExistsException nee) {
                // it's not the first node with this path to ever exist - that's normal
            }
        }
    }

    static byte[] getData(Map<String, Object> props) {
        return EndpointUtils.getEndpointDescriptionXML(props).getBytes();
    }

    static String getKey(String endpoint) throws URISyntaxException {
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
        LOG.debug("closing - removing all endpoints");
        synchronized (endpoints) {
            closed = true;
            for (EndpointDescription ed : endpoints) {
                try {
                    removeEndpoint(ed);
                } catch (Exception ex) {
                    LOG.error("Exception while removing endpoint during close", ex);
                }
            }
            endpoints.clear();
        }
        discoveryPluginTracker.close();
    }
}
