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

import java.util.Dictionary;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.discovery.Discovery;

public class DiscoveryServiceImplTest extends TestCase {
    public void testSettings() {
        DiscoveryServiceImpl dsi = new DiscoveryServiceImpl();
        
        String host = "somehost";
        dsi.setZooKeeperHost(host);
        assertEquals(host, dsi.getZooKeeperHost());
        
        int port = 9898;
        dsi.setZooKeeperPort(port);
        assertEquals(port, dsi.getZooKeeperPort());
        
        int timeout = 123456;
        dsi.setZooKeeperTimeout(timeout);
        assertEquals(timeout, dsi.getZooKeeperTimeout());
    }
    
    public void testRegisterService() throws Exception {
        DiscoveryServiceImpl dsi = new DiscoveryServiceImpl();
        dsi.setZooKeeperPort(23456);
        
        final ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        sreg.unregister();
        EasyMock.replay(sreg);       

        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.registerService(
            EasyMock.eq(Discovery.class.getName()), EasyMock.eq(dsi), (Dictionary<?, ?>) EasyMock.anyObject())).
            andAnswer(new IAnswer<ServiceRegistration>(){        

            @SuppressWarnings("unchecked")
            @Override
            public ServiceRegistration answer() throws Throwable {
                Dictionary<String, Object> props = 
                    (Dictionary<String, Object>) EasyMock.getCurrentArguments()[2];
                assertEquals("localhost", props.get("zookeeper.host"));
                assertEquals(23456, props.get("zookeeper.port"));
                assertEquals(10000, props.get("zookeeper.timeout"));
                assertEquals("org.apache.cxf.dosgi.discovery.zookeeper", props.get(Constants.SERVICE_PID));
                
                return sreg;
            }
        });
        EasyMock.replay(bc);
        
        dsi.setBundleContext(bc);
        dsi.afterPropertiesSet();
        EasyMock.verify(bc);
        
        dsi.destroy();
        EasyMock.verify(sreg);
    }
}
