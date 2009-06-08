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
package org.apache.cxf.dosgi.discovery.singlebundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class AggregatedActivator implements BundleActivator {
    static final String ACTIVATOR_RESOURCE = "activators.list";

    private List<BundleActivator> activators = new ArrayList<BundleActivator>(); 

    public void start(BundleContext ctx) throws Exception {
        startEmbeddedActivators(ctx);
    }

    public void stop(BundleContext ctx) throws Exception {
        stopEmbeddedActivators(ctx);
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
                    th.printStackTrace();
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    void stopEmbeddedActivators(BundleContext ctx) throws Exception {
        for (BundleActivator ba : activators) {
            ba.stop(ctx);
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
