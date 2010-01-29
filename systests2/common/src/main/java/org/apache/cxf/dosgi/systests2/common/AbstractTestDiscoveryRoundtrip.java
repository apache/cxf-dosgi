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
package org.apache.cxf.dosgi.systests2.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.Hashtable;

import org.apache.cxf.dosgi.systests2.common.test2.Test2Service;
import org.apache.cxf.dosgi.systests2.common.test2.client.ClientActivator;
import org.apache.cxf.dosgi.systests2.common.test2.client.Test2ServiceTracker;
import org.apache.cxf.dosgi.systests2.common.test2.server.ServerActivator;
import org.apache.cxf.dosgi.systests2.common.test2.server.Test2ServiceImpl;
import org.junit.Assert;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractTestDiscoveryRoundtrip {
    protected abstract BundleContext getBundleContext(); 

    protected static InputStream getClientBundle() {
        return TinyBundles.newBundle()
            .add(ClientActivator.class)
            .add(Test2Service.class)
            .add(Test2ServiceTracker.class)
            .set(Constants.BUNDLE_SYMBOLICNAME, "test2ClientBundle")
            .set(Constants.BUNDLE_ACTIVATOR, ClientActivator.class.getName())
            .build(TinyBundles.withBnd());
    }

    protected static InputStream getServerBundle() {
        return TinyBundles.newBundle()
            .add(ServerActivator.class)
            .add(Test2Service.class)
            .add(Test2ServiceImpl.class)
            .set(Constants.BUNDLE_SYMBOLICNAME, "test2ServerBundle")
            .set(Constants.BUNDLE_ACTIVATOR, ServerActivator.class.getName())
            .build(TinyBundles.withBnd());
    }

    public void baseTestDiscoveryRoundtrip() throws Exception {
        for( Bundle b : getBundleContext().getBundles() )
        {
            System.out.println( "*** Bundle " + b.getBundleId() + " : " + b.getSymbolicName() + "/" + b.getState());
        }
        
        // Find a free port to run the Zookeeper Server on
        final int zkPort = new ServerSocket(0).getLocalPort();
        System.out.println("*** Port for Zookeeper Server: " + zkPort);

        ServiceTracker st = null;
        try {
            st = new ServiceTracker(getBundleContext(), ConfigurationAdmin.class.getName(), null) {
                @Override
                public Object addingService(ServiceReference reference) {
                    Object svc = super.addingService(reference);
                    if (svc instanceof ConfigurationAdmin) {
                        try {
                            ConfigurationAdmin cadmin = (ConfigurationAdmin) svc;
                            org.osgi.service.cm.Configuration zkServerCfg = 
                                cadmin.getConfiguration("org.apache.cxf.dosgi.discovery.zookeeper.server", null);
                            Hashtable<String, Object> svrProps = new Hashtable<String, Object>();
                            svrProps.put("clientPort", zkPort);
                            zkServerCfg.update(svrProps);                            
                            
                            org.osgi.service.cm.Configuration zkClientCfg = 
                                cadmin.getConfiguration("org.apache.cxf.dosgi.discovery.zookeeper", null);
                            Hashtable<String, Object> cliProps = new Hashtable<String, Object>();
                            cliProps.put("zookeeper.host", "127.0.0.1");
                            cliProps.put("zookeeper.port", "" + zkPort);
                            zkClientCfg.update(cliProps);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return svc;
                }            
            };
            st.open();
                                
            ServiceReference ref = waitService(String.class.getName(), "(testResult=test2)");
            String trace = (String) ref.getProperty("result");
            Assert.assertFalse("Stack trace must not contain client packages. " +
                "Invocation should go through the protocol stack. " +
                "It should not be directly invoked. " + trace,
                trace.contains("org.apache.cxf.dosgi.systests2.common.test2.client"));
            
            // If gone through the protocol stack the following packages should be 
            // in there:
            Assert.assertTrue(trace.contains("org.apache.cxf.transport"));
            Assert.assertTrue(trace.contains("org.apache.cxf.interceptor"));
        } finally {
            st.close();
        }        
    }
    
    private ServiceReference waitService(String cls, String filter) throws Exception {        
        ServiceReference[] refs = null;
        for (int i=0; i < 20; i++) {
            refs = getBundleContext().getServiceReferences(cls, filter);
            if (refs != null && refs.length > 0) {
                return refs[0];
            }
            System.out.println("Waiting for service: " + cls + filter);
            Thread.sleep(1000);
        }
        throw new Exception("Service not found: " + cls + filter);
    }    
}
