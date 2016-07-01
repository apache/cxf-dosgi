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
package org.apache.cxf.dosgi.systests2.multi;

import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class AbstractDosgiTest {
    static final int ZK_PORT = 35101;
    private static final int TIMEOUT = 20;
    
    @Inject
    BundleContext bundleContext;
    
    
    
    /**
     * Sleeps for a short interval, throwing an exception if timeout has been reached.
     * Used to facilitate a retry interval with timeout when used in a loop.
     *
     * @param startTime the start time of the entire operation in milliseconds
     * @param timeout the timeout duration for the entire operation in seconds
     * @param message the error message to use when timeout occurs
     * @throws InterruptedException if interrupted while sleeping
     */
    private static void sleepOrTimeout(long startTime, long timeout, String message) throws
            InterruptedException, TimeoutException {
        timeout *= 1000; // seconds to millis
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = timeout - elapsed;
        if (remaining <= 0) {
            throw new TimeoutException(message);
        }
        long interval = Math.min(remaining, 1000);
        Thread.sleep(interval);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    protected ServiceReference waitService(BundleContext bc, Class cls, String filter, int timeout)
        throws Exception {
        System.out.println("Waiting for service: " + cls + " " + filter);
        long startTime = System.currentTimeMillis();
        while (true) {
            Collection refs = bc.getServiceReferences(cls, filter);
            if (refs != null && refs.size() > 0) {
                return (ServiceReference)refs.iterator().next();
            }
            sleepOrTimeout(startTime, timeout, "Service not found: " + cls + " " + filter);
        }
    }

    protected void waitPort(int port) throws Exception {
        System.out.println("Waiting for server to appear on port: " + port);
        long startTime = System.currentTimeMillis();
        while (true) {
            Socket s = null;
            try {
                s = new Socket((String)null, port);
                // yep, its available
                return;
            } catch (IOException e) {
                sleepOrTimeout(startTime, TIMEOUT, "Timeout waiting for port " + port);
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    protected Bundle getBundleByName(BundleContext bc, String name) {
        for (Bundle bundle : bc.getBundles()) {
            if (bundle.getSymbolicName().equals(name)) {
                return bundle;
            }
        }
        return null;
    }

    protected static int getFreePort() {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true); // enables quickly reopening socket on same port
            socket.bind(new InetSocketAddress(0)); // zero finds a free port
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected void waitWebPage(String urlSt) throws InterruptedException, TimeoutException {
        System.out.println("Waiting for url " + urlSt);
        HttpURLConnection con = null;
        long startTime = System.currentTimeMillis();
        while (true) {
            try {
                URL url = new URL(urlSt);
                con = (HttpURLConnection)url.openConnection();
                int status = con.getResponseCode();
                if (status == 200) {
                    return;
                }
            } catch (ConnectException e) {
                // Ignore connection refused
            } catch (MalformedURLException e) {
                throw new RuntimeException(e.getMessage(), e);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
            sleepOrTimeout(startTime, TIMEOUT, "Timeout waiting for web page " + urlSt);
        }
    }

    protected void assertBundlesStarted() {
        for (Bundle bundle : bundleContext.getBundles()) {
            System.out.println(bundle.getSymbolicName() + ":" + bundle.getVersion() + ": " + bundle.getState());
            if (bundle.getState() != Bundle.ACTIVE) {
                try {
                    bundle.start();
                } catch (BundleException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    protected static Option configZKConsumer() {
        return newConfiguration("org.apache.aries.rsa.discovery.zookeeper").put("zookeeper.host", "127.0.0.1")
            .put("zookeeper.port", "" + ZK_PORT).asOption();
    }

    protected static Option configZKServer() {
        return newConfiguration("org.apache.aries.rsa.discovery.zookeeper.server").put("clientPort", "" + ZK_PORT)
            .asOption();
    }

}
