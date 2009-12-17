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
package org.apache.cxf.dosgi.topologymanager;

import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;

import org.junit.Test;

import static org.junit.Assert.*;

public class ActivatorTest {

    @Test
    public void testTopologyManagerInit() throws Exception {

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);

        final TopologyManager tm = EasyMock.createNiceMock(TopologyManager.class);
        final TopologyManagerImport tmi = EasyMock.createNiceMock(TopologyManagerImport.class);
        final RemoteServiceAdminList rsal = EasyMock.createNiceMock(RemoteServiceAdminList.class);
        final RemoteServiceAdminListenerImpl rsaListener = EasyMock.createNiceMock(RemoteServiceAdminListenerImpl.class);

        tm.start();
        EasyMock.expectLastCall().once();
        
        tm.stop();
        EasyMock.expectLastCall().once();
        
        tmi.start();
        EasyMock.expectLastCall().once();
        
        tmi.stop();
        EasyMock.expectLastCall().once();
        
        rsaListener.start();
        EasyMock.expectLastCall().once();
        
        rsaListener.stop();
        EasyMock.expectLastCall().once();
        
        
        
        Activator a = new Activator() {
            @Override
            protected TopologyManager createTopologyManager(BundleContext bc, RemoteServiceAdminList rl) {
                return tm;
            }
            @Override
            protected TopologyManagerImport createTopologyManagerImport(BundleContext bc,
                                                                        RemoteServiceAdminList rl) {
                return tmi;
            }
            
            @Override
            protected RemoteServiceAdminList createRemoteServiceAdminList(BundleContext bc) {
                // TODO Auto-generated method stub
                return rsal;
            }
            
            @Override
            protected RemoteServiceAdminListenerImpl createRemoteServiceAdminListenerImpl(
                                                                                          BundleContext bc,
                                                                                          TopologyManager topManager,
                                                                                          TopologyManagerImport topManagerImport) {
                // TODO Auto-generated method stub
                return rsaListener;
            }
            
        };
        
        EasyMock.replay(bc);
        EasyMock.replay(tm);
        EasyMock.replay(tmi);
        
        a.start(bc);

        a.stop(bc);
        
        EasyMock.verify(tm);
        EasyMock.verify(tmi);

    }

}
