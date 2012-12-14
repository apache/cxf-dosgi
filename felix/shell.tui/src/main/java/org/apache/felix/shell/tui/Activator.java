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
package org.apache.felix.shell.tui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.felix.shell.ShellService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {
    private static final String RUN_CMD = "run ";

    private BundleContext context;
    private ShellTuiRunnable shellRunnable;
    private Thread thread;
    private ServiceReference shellRef;
    private ShellService shell;

    public void start(BundleContext bcontext) {
        context = bcontext;

        // Listen for registering/unregistering impl service.
        ServiceListener sl = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                synchronized (Activator.this) {
                    // Ignore additional services if we already have one.
                    if (event.getType() == ServiceEvent.REGISTERED && shellRef != null) {
                        return;
                    } else if (event.getType() == ServiceEvent.REGISTERED && shellRef == null) {
                        // Initialize the service if we don't have one.
                        initializeService();
                    } else if (event.getType() == ServiceEvent.UNREGISTERING
                        && event.getServiceReference().equals(shellRef)) {
                        
                        // Unget the service if it is unregistering.
                        context.ungetService(shellRef);
                        shellRef = null;
                        shell = null;
                        // Try to get another service.
                        initializeService();
                    }
                }
            }
        };
        try {
            context.addServiceListener(sl,
                                         "(objectClass="
                                             + org.apache.felix.shell.ShellService.class.getName() + ")");
        } catch (InvalidSyntaxException ex) {
            System.err.println("ShellTui: Cannot add service listener.");
            System.err.println("ShellTui: " + ex);
        }

        // Now try to manually initialize the impl service
        // since one might already be available.
        initializeService();

        // Start impl thread.
        shellRunnable = new ShellTuiRunnable();
        thread = new Thread(shellRunnable, "Felix Shell TUI");
        thread.start();
    }

    private synchronized void initializeService() {
        if (shell != null) {
            return;
        }
        shellRef = context.getServiceReference(org.apache.felix.shell.ShellService.class.getName());
        if (shellRef == null) {
            return;
        }
        shell = (ShellService)context.getService(shellRef);
    }

    public void stop(BundleContext bcontext) {
        if (shellRunnable != null) {
            shellRunnable.stop();
            thread.interrupt();
        }
    }

    private class ShellTuiRunnable implements Runnable {
        private boolean stop;

        public void stop() {
            stop = true;
        }

        public void run() {
            String line = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

            while (!stop) {
                System.out.print("-> ");

                try {
                    line = in.readLine();
                } catch (IOException ex) {
                    System.err.println("Could not read input, please try again.");
                    continue;
                }

                synchronized (Activator.this) {
                    if (shell == null) {
                        System.out.println("No impl service available.");
                        continue;
                    }

                    if (line == null) {
                        continue;
                    }

                    line = line.trim();

                    if (line.length() == 0) {
                        continue;
                    }

                    try {
                        if (line.startsWith(RUN_CMD)) {
                            String path = line.substring(RUN_CMD.length()).trim();
                            System.out.println("loading commands from: " + path);
                            File commands = new File(path);
                            if (commands.exists()) {
                                BufferedReader reader = new BufferedReader(new FileReader(commands));
                                String command = reader.readLine().trim();
                                while (command != null) {
                                    if (command.length() > 0) {
                                        System.out.println("\nexecuting: " + command);
                                        shell.executeCommand(command.trim(), System.out, System.err);
                                    }
                                    command = reader.readLine();
                                }
                                reader.close();
                            } else {
                                System.err.println(path + " not found");
                            }
                        } else {
                            shell.executeCommand(line, System.out, System.err);
                        }

                    } catch (Exception ex) {
                        System.err.println("ShellTui: " + ex);
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
