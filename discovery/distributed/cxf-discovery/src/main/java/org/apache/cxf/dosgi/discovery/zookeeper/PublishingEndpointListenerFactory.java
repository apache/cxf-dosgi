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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates local Endpointlisteners that publish to Zookeeper 
 */
public class PublishingEndpointListenerFactory implements ServiceFactory {
    public static final String DISCOVERY_ZOOKEEPER_ID = "org.apache.cxf.dosgi.discovery.zookeeper";
    private static final Logger LOG = LoggerFactory.getLogger(PublishingEndpointListenerFactory.class);

    private BundleContext bctx;
    private ZooKeeper zookeeper;
    private List<PublishingEndpointListener> listeners = new ArrayList<PublishingEndpointListener>();
    private ServiceRegistration serviceRegistartion;

    public PublishingEndpointListenerFactory(ZooKeeper zooKeeper, BundleContext bctx) {
        this.bctx = bctx;
        this.zookeeper = zooKeeper;
    }

    public Object getService(Bundle b, ServiceRegistration sr) {
        LOG.debug("new EndpointListener from factory");
        synchronized (listeners) {
            PublishingEndpointListener epl = new PublishingEndpointListener(zookeeper, bctx);
            listeners.add(epl);
            return epl;
        }
    }

    public void ungetService(Bundle b, ServiceRegistration sr, Object s) {
        LOG.debug("remove EndpointListener");
        synchronized (listeners) {
            if (listeners.contains(s)) {
                PublishingEndpointListener epl = (PublishingEndpointListener)s;
                epl.close();
                listeners.remove(epl);
            }
        }
    }

    public synchronized void start() {
        Properties props = new Properties();
        props.put(EndpointListener.ENDPOINT_LISTENER_SCOPE, "(&(" + Constants.OBJECTCLASS + "=*)("+RemoteConstants.ENDPOINT_FRAMEWORK_UUID+"="+Util.getUUID(bctx)+"))");
        props.put(DISCOVERY_ZOOKEEPER_ID, "true");
        serviceRegistartion = bctx.registerService(EndpointListener.class.getName(), this, props);
    }

    public synchronized void stop() {
        if (serviceRegistartion != null) {
            serviceRegistartion.unregister();
        }
        
        for (PublishingEndpointListener epl : listeners) {
            epl.close();
        }
    }

    /**
     * only for the test case !
     */
    protected List<PublishingEndpointListener> getListeners(){
        return listeners;
    }
    
}
