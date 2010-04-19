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

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.discovery.local.LocalDiscoveryUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.jdom.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;

public class InterfaceDataMonitorListenerImpl implements DataMonitorListener {
    private static final Logger LOG = Logger.getLogger(InterfaceDataMonitorListenerImpl.class.getName());

    final ZooKeeper zookeeper;
    final String znode;
    final String interFace;
    final EndpointListenerTrackerCustomizer.Interest discoveredServiceTracker;
    final String scope;
    final boolean recursive;

    private final BundleContext bctx;

    // This map is *only* accessed in the change() method
    private Map<String, Map<String, Object>> nodes = new HashMap<String, Map<String, Object>>();

    public InterfaceDataMonitorListenerImpl(ZooKeeper zk, String intf,
                                            EndpointListenerTrackerCustomizer.Interest dst, String scope,
                                            BundleContext bc) {
        zookeeper = zk;
        znode = Util.getZooKeeperPath(intf);
        if (intf == null || "".equals(intf))
            recursive = true;
        else
            recursive = false;
        interFace = intf;
        discoveredServiceTracker = dst;
        bctx = bc;
        this.scope = scope;
    }

    public synchronized void change() {
        Map<String, Map<String, Object>> newNodes = new HashMap<String, Map<String, Object>>();
        Map<String, Map<String, Object>> prevNodes = nodes;
        LOG.info("Zookeeper callback on node: " + znode);

        processChildren(znode, newNodes, prevNodes);

        for (Map<String, Object> props : prevNodes.values()) {
            // whatever's left in prevNodes now has been removed from Discovery
            EndpointDescription epd = new EndpointDescription(props);

            for (ServiceReference sref : discoveredServiceTracker.relatedServiceListeners) {
                if (bctx.getService(sref) instanceof EndpointListener) {
                    EndpointListener epl = (EndpointListener)bctx.getService(sref);
                    LOG.info("calling EndpointListener endpointRemoved: " + epl + "from bundle "
                             + sref.getBundle().getSymbolicName());
                    epl.endpointRemoved(epd, scope);
                }
            }
        }

        nodes = newNodes;
    }

    private void processChildren(String znode, Map<String, Map<String, Object>> newNodes,
                                 Map<String, Map<String, Object>> prevNodes) {

        List<String> children;
        try {
            LOG.fine("Processing " + znode);
            children = zookeeper.getChildren(znode, false);

            for (String child : children) {

                Map<String, Object> p = processChild(znode, child, prevNodes.get(child));
                if (p != null) {
                    newNodes.put(child, p);
                    prevNodes.remove(child);
                }
                if (recursive) {
                    String newNode = znode + '/' + child;
                    processChildren(newNode, newNodes, prevNodes);
                }
            }

        } catch (KeeperException e) {
            LOG.log(Level.SEVERE, "Problem processing Zookeeper node: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            LOG.log(Level.SEVERE, "Problem processing Zookeeper node: " + e.getMessage(), e);
        }

    }

    private Map<String, Object> processChild(String znode, String child, Map<String, Object> prevVal) {

        String node = znode + '/' + child;

        try {
            Stat s = zookeeper.exists(node, false);
            if (s.getDataLength() <= 0) {
                LOG.fine(node + " does not contain any discovery data");
                return null;
            }
            byte[] data = zookeeper.getData(node, false, null);
            LOG.info("Child: " + node);

            List<Element> elements = LocalDiscoveryUtils.getElements(new ByteArrayInputStream(data));
            EndpointDescription epd = null;
            if (elements.size() > 0)
                epd = LocalDiscoveryUtils.getEndpointDescription(elements.get(0));
            else {
                LOG.warning("No Discovery information found for node: " + node);
                return null;
            }

            LOG.finest("Properties: " + epd.getProperties());

            if (prevVal == null) {
                // This guy is new

                for (ServiceReference sref : discoveredServiceTracker.relatedServiceListeners) {
                    if (bctx.getService(sref) instanceof EndpointListener) {
                        EndpointListener epl = (EndpointListener)bctx.getService(sref);

                        LOG.info("calling EndpointListener; " + epl + "from bundle "
                                 + sref.getBundle().getSymbolicName());

                        epl.endpointAdded(epd, scope);
                    }
                }
            } else if (!prevVal.equals(epd.getProperties())) {
                // There's been a modification
                // ServiceEndpointDescriptionImpl sed = new
                // ServiceEndpointDescriptionImpl(Collections.singletonList(interFace), m);
                // DiscoveredServiceNotification dsn = new
                // DiscoveredServiceNotificationImpl(Collections.emptyList(),
                // Collections.singleton(interFace), DiscoveredServiceNotification.MODIFIED, sed);
                // discoveredServiceTracker.serviceChanged(dsn);

                // TODO

            }

            return epd.getProperties();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Problem processing Zookeeper callback: " + e.getMessage(), e);
        }

        return null;
    }

    public void inform(ServiceReference sref) {
        LOG.fine("need to inform the service reference of maybe already existing endpoints");
        // TODO Auto-generated method stub

    }
}
