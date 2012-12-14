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
package org.apache.cxf.dosgi.systests2.multi.customintent.service;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class GreeterServiceWithCustomIntentActivator implements BundleActivator {

    public void start(BundleContext context) throws Exception {
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, "*");
        props.put(RemoteConstants.SERVICE_EXPORTED_CONFIGS, "org.apache.cxf.ws");
        props.put("org.apache.cxf.ws.address", "http://localhost:9090/greeter");
        props.put(RemoteConstants.SERVICE_EXPORTED_INTENTS, "myIntent");
        context.registerService(GreeterService.class.getName(), new EmptyGreeterService(), props);
    }

    public void stop(BundleContext context) throws Exception {
    }
    
}
