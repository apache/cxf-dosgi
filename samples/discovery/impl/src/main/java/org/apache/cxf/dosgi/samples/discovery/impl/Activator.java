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
package org.apache.cxf.dosgi.samples.discovery.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.cxf.dosgi.samples.discovery.DisplayService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    private ServiceRegistration reg;

    public void start(BundleContext bc) throws Exception {        
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        
        String host = getHostName();
        int port = getPort();
        
        props.put("service.exported.interfaces", "*");
        props.put("service.exported.configs", "org.apache.cxf.ws");
        props.put("org.apache.cxf.ws.address", getAddress(host, port)); // old obsolete value
        props.put("endpoint.id", getAddress(host, port));

        reg = bc.registerService(DisplayService.class.getName(), 
                new DisplayServiceImpl(host + ":" + port), props);
    }

    private static String getAddress(String host, int port) throws Exception {        
        return "http://" + host + ":" + port + "/display";
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    private static int getPort() throws IOException {
        return new ServerSocket(0).getLocalPort();
    }

    public void stop(BundleContext bc) throws Exception {
        reg.unregister();
    }

}
