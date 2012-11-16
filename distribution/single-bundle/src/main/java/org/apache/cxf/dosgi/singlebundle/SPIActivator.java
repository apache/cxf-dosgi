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
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.servicemix.specs.locator.OsgiLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPIActivator implements BundleActivator, SynchronousBundleListener {
    private static final Logger LOG = LoggerFactory.getLogger(SPIActivator.class);
    private ConcurrentMap<Long, Map<String, Callable<Class>>> factories = new ConcurrentHashMap<Long, Map<String, Callable<Class>>>();

    public synchronized void start(BundleContext bundleContext) throws Exception {
        register(bundleContext.getBundle());
    }

    public synchronized void stop(BundleContext bundleContext) throws Exception {
        while (!factories.isEmpty()) {
            unregister(factories.keySet().iterator().next());
        }
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.RESOLVED) {
            register(event.getBundle());
        } else if (event.getType() == BundleEvent.UNRESOLVED) {
            unregister(event.getBundle().getBundleId());
        }
    }

    protected void register(final Bundle bundle) {
        Map<String, Callable<Class>> map = factories.get(bundle.getBundleId());

        Vector<URL> v = new Vector<URL>();
//        try {
//            Resource[] resources = bundle.getResources("classpath*:META-INF/services/*");
//            for (Resource r : resources) {
//                v.add(r.getURL());
//            }
//        } catch (IOException e1) {
//            LOG.error("Failed to resolve service resources", e1);
//        }

        Enumeration<URL> e = v.elements();
        if (e != null) {
            while (e.hasMoreElements()) {
                final URL u = (URL) e.nextElement();
                final String url = u.toString();
                if (url.endsWith("/")) {
                    continue;
                }
                final String factoryId = url.substring(url.lastIndexOf("/") + 1);
                if (map == null) {
                    map = new HashMap<String, Callable<Class>>();
                    factories.put(bundle.getBundleId(), map);
                }
                map.put(factoryId, new Callable<Class>() {
                    public Class call() throws Exception {
                        BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream(), "UTF-8"));
                        String factoryClassName = br.readLine();
                        br.close();
                        return bundle.loadClass(factoryClassName);
                    }
                });
            }
        }
        if (map != null) {
            for (Map.Entry<String, Callable<Class>> entry : map.entrySet()) {
                OsgiLocator.register(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void unregister(long bundleId) {
        Map<String, Callable<Class>> map = factories.remove(bundleId);
        if (map != null) {
            for (Map.Entry<String, Callable<Class>> entry : map.entrySet()) {
                OsgiLocator.unregister(entry.getKey(), entry.getValue());
            }
        }
    }
}
