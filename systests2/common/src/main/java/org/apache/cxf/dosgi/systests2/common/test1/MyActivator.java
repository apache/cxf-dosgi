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
package org.apache.cxf.dosgi.systests2.common.test1;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;

public class MyActivator implements BundleActivator {    
    private ServiceTracker startTracker;
    private ServiceTracker tracker;

    public void start(final BundleContext bc) throws Exception {
        Filter filter = bc.createFilter("(&(objectClass=java.lang.Object)(testName=test1))");
        tracker = new MyServiceTracker(bc);
        
        // The start tracker waits until a service from the test class is set before the 
        // 'MyServiceTracker' is activated.
        startTracker = new StartServiceTracker(bc, filter, tracker);
        startTracker.open();
    }
    
    public void stop(BundleContext bc) throws Exception {
        startTracker.close();
        tracker.close();
    }
}
