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
package org.apache.cxf.dosgi.singlebundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregatedActivator implements BundleActivator {
    static final String HTTP_PORT_PROPERTY = "org.osgi.service.http.port";
    static final String HTTPS_PORT_PROPERTY = "org.osgi.service.http.port.secure";
    static final String HTTPS_ENABLED_PROPERTY = "org.osgi.service.http.secure.enabled";
    static final String ACTIVATOR_RESOURCE = "activators.list";
    static String defaultHttpPort = "8080";

    private static final Logger LOG = LoggerFactory.getLogger(AggregatedActivator.class);

    
    
    private List<BundleActivator> activators = new ArrayList<BundleActivator>(); 

    public void start(BundleContext ctx) throws Exception {
        setHttpServicePort(ctx);
        startEmbeddedActivators(ctx);
    }

    public void stop(BundleContext ctx) throws Exception {
        stopEmbeddedActivators(ctx);
    }

    void setHttpServicePort(BundleContext ctx) {
        boolean https = false;
        String port;
        if ("true".equalsIgnoreCase(ctx.getProperty(HTTPS_ENABLED_PROPERTY))) {
            https = true;
            port = ctx.getProperty(HTTPS_PORT_PROPERTY);            
        } else {
            port = ctx.getProperty(HTTP_PORT_PROPERTY);            
        }
        
        if (port == null || port.length() == 0) {
            port = tryPortFree(defaultHttpPort);
            if (port == null) {
                LOG.debug("Port {} is not available. ", defaultHttpPort);
                port = tryPortFree("0");
            }
            LOG.info("Setting HttpService port to: " + port);
            
            String prop = https ? HTTPS_PORT_PROPERTY : HTTP_PORT_PROPERTY;
            System.setProperty(prop, port);
        } else {
            if (tryPortFree(port) == null) {
                LOG.warn("The system is configured to use HttpService port {}. However this port is already in use.",
                         port);
            } else {
                LOG.info("HttpService using port: {}", port);
            }
        }
    }
    
    private String tryPortFree(String port) {
        int p = Integer.parseInt(port);
        
        ServerSocket s = null;
        try {
            s = new ServerSocket(p);
            return "" + s.getLocalPort(); 
        } catch (IOException e) {
            return null;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        
    }

    void startEmbeddedActivators(BundleContext ctx) throws Exception {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            for (String s : getActivators()) {
                try {
                    Class<?> clazz = getClass().getClassLoader().loadClass(s);
                    Object o = clazz.newInstance();
                    if (o instanceof BundleActivator) {
                        BundleActivator ba = (BundleActivator) o;
                        activators.add(ba);
                        ba.start(ctx);
                    }
                } catch (Throwable th) {
                    LOG.error("Failed to start Activator " + s, th);
                }
            }

            SPIActivator sba = new SPIActivator();
            sba.start(ctx);
            activators.add(sba);
            
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    void stopEmbeddedActivators(BundleContext ctx) throws Exception {
        for (BundleActivator ba : activators) {
            try {
                ba.stop(ctx);
            } catch (Throwable ex) {
                LOG.warn("BundleActivator {} can not be stopped", ba.getClass().getName());
            }
        }
    }
    
    static Collection<String> getActivators() throws IOException {
        List<String> bundleActivators = new ArrayList<String>();
        
        URL url = AggregatedActivator.class.getResource(ACTIVATOR_RESOURCE);
        if (url == null) {
            return Collections.emptyList();
        }
        
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        String line = null;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() > 0) {
                bundleActivators.add(line);
            }
        }
        
        return bundleActivators;
    }
    
}
