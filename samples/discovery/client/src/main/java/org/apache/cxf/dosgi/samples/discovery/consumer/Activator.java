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
package org.apache.cxf.dosgi.samples.discovery.consumer;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.dosgi.samples.discovery.DisplayService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
    private ServiceTracker tracker;
    private Map<DisplayService, String> displays = new ConcurrentHashMap<DisplayService, String>();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> handle;

    public void start(BundleContext bc) throws Exception {
        tracker = new ServiceTracker(bc, DisplayService.class.getName(), null) {
            public Object addingService(ServiceReference reference) {
                Object svc = super.addingService(reference);
                if (svc instanceof DisplayService) {
                    DisplayService d = (DisplayService) svc;
                    System.out.println("Adding display: " + d.getID() + " (" + d + ")");
                    displays.put(d, d.getID());
                }
                return svc;
            }

            public void removedService(ServiceReference reference, Object service) {
                String value = displays.remove(service);
                System.out.println("Removed display: " + value);
                super.removedService(reference, service);
            }            
        };        
        tracker.open();
        
        scheduler = Executors.newScheduledThreadPool(1);
        Runnable printer = new Runnable() {
            int counter;
            public void run() {
                counter++;
                String text = "some text " + counter;
                System.out.println("Sending text to displays: " + text);
                for (Iterator<Entry<DisplayService, String>> it = displays.entrySet().iterator(); it.hasNext();) {
                    Entry<DisplayService, String> entry = it.next();
                    try {
                        entry.getKey().displayText(text);
                    } catch (Throwable th) {
                        System.out.println("Could not send message to display: " + entry.getValue());
                    }
                }
            }            
        };
        handle = scheduler.scheduleAtFixedRate(printer, 5, 5, TimeUnit.SECONDS);
    }

    public void stop(BundleContext bc) throws Exception {
        handle.cancel(true);
        tracker.close();
    }
}
