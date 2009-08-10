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
import java.util.Collection;
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
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.ServicePublication;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class PublishToZooKeeperCustomizer implements ServiceTrackerCustomizer {
    private static final Logger LOG = Logger.getLogger(PublishToZooKeeperCustomizer.class.getName());
    private static final Pattern LOCALHOST_MATCH = Pattern.compile("(.*://)(localhost|127.0.0.1)(:.*)");

    private final BundleContext bundleContext;
    private final ZooKeeper zookeeper;
    
    PublishToZooKeeperCustomizer(BundleContext bc, ZooKeeper zk) {
        bundleContext = bc;
        zookeeper = zk;
    }
    
    public Object addingService(ServiceReference sr) {
        try {
            Object obj = bundleContext.getService(sr);
            Collection<String> interfaces = Util.getMultiValueProperty(
                    sr.getProperty(ServicePublication.SERVICE_INTERFACE_NAME));
            String endpointKey = getKey(sr.getProperty(ServicePublication.ENDPOINT_LOCATION).toString());

            for (String name : interfaces) {
                String path = Util.getZooKeeperPath(name);
                String fullPath = path + '/' + endpointKey;
                LOG.info("Creating ZooKeeper node: " + fullPath);

                ensurePath(path);
                zookeeper.create(fullPath, getData(sr),
                        Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            }
            return obj;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception while processing the addition of a ServicePublication.", ex);
            return null;
        }
    }
    
    public void modifiedService(ServiceReference sr, Object obj) {
        removedService(sr, obj);
        addingService(sr);
    }

    public void removedService(ServiceReference sr, Object obj) {
        try {
            Collection<String> interfaces = Util.getMultiValueProperty(
                    sr.getProperty(ServicePublication.SERVICE_INTERFACE_NAME));
            String endpointKey = getKey(sr.getProperty(ServicePublication.ENDPOINT_LOCATION).toString());
            
            for (String name : interfaces) {
                String path = Util.getZooKeeperPath(name);
                String fullPath = path + '/' + endpointKey;
                LOG.info("Removing ZooKeeper node: " + fullPath);
                zookeeper.delete(fullPath, -1);                                
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception while processing the removal of a ServicePublication.", ex);
        }
    }

    void ensurePath(String path) throws KeeperException, InterruptedException {
        StringBuilder current = new StringBuilder();
        
        String[] tree = path.split("/");
        for (int i = 0; i < tree.length; i++) {
            if (tree[i].length() == 0) {
                continue;
            }
            
            current.append('/');
            current.append(tree[i]);
            if (zookeeper.exists(current.toString(), false) == null) {
                zookeeper.create(current.toString(), new byte [0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    static byte[] getData(ServiceReference sr) throws IOException {
        Properties p = new Properties();
        String host = InetAddress.getLocalHost().getHostAddress();
        
        Map<String, Object> serviceProps = (Map<String, Object>) sr.getProperty(ServicePublication.SERVICE_PROPERTIES);
        if (serviceProps != null) {
            for (Map.Entry<String, Object> prop : serviceProps.entrySet()) {
                p.setProperty(prop.getKey(), 
                    filterLocalHost(prop.getValue().toString(), host));
            }
        }
        
        copyProperty(ServicePublication.ENDPOINT_ID, sr, p, host);
        copyProperty(ServicePublication.ENDPOINT_LOCATION, sr, p, host);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        p.store(baos, "");
        return baos.toByteArray();
    }

    static String filterLocalHost(String value, String replacement) {
        Matcher m = LOCALHOST_MATCH.matcher(value);
        return m.replaceAll("$1" + replacement + "$3");
    }

    private static void copyProperty(String key, ServiceReference sr, Properties p, String localhost) {
        Object eID = sr.getProperty(key);
        if (eID != null) {
            p.setProperty(key, filterLocalHost(eID.toString(), localhost));
        }
    }

    static String getKey(String endpoint) throws UnknownHostException, URISyntaxException {
        URI uri = new URI(endpoint);
        if ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost())) {                
            uri = new URI(uri.getScheme(), uri.getUserInfo(), InetAddress.getLocalHost().getHostAddress(), 
                uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getHost());
        sb.append("#");
        sb.append(uri.getPort());
        sb.append("#");
        sb.append(uri.getPath().replace('/', '#'));
        return sb.toString();
    }
}
