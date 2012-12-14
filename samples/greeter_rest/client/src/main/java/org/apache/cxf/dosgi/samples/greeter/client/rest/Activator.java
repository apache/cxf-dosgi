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
package org.apache.cxf.dosgi.samples.greeter.client.rest;

import javax.swing.JOptionPane;

import org.apache.cxf.dosgi.samples.greeter.rest.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.rest.GreeterInfo;
import org.apache.cxf.dosgi.samples.greeter.rest.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.rest.GreeterService2;
import org.apache.cxf.dosgi.samples.greeter.rest.GreetingPhrase;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {    
    private ServiceTracker tracker;
    private ServiceTracker tracker2;

    public void start(final BundleContext bc) {
        tracker = new ServiceTracker(bc, GreeterService.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                Object result = super.addingService(reference);

                useGreeterService(bc, reference);
                
                return result;
            }
        };
        tracker.open();
        
        tracker2 = new ServiceTracker(bc, GreeterService2.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                Object result = super.addingService(reference);

                useGreeterService2(bc, reference);
                
                return result;
            }
        };
        tracker2.open();
    }

    protected void useGreeterService(final BundleContext bc, ServiceReference reference) {
        Object svc = bc.getService(reference);
        if (!(svc instanceof GreeterService)) {
            return;
        }
        final GreeterService greeter = (GreeterService) svc;

        Thread t = new Thread(new Runnable() {
            public void run() {
                greeterUI(bc, greeter);
            }
        });
        t.start();
    }
    
    protected void useGreeterService2(final BundleContext bc, ServiceReference reference) {
        Object svc = bc.getService(reference);
        if (!(svc instanceof GreeterService2)) {
            return;
        }
        final GreeterService2 greeter = (GreeterService2) svc;

        Thread t = new Thread(new Runnable() {
            public void run() {
                greeter2UI(bc, greeter);
            }
        });
        t.start();
    }

    private void greeterUI(final BundleContext bc, final GreeterService greeter) {
        while (true) {
            System.out.println("*** Opening greeter client dialog ***");
            String name = JOptionPane.showInputDialog("Enter name:");
            if (name == null) {
                break;
            } else {
                System.out.println("*** Invoking greeter ***");
                try {
                    GreeterInfo info = greeter.greetMe(name);
    
                    System.out.println("greetMe(\"" + name + "\") returns:");
                    for (GreetingPhrase greeting: info.getGreetings()) {
                        System.out.println("  " + greeting.getPhrase() 
                                + " " + greeting.getName());
                    }
                } catch (GreeterException ex) {
                    System.out.println("GreeterException : " + ex.toString());
                }
            }
        }
    }
    
    private void greeter2UI(final BundleContext bc, final GreeterService2 greeter) {
        while (true) {
            System.out.println("*** Opening greeter2 client dialog ***");
            String name = JOptionPane.showInputDialog("Greeter2 : Enter name");
            if (name == null) {
                break;
            } else {
                System.out.println("*** Invoking greeter2 ***");
                try {
                    GreeterInfo info = greeter.greetMe(name);
    
                    System.out.println("greetMe(\"" + name + "\") returns:");
                    for (GreetingPhrase greeting: info.getGreetings()) {
                        System.out.println("  " + greeting.getPhrase() 
                                + " " + greeting.getName());
                    }
                } catch (GreeterException ex) {
                    System.out.println("GreeterException : " + ex.toString());
                }
            }
        }
    }

    public void stop(BundleContext bc) throws Exception {
        tracker.close();
        tracker2.close();
    }
}
