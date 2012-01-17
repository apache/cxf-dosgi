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

import static org.junit.Assert.*;

import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

import org.junit.Test;

public class RemoteServiceAdminListTest {

    RemoteServiceAdminList rsal = null;
    
    @Test
    public void testRemoteServiceAdminAddingAndRemoval() throws InvalidSyntaxException{
        
        IMocksControl control = EasyMock.createNiceControl();
        
        BundleContext bc = control.createMock(BundleContext.class);
        TopologyManager tm = control.createMock(TopologyManager.class);
        TopologyManagerImport tmi = control.createMock(TopologyManagerImport.class);
        
        
        RemoteServiceAdmin rsa = control.createMock(RemoteServiceAdmin.class);
        final ServiceReference rsaSref = control.createMock(ServiceReference.class);
        
        
        tm.removeRemoteServiceAdmin(EasyMock.eq(rsa));
        EasyMock.expectLastCall().once();
        
        tm.triggerExportImportForRemoteServiceAdmin(EasyMock.eq(rsa));
        EasyMock.expectLastCall().once();
        
        tmi.triggerExportImportForRemoteServiceAdmin(EasyMock.eq(rsa));
        EasyMock.expectLastCall().once();
        
        bc.addServiceListener((ServiceListener)EasyMock.anyObject(),(String)EasyMock.anyObject()); 
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            public Object answer() throws Throwable {

                System.out.println("->   addServiceListener ");

                ServiceListener sl = (ServiceListener)EasyMock.getCurrentArguments()[0];

                ServiceEvent se = new ServiceEvent(ServiceEvent.REGISTERED, rsaSref);
                sl.serviceChanged(se);

                assertEquals(1,rsal.size());
                
                se = new ServiceEvent(ServiceEvent.UNREGISTERING, rsaSref);
                sl.serviceChanged(se);
                
                assertEquals(0,rsal.size());
                
                return null;
            }
        }).anyTimes();

        EasyMock.expect(bc.getService(EasyMock.same(rsaSref))).andReturn(rsa).anyTimes();

        EasyMock.expect(bc.createFilter((String)EasyMock.anyObject())).andReturn(null).anyTimes();
        
        
        
       control.replay();
        
        
        
        
       rsal  = new RemoteServiceAdminList(bc);
        
        
        
        
        
        rsal.setTopologyManager(tm);
        rsal.setTopologyManagerImport(tmi);
        
        System.out.println("start");
        rsal.start();
        
        
        
        control.verify();
        
        
        
    }
    
}
