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
package org.apache.cxf.dosgi.singlebundle;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.Activator;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;

public class DSWActivatorTest extends TestCase {

    public void testStartStop() throws Exception{

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        final Activator a = EasyMock.createNiceMock(Activator.class);
        
        a.setBundleContext(bc);
        EasyMock.expectLastCall().atLeastOnce();
        
        a.start();
        EasyMock.expectLastCall().once();
                
        EasyMock.replay(bc);
        EasyMock.replay(a);
        
        DSWActivator da = new DSWActivator(){
            protected org.apache.cxf.dosgi.dsw.Activator createActivator() {
                return a;
            };
        };
        
        da.start(bc);
        
        EasyMock.verify(a);
        EasyMock.reset(a);
        
        a.stop();
        EasyMock.expectLastCall().once();

        EasyMock.replay(a);
        
        da.stop(bc);
        
        EasyMock.verify(a);
        EasyMock.verify(bc);
        
    }
    
}
