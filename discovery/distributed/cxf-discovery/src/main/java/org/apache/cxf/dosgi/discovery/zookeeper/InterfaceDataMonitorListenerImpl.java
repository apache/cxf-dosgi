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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.discovery.local.LocalDiscoveryUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.jdom.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
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
    final InterfaceMonitor parent;

    private final BundleContext bctx;

    // This map is *only* accessed in the change() method
    private Map<String, Map<String, Object>> nodes = new HashMap<String, Map<String, Object>>();

    public InterfaceDataMonitorListenerImpl(ZooKeeper zk, String intf,
                                            EndpointListenerTrackerCustomizer.Interest dst, String scope,
                                            BundleContext bc, InterfaceMonitor interfaceMonitor) {
        parent = interfaceMonitor;
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

        LOG.fine("InterfaceDataMonitorListenerImpl is recursive: " + recursive);
    }

    public synchronized void change() {
        Map<String, Map<String, Object>> newNodes = new HashMap<String, Map<String, Object>>();
        Map<String, Map<String, Object>> prevNodes = nodes;
        LOG.info("Zookeeper callback on node: " + znode);

        processChildren(znode, newNodes, prevNodes);

        LOG.fine("processChildren done nodes that are missing now and need to be removed: "
                 + prevNodes.values());

        for (Map<String, Object> props : prevNodes.values()) {
            // whatever's left in prevNodes now has been removed from Discovery
            EndpointDescription epd = new EndpointDescription(props);

            //notifyListeners(epd, true);

            for (ServiceReference sref : discoveredServiceTracker.relatedServiceListeners) {
                if (bctx.getService(sref) instanceof EndpointListener) {
                    EndpointListener epl = (EndpointListener)bctx.getService(sref);

                    // return the >first< matching scope of the listener
                    // TODO: this code also exists for the endpoint adding in the processChild() method ->
                    // refactor !
                    String[] scopes = Util.getScopes(sref);
                    for (final String currentScope : scopes) {
                        LOG.fine("matching " + epd + " against " + currentScope);
                        Filter f = null;
                        try {
                            f = FrameworkUtil.createFilter(currentScope);

                            Dictionary d = new Properties();
                            Set<Map.Entry<String, Object>> entries = props.entrySet();
                            for (Map.Entry<String, Object> entry : entries) {
                                d.put(entry.getKey(), entry.getValue());
                            }

                            if (f.match(d)) {
                                LOG.fine("MATCHED " + epd + "against " + currentScope);
                                LOG.info("calling EndpointListener endpointRemoved: " + epl + "from bundle "
                                         + sref.getBundle().getSymbolicName() + " for endpoint: " + epd);

                                epl.endpointRemoved(epd, currentScope);
                                break;
                            }
                        } catch (InvalidSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }


        }
        nodes = newNodes;
    }

    /**
     * iterates through all child nodes of the given node and tries to find endpoints. If the recursive flag
     * is set it also traverses into the child nodes.
     * 
     * @return true if an endpoint was found and if the node therefore needs to be monitored for changes
     */
    private boolean processChildren(String znode, Map<String, Map<String, Object>> newNodes,
                                    Map<String, Map<String, Object>> prevNodes) {

        List<String> children;
        try {
            LOG.info("Processing the children of " + znode);
            children = zookeeper.getChildren(znode, false);

            boolean foundANode = false;
            for (String child : children) {

                Map<String, Object> p = processChild(znode, child, prevNodes.get(child));
                if (p != null) {
                    LOG.fine("found new node " + znode + "/[" + child + "]   ( []->child )  props: "
                             + p.values());
                    newNodes.put(child, p);
                    prevNodes.remove(child);
                    foundANode = true;
                }
                if (recursive) {
                    String newNode = znode + '/' + child;
                    if (processChildren(newNode, newNodes, prevNodes))
                        zookeeper.getChildren(newNode, parent);
                }
            }

            return foundANode;
        } catch (KeeperException e) {
            LOG.log(Level.SEVERE, "Problem processing Zookeeper node: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            LOG.log(Level.SEVERE, "Problem processing Zookeeper node: " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Scan the node data for Endpoint information and publish it to the related service listeners
     * 
     * @return the properties of the endpoint found in the node or null if no endpoint was found
     */
    private Map<String, Object> processChild(String znode, String child, Map<String, Object> prevVal) {

        String node = znode + '/' + child;

        try {
            Stat s = zookeeper.exists(node, false);
            if (s.getDataLength() <= 0) {
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
                 notifyListeners(epd, false);

            } else if (!prevVal.equals(epd.getProperties())) {
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

    private void notifyListeners(EndpointDescription epd, boolean isRemoval) {

        System.out.println("****************  notifyListeners("+epd+"  ,  "+isRemoval+")");
        
        for (ServiceReference sref : discoveredServiceTracker.relatedServiceListeners) {
            if (bctx.getService(sref) instanceof EndpointListener) {
                final EndpointListener epl = (EndpointListener)bctx.getService(sref);

                String[] scopes = Util.getScopes(sref);
                for (final String currentScope : scopes) {
                    Filter f;
                    try {
                        f = FrameworkUtil.createFilter(currentScope);

                        Dictionary d = new Properties();
                        Map<String, Object> props = epd.getProperties();
                        Set<Map.Entry<String, Object>> entries = props.entrySet();
                        for (Map.Entry<String, Object> entry : entries) {
                            d.put(entry.getKey(), entry.getValue());
                        }

                        LOG.fine("matching " + epd + " against " + currentScope);

                        if (f.match(d)) {
                            LOG.fine("MATCHED " + epd + "against " + currentScope);

                            LOG.info("scheduling EndpointListener call for listener ; " + epl
                                     + "  from bundle  " + sref.getBundle().getSymbolicName()
                                     + " based on scope [" + currentScope + "]");

                            if (isRemoval)
                                epl.endpointRemoved(epd, currentScope);
                            else
                                epl.endpointAdded(epd, currentScope);

                            break;
                        }
                    } catch (InvalidSyntaxException e) {
                        LOG.warning("skipping scope [" + currentScope
                                    + "] of endpoint listener from bundle "+sref.getBundle().getSymbolicName()+" becaue it is invalid: " + e.getMessage());
                    }
                }

            }
        }
    }
}
