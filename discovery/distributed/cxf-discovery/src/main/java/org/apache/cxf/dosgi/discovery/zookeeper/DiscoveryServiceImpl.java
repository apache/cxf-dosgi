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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.discovery.Discovery;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;

public class DiscoveryServiceImpl implements Discovery, InitializingBean, DisposableBean, BundleContextAware {
    private static final String SERVICE_PID = "org.apache.cxf.dosgi.discovery.zookeeper";

    private static final Logger LOG = Logger.getLogger(DiscoveryServiceImpl.class.getName());
    
    private String zooKeeperHost = "localhost";
    private int zooKeeperPort;
    private int zooKeeperTimeout = 10000;

    private BundleContext bundleContext;

    private ServiceRegistration reg;

    public String getZooKeeperHost() {
        return zooKeeperHost;
    }
    
    public void setZooKeeperHost(String host) {
        LOG.info("Setting host to: " + host);
        zooKeeperHost = host;
    }
    
    public int getZooKeeperPort() {
        return zooKeeperPort;
    }
    
    public void setZooKeeperPort(int port) {
        LOG.info("Setting port to: " + port);
        zooKeeperPort = port;
    }    

    public int getZooKeeperTimeout() {
        return zooKeeperTimeout;
    }
    
    public void setZooKeeperTimeout(int timeout) {
        LOG.info("Setting timeout to: " + timeout);
        zooKeeperTimeout = timeout;
    }

    @Override
    public void setBundleContext(BundleContext bc) {
        bundleContext = bc;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, SERVICE_PID);
        props.put("zookeeper.host", getZooKeeperHost());
        props.put("zookeeper.port", getZooKeeperPort());
        props.put("zookeeper.timeout", getZooKeeperTimeout());
        reg = bundleContext.registerService(Discovery.class.getName(), this, props);
    }

    @Override
    public void destroy() throws Exception {
        reg.unregister();
    }    
}
