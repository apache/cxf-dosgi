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
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointListener;

public class EndpointListenerFactory implements ServiceFactory {

    private Logger LOG = Logger.getLogger(EndpointListenerFactory.class.getName());
    private BundleContext bctx;
    private ZooKeeperDiscovery discovery;
    private List<EndpointListenerImpl> listeners = new ArrayList<EndpointListenerImpl>();
    private ServiceRegistration serviceRegistartion;

    public EndpointListenerFactory(ZooKeeperDiscovery zooKeeperDiscovery, BundleContext bctx) {
        this.bctx = bctx;
        discovery = zooKeeperDiscovery;
    }

    public Object getService(Bundle b, ServiceRegistration sr) {
        LOG.fine("new EndpointListener from factory");
        synchronized (listeners) {
            EndpointListenerImpl epl = new EndpointListenerImpl(discovery, bctx);
            listeners.add(epl);
            return epl;
        }

    }

    public void ungetService(Bundle b, ServiceRegistration sr, Object s) {
        LOG.fine("remove EndpointListener");
        synchronized (listeners) {
            if (listeners.contains(s)) {
                EndpointListenerImpl epl = (EndpointListenerImpl)s;
                epl.close();
                listeners.remove(epl);
            }

        }

    }

    public synchronized void start() {
        serviceRegistartion = bctx.registerService(EndpointListener.class.getName(), this, null);
        updateServiceRegistration();
    }

    private void updateServiceRegistration() {
        Properties props = new Properties();
        props.put(EndpointListener.ENDPOINT_LISTENER_SCOPE, "(" + Constants.OBJECTCLASS + "=*)");
        serviceRegistartion.setProperties(props);
    }

    public synchronized void stop() {
        if (serviceRegistartion != null)
            serviceRegistartion.unregister();
        
        for (EndpointListenerImpl epl : listeners) {
            epl.close();
        }
    }

}
