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
package org.apache.cxf.dosgi.discovery.zookeeper;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import org.junit.Test;

public class ActivatorTest {

    @Test
    public void testZookeeperDiscoveryStartstop() throws Exception {

        IMocksControl c = EasyMock.createNiceControl();
        final ZooKeeperDiscovery z = c.createMock(ZooKeeperDiscovery.class);

        z.start();

        EasyMock.expectLastCall().once();

        z.stop();

        EasyMock.expectLastCall().once();

        Activator a = new Activator() {

            @Override
            protected ZooKeeperDiscovery createZooKeeperDiscovery() {
                return z;
            }

        };

        BundleContext bc = c.createMock(BundleContext.class);
        ServiceRegistration sr = c.createMock(ServiceRegistration.class);

        EasyMock.expect(
                        bc.registerService(EasyMock.eq(ManagedService.class.getName()), EasyMock.eq(a),
                                           (Dictionary)EasyMock.anyObject())).andReturn(sr).once();
        sr.unregister();
        EasyMock.expectLastCall().once();

        c.replay();

        a.start(bc);
        
        a.updated(null);
        
        a.stop(bc);

        c.verify();

    }

    @Test
    public void testConfugrationUpdate() throws Exception {

        IMocksControl c = EasyMock.createNiceControl();
        final ZooKeeperDiscovery z = c.createMock(ZooKeeperDiscovery.class);

        Activator a = new Activator() {
            @Override
            protected ZooKeeperDiscovery createZooKeeperDiscovery() {
                return z;
            }

        };

        BundleContext bc = c.createMock(BundleContext.class);

        z.stop();
        EasyMock.expectLastCall().andStubThrow(new RuntimeException("No Update should take place here !!"));

        c.replay();

        a.start(bc);

        a.updated(null);

        c.verify();
        c.reset();

        final Dictionary d = new Properties();
        d.put("test", "value");

        z.stop();
        EasyMock.expectLastCall().once();
        z.start();
        EasyMock.expectLastCall().once();
        
        
        c.replay();
        a.updated(d);
        c.verify();
    }

}
