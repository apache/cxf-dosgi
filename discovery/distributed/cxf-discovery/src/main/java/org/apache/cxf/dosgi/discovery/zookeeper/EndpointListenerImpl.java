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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class EndpointListenerImpl implements EndpointListener {

    private Logger LOG = Logger.getLogger(EndpointListenerImpl.class.getName());

    private ZooKeeperDiscovery discovery;
    private BundleContext bctx;
    
    private List<EndpointDescription> endpoints = new ArrayList<EndpointDescription>();

    private boolean closed = false;

    public EndpointListenerImpl(ZooKeeperDiscovery zooKeeperDiscovery, BundleContext bctx) {
        this.bctx = bctx;
        discovery = zooKeeperDiscovery;
    }

    private ZooKeeper getZooKeeper() {
        return discovery.getZookeeper();
    }

    public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
        LOG.info("endpointDescription added: " + endpoint);

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

                ZooKeeper zk = getZooKeeper();
                for (String name : interfaces) {
                    String path = Util.getZooKeeperPath(name);
                    String fullPath = path + '/' + endpointKey;
                    LOG.info("Creating ZooKeeper node: " + fullPath);

                    ensurePath(path, zk);
                    zk.create(fullPath, getData(endpoint), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                }

                endpoints.add(endpoint);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Exception while processing the addition of a ServicePublication.", ex);
            }
        }

    }

    public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
        LOG.info("endpointDescription removed: " + endpoint);

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

        ZooKeeper zk = getZooKeeper();
        for (String name : interfaces) {
            String path = Util.getZooKeeperPath(name);
            String fullPath = path + '/' + endpointKey;
            LOG.fine("Removing ZooKeeper node: " + fullPath);
            zk.delete(fullPath, -1);
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

    static byte[] getData(EndpointDescription sr) throws IOException {
        Properties p = new Properties();

        Map<String, Object> serviceProps = (Map<String, Object>)sr.getProperties();
        if (serviceProps != null) {
            for (Map.Entry<String, Object> prop : serviceProps.entrySet()) {
                Object val = prop.getValue();
                if (val == null) {
                    // null values are not allowed
                    continue;
                }
                p.setProperty(prop.getKey(), val.toString());
            }
        }

        {
            String[] oc = (String[])serviceProps.get(Constants.OBJECTCLASS);
            if (oc.length > 0)
                p.put(Constants.OBJECTCLASS, oc[0]);
        }

        // Marc: FIXME: What is/was ths good for ??!?!?
        // copyProperty(ServicePublication.ENDPOINT_SERVICE_ID, sr, p, host);
        // copyProperty(ServicePublication.ENDPOINT_LOCATION, sr, p, host);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        p.store(baos, "");
        return baos.toByteArray();
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
    }

}
