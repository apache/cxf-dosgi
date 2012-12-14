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
package org.apache.cxf.dosgi.systests2.common.test2.server;

import java.net.ServerSocket;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.cxf.dosgi.systests2.common.test2.Test2Service;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class ServerActivator implements BundleActivator {    
    private ServiceRegistration reg;

    public void start(BundleContext bc) throws Exception {
        Test2Service svc = new Test2ServiceImpl();
                
        // Dynamically assign a free port
        int freePort = new ServerSocket(0).getLocalPort();
        String url = "http://localhost:" + freePort + "/test2";
        System.out.println("*** Server using URL: " + url);
        
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("service.exported.interfaces", "*");
        props.put("service.exported.configs", "org.apache.cxf.ws");
        props.put("endpoint.id", url);
        
        reg = bc.registerService(Test2Service.class.getName(), svc, props);
    }

    public void stop(BundleContext bc) throws Exception {
        reg.unregister();
    }
}
