/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cxf.dosgi.discovery.zookeeper.server;

import java.io.File;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

public class ActivatorTest extends TestCase {
    public void testActivator() throws Exception {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("tickTime", "2000");
        props.put("initLimit", "10");
        props.put("syncLimit", "5");
        props.put("dataDir", new File(System.getProperty("java.io.tmpdir") + "/zkdata").getCanonicalFile().toString());
        props.put(Constants.SERVICE_PID, "org.apache.cxf.dosgi.discovery.zookeeper.server");
        
        ServiceRegistration sreg = EasyMock.createMock(ServiceRegistration.class);
        BundleContext bc = EasyMock.createStrictMock(BundleContext.class);
        EasyMock.expect(bc.getDataFile(""))
            .andReturn(new File(System.getProperty("java.io.tmpdir")));
        EasyMock.expect(bc.registerService(
            EasyMock.eq(ManagedService.class.getName()), EasyMock.anyObject(), EasyMock.eq(props)))
                .andReturn(sreg);
        EasyMock.replay(bc);
        EasyMock.replay(sreg);
                
        Activator a = new Activator();
        a.start(bc);
        assertNotNull(a.ms.serviceRegistration);        
        
        a.ms = EasyMock.createMock(org.apache.cxf.dosgi.discovery.zookeeper.server.ManagedService.class);
        a.ms.shutdown();
        EasyMock.expectLastCall();
        EasyMock.replay(a.ms);
        
        EasyMock.reset(bc);
        EasyMock.reset(sreg);
        sreg.unregister();
        EasyMock.expectLastCall();
        EasyMock.replay(bc);
        EasyMock.replay(sreg);

        a.stop(bc);
        EasyMock.verify(bc);
        EasyMock.verify(sreg);
        EasyMock.verify(a.ms);
    }
}
