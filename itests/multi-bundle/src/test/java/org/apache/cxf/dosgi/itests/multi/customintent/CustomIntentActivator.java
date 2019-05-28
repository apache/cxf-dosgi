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
package org.apache.cxf.dosgi.itests.multi.customintent;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.cxf.dosgi.itests.multi.DummyTaskServiceImpl;
import org.apache.cxf.dosgi.samples.soap.TaskService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class CustomIntentActivator implements BundleActivator {

    public void start(BundleContext context) throws Exception {
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("org.apache.cxf.dosgi.IntentName", "myIntent");
        context.registerService(CustomFeatureProvider.class, new CustomFeatureProvider(), props);

        Dictionary<String, String> props2 = new Hashtable<String, String>();
        props2.put(RemoteConstants.SERVICE_EXPORTED_CONFIGS, "org.apache.cxf.ws");
        props2.put("org.apache.cxf.ws.address", "/taskservice");
        props2.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, "*");
        props2.put(RemoteConstants.SERVICE_EXPORTED_INTENTS, "myIntent");
        context.registerService(TaskService.class, new DummyTaskServiceImpl(), props2);
    }

    public void stop(BundleContext context) throws Exception {
    }
}
