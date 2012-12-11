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
package org.apache.cxf.dosgi.discovery.local.internal;

import junit.framework.TestCase;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;

public class ActivatorTest extends TestCase {
    public void testActivator() throws Exception {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.createFilter((String) EasyMock.anyObject())).andAnswer(new IAnswer<Filter>() {            
            public Filter answer() throws Throwable {                
                return FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(bc);
        
        Activator a = new Activator();
        a.start(bc);
        assertNotNull(a.localDiscovery);
        
        a.localDiscovery = EasyMock.createMock(LocalDiscovery.class);
        a.localDiscovery.shutDown();
        EasyMock.expectLastCall();
        EasyMock.replay(a.localDiscovery);
        a.stop(bc);
        
        EasyMock.verify(a.localDiscovery);
    }
}
