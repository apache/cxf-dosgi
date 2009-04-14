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

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.service.discovery.DiscoveredServiceTracker;
import org.osgi.service.discovery.ServicePublication;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;

public class DiscoveryBean implements BundleContextAware, InitializingBean, DisposableBean, Watcher {
    private BundleContext bundleContext;
    private DiscoveryServiceImpl discoveryService;
    private ZooKeeper zooKeeper;

    private FindInZooKeeperCustomizer finderCustomizer;
    private ServiceTracker lookupTracker;
    private ServiceTracker publicationTracker;
        
    public void setBundleContext(BundleContext bc) {
        bundleContext = bc;
    }

    public void setDiscoveryServiceBean(DiscoveryServiceImpl discovery) {
        discoveryService = discovery;
    }

    public void afterPropertiesSet() throws Exception {
        String hostPort = discoveryService.getZooKeeperHost() + ":" + 
                          discoveryService.getZooKeeperPort();
        zooKeeper = new ZooKeeper(hostPort, discoveryService.getZooKeeperTimeout(), this);
        
        publicationTracker = new ServiceTracker(bundleContext, ServicePublication.class.getName(), 
                new PublishToZooKeeperCustomizer(bundleContext, zooKeeper));
        publicationTracker.open();
        
        finderCustomizer = new FindInZooKeeperCustomizer(bundleContext, zooKeeper);
        lookupTracker = new ServiceTracker(bundleContext, DiscoveredServiceTracker.class.getName(),
                finderCustomizer);
        lookupTracker.open();        
    }

    public void destroy() throws Exception {
        lookupTracker.close();
        publicationTracker.close();
        zooKeeper.close();
    }

    public void process(WatchedEvent event) {
        System.out.println("*** [Spring] process (dropped): " + event);
        // TODO do we need this? The zookeeper examples do this, but I'm unsure why...
        // finderCustomizer.process(event);
    }
}
