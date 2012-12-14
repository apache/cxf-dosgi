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
package org.apache.cxf.dosgi.samples.ds.consumer;

import org.apache.cxf.dosgi.samples.ds.AdderService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This Activator simply registers a service tracker to indicate its interest in the
 * AdderService which causes the service to get registered by the Listener Hook.
 * It is a workaround for the problem that the current ListenerHook is incompatible
 * with the Equinox DS implementation which doesn't specify a filter when looking up 
 * a service. See also DOSGI-73.
 */
public class Activator implements BundleActivator {    
    private ServiceTracker tracker;

    public void start(BundleContext context) throws Exception {
        tracker = new ServiceTracker(context, AdderService.class.getName(), null);
        tracker.open();
    }

    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }
}
