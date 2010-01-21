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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class Activator implements BundleActivator, ManagedService {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());

    private ZooKeeperDiscovery zkd;
    private Dictionary zkProperties;
    private BundleContext bctx;
    ServiceRegistration cmReg;

    public synchronized void start(BundleContext bc) throws Exception {
        bctx = bc;
        zkProperties = getCMDefaults();
        zkd = createZooKeeperDiscovery();

        cmReg = bc.registerService(ManagedService.class.getName(), this, zkProperties);
    }

    public synchronized void stop(BundleContext bc) throws Exception {
        cmReg.unregister();
        zkd.stop();
    }

    public synchronized void updated(Dictionary configuration) throws ConfigurationException {
        LOG.info("Received configuration update for Zookeeper Discovery: " + configuration);
        if (configuration == null)
            return;

        Dictionary effective = getCMDefaults();
        // apply all values on top of the defaults
        for (Enumeration e = configuration.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            if (key != null) {
                Object val = configuration.get(key);
                effective.put(key, val);
            }
        }

        if (zkProperties.equals(effective)) {
            LOG.info("properties haven't changed ...");
            return;
        }

        zkProperties = effective;
        cmReg.setProperties(zkProperties);

        synchronized (this) {
            zkd.stop();
            zkd = createZooKeeperDiscovery();
        }

        // call start in any case
        try {
            zkd.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Dictionary getCMDefaults() {
        Dictionary props = new Hashtable();
        props.put("zookeeper.timeout", "3000");
        props.put("zookeeper.port", "2181");
        props.put(Constants.SERVICE_PID, "org.apache.cxf.dosgi.discovery.zookeeper");
        return props;
    }

    // for testing
    protected ZooKeeperDiscovery createZooKeeperDiscovery() {
        return new ZooKeeperDiscovery(bctx, zkProperties);
    }

}
