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
package org.apache.cxf.dosgi.discovery.zookeeper.server.config;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
    private static final String ZOOKEEPER_PORT = "org.apache.cxf.dosgi.discovery.zookeeper.port";
    private static final String PID = "org.apache.cxf.dosgi.discovery.zookeeper.server";
    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> st;

    public void start(BundleContext context) throws Exception {
        synchronized (Activator.class) {
            // Only one thread gets to set the port number
            if (System.getProperty(ZOOKEEPER_PORT) == null) {
                String port = getFreePort();
                System.setProperty(ZOOKEEPER_PORT, port);
                LOG.info("Global ZooKeeper port: {}", port);
            }
        }

        st = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(context, ConfigurationAdmin.class, null) {
            @Override
            public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
                ConfigurationAdmin service = super.addingService(reference);
                try {
                    Configuration cfg = service.getConfiguration(PID, null);
                    Dictionary<String, Object> props = new Hashtable<String, Object>();
                    String zp = System.getProperty(ZOOKEEPER_PORT);
                    props.put("clientPort", zp);
                    cfg.update(props);
                    LOG.debug("Set ZooKeeper client port to {}", zp);
                } catch (IOException e) {
                    LOG.error("Failed to configure ZooKeeper server!", e);
                }
                return service;
            }
        };
        st.open();

        // The following section is done synchronously otherwise it doesn't happen in time for the CT
        ServiceReference[] refs = context.getServiceReferences(ManagedService.class.getName(),
                "(service.pid=org.apache.cxf.dosgi.discovery.zookeeper)");
        if (refs == null || refs.length == 0) {
            throw new RuntimeException("This bundle must be started after the bundle with the ZooKeeper "
                                       + "Discovery Managed Service was started.");
        }

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("zookeeper.host", "127.0.0.1");
        props.put("zookeeper.port", System.getProperty(ZOOKEEPER_PORT));

        ManagedService ms = (ManagedService) context.getService(refs[0]);
        try {
            ms.updated(props);
        } finally {
            if (ms != null) {
                context.ungetService(refs[0]);
            }
        }
        LOG.debug("Passed the zookeeper.host property to the ZooKeeper Client managed service.");
    }

    private String getFreePort() {
        try {
            ServerSocket ss = new ServerSocket(0);
            String port = "" + ss.getLocalPort();
            ss.close();
            return port;
        } catch (IOException e) {
            LOG.error("Failed to find a free port!", e);
            return null;
        }
    }

    public void stop(BundleContext context) throws Exception {
        st.close();
    }
}
