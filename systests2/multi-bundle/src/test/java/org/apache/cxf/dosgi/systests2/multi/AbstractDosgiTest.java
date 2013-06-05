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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class AbstractDosgiTest {

    protected ServiceReference waitService(BundleContext bc, Class<?> cls, String filter, int timeout)
        throws Exception {
        ServiceReference[] refs;
        for (int i = 0; i < timeout; i++) {
            refs = bc.getServiceReferences(cls.getName(), filter);
            if (refs != null && refs.length > 0) {
                return refs[0];
            }
            System.out.println("Waiting for service: " + cls + filter);
            Thread.sleep(1000);
        }
        throw new Exception("Service not found: " + cls + filter);
    }

    protected int getIntSysProperty(String key, int defaultValue) {
        String valueSt = System.getProperty(key);
        int value = valueSt == null ? 0 : Integer.valueOf(valueSt);
        return (value > 0) ? value : defaultValue;
    }

    protected void waitPort(int port) throws Exception {
        for (int i = 0; i < 20; i++) {
            Socket s = null;
            try {
                s = new Socket((String)null, port);
                // yep, its available
                return;
            } catch (IOException e) {
                // wait
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            System.out.println("Waiting for server to appear on port: " + port);
            Thread.sleep(1000);
        }
        throw new TimeoutException();
    }

    protected GreeterService createGreeterServiceProxy(String serviceUri) {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setServiceClass(GreeterService.class);
        factory.setAddress(serviceUri);
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());
        return (GreeterService)factory.create();
    }

    protected Bundle getBundleByName(BundleContext bc, String sn) {
        for (Bundle bundle : bc.getBundles()) {
            if (bundle.getSymbolicName().equals(sn)) {
                return bundle;
            }
        }
        return null;
    }

    protected int getFreePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        try {
            return socket.getLocalPort();
        } finally {
            socket.close();
        }
    }
}
