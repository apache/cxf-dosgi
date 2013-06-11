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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.dosgi.discovery.local.util.EndpointUtils;
import org.apache.cxf.dosgi.discovery.zookeeper.util.Utils;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors ZooKeeper for changes in published endpoints.
 * <p>
 * Specifically, it monitors the node path associated with a given interface class,
 * whose data is a serialized version of an EndpointDescription, and notifies an
 * EndpointListener when changes are detected (which can then propagate the
 * notification to other EndpointListeners with a matching scope).
 */
public class InterfaceMonitor implements Watcher, StatCallback {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMonitor.class);

    private final String znode;
    private final ZooKeeper zookeeper;
    private final EndpointListener epListener;
    private final boolean recursive;
    private volatile boolean closed;

    // This map reference changes, so don't synchronize on it
    private Map<String, EndpointDescription> nodes = new HashMap<String, EndpointDescription>();

    public InterfaceMonitor(ZooKeeper zk, String intf, EndpointListener epListener, String scope) {
        this.zookeeper = zk;
        this.znode = Utils.getZooKeeperPath(intf);
        this.recursive = intf == null || intf.isEmpty();
        this.epListener = epListener;
        LOG.debug("Creating new InterfaceMonitor {} for scope [{}] and objectClass [{}]",
                new Object[] {recursive ? "(recursive)" : "", scope, intf});
    }

    public void start() {
        watch();
    }

    private void watch() {
        LOG.debug("registering a zookeeper.exists({}) callback", znode);
        zookeeper.exists(znode, this, this, null);
    }

    /**
     * Zookeeper Watcher interface callback.
     */
    public void process(WatchedEvent event) {
        LOG.debug("ZooKeeper watcher callback for event {}", event);
        processDelta();
    }

    /**
     * Zookeeper StatCallback interface callback.
     */
    @SuppressWarnings("deprecation")
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        LOG.debug("ZooKeeper callback on node: {} code: {}", znode, rc);

        switch (rc) {
        case Code.Ok:
        case Code.NoNode:
            processDelta();
            return;

        case Code.SessionExpired:
        case Code.NoAuth:
        case Code.ConnectionLoss:
            return;

        default:
            watch();
        }
    }

    private void processDelta() {
        if (closed) {
            return;
        }

        if (zookeeper.getState() != ZooKeeper.States.CONNECTED) {
            LOG.info("zookeeper connection was already closed! Not processing changed event.");
            return;
        }

        try {
            if (zookeeper.exists(znode, false) != null) {
                zookeeper.getChildren(znode, this);
                refreshNodes();
            } else {
                LOG.debug("znode {} doesn't exist -> not processing any changes", znode);
            }
        } catch (Exception e) {
            LOG.error("Error getting ZooKeeper data.", e);
        }
    }

    public synchronized void close() {
        closed = true;
        for (EndpointDescription epd : nodes.values()) {
            epListener.endpointRemoved(epd, null);
        }
        nodes.clear();
    }

    private synchronized void refreshNodes() {
        if (closed) {
            return;
        }
        LOG.info("Processing change on node: {}", znode);

        Map<String, EndpointDescription> newNodes = new HashMap<String, EndpointDescription>();
        Map<String, EndpointDescription> prevNodes = nodes;
        processChildren(znode, newNodes, prevNodes);

        // whatever is left in prevNodes now has been removed from Discovery
        LOG.debug("processChildren done. Nodes that are missing now and need to be removed: {}", prevNodes.values());
        for (EndpointDescription epd : prevNodes.values()) {
            epListener.endpointRemoved(epd, null);
        }
        nodes = newNodes;
    }

    /**
     * Iterates through all child nodes of the given node and tries to find
     * endpoints. If the recursive flag is set it also traverses into the child
     * nodes.
     *
     * @return true if an endpoint was found and if the node therefore needs to
     *         be monitored for changes
     */
    private boolean processChildren(String zn, Map<String, EndpointDescription> newNodes,
            Map<String, EndpointDescription> prevNodes) {
        List<String> children;
        try {
            LOG.debug("Processing the children of {}", zn);
            children = zookeeper.getChildren(zn, false);

            boolean foundANode = false;
            for (String child : children) {
                String childZNode = zn + '/' + child;
                EndpointDescription epd = getEndpointDescriptionFromNode(childZNode);
                if (epd != null) {
                    EndpointDescription prevEpd = prevNodes.get(child);
                    LOG.info("found new node " + zn + "/[" + child + "]   ( []->child )  props: "
                            + epd.getProperties().values());
                    newNodes.put(child, epd);
                    prevNodes.remove(child);
                    foundANode = true;
                    LOG.debug("Properties: {}", epd.getProperties());
                    if (prevEpd == null) {
                        // This guy is new
                        epListener.endpointAdded(epd, null);
                    } else if (!prevEpd.getProperties().equals(epd.getProperties())) {
                        // TODO
                    }
                }
                if (recursive && processChildren(childZNode, newNodes, prevNodes)) {
                    zookeeper.getChildren(childZNode, this);
                }
            }

            return foundANode;
        } catch (KeeperException e) {
            LOG.error("Problem processing Zookeeper node", e);
        } catch (InterruptedException e) {
            LOG.error("Problem processing Zookeeper node", e);
        }
        return false;
    }

    /**
     * Retrieves data from the given node and parses it into an EndpointDescription.
     *
     * @param node a node path
     * @return endpoint found in the node or null if no endpoint was found
     */
    private EndpointDescription getEndpointDescriptionFromNode(String node) {
        try {
            Stat s = zookeeper.exists(node, false);
            if (s.getDataLength() <= 0) {
                return null;
            }
            byte[] data = zookeeper.getData(node, false, null);
            LOG.debug("Got data for node: {}", node);

            EndpointDescription endpoint = EndpointUtils.getFirstEnpointDescription(data);
            if (endpoint != null) {
                return endpoint;
            }
            LOG.warn("No Discovery information found for node: {}", node);
        } catch (Exception e) {
            LOG.error("Problem getting EndpointDescription from node " + node, e);
        }
        return null;
    }
}
