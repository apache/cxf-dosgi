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
package org.apache.cxf.dosgi.dsw.service;


import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ImportRegistrationImplTest {

    @Test
    public void testException() {
        
        IMocksControl c = EasyMock.createNiceControl();
        Exception e = c.createMock(Exception.class);
        c.replay();
        
        ImportRegistrationImpl i = new ImportRegistrationImpl(e);
        
        assertEquals(e, i.getException());
        assertNull(i.getImportedEndpointDescription());
        assertNull(i.getImportedService());
        assertEquals(i, i.getParent());
    }
    
    @Test
    public void testDefaultCtor() {
        
        IMocksControl c = EasyMock.createNiceControl();
        EndpointDescription ed = c.createMock(EndpointDescription.class);
        RemoteServiceAdminCore rsac = c.createMock(RemoteServiceAdminCore.class);
        
        
        c.replay();
        
        ImportRegistrationImpl i = new ImportRegistrationImpl(ed, rsac);
        
        assertNull(i.getException());
        assertEquals(i, i.getParent());
        assertEquals(ed, i.getImportedEndpointDescription());
        
    }

    
    @Test
    public void testCloneAndClose() {
     
        IMocksControl c = EasyMock.createControl();
        EndpointDescription ed = c.createMock(EndpointDescription.class);
        RemoteServiceAdminCore rsac = c.createMock(RemoteServiceAdminCore.class);
        
        ServiceRegistration sr = c.createMock(ServiceRegistration.class);
        ServiceReference sref = c.createMock(ServiceReference.class);
        EasyMock.expect(sr.getReference()).andReturn(sref).anyTimes();
        
        c.replay();
        
        ImportRegistrationImpl i1 = new ImportRegistrationImpl(ed, rsac);
        
        ImportRegistrationImpl i2 = new ImportRegistrationImpl(i1);
        
        ImportRegistrationImpl i3 = new ImportRegistrationImpl(i2);
        
        
        try {
            i2.setImportedServiceRegistration(sr);    
            assertTrue("An exception should be thrown here !", false);
        } catch (IllegalStateException e) {
            // must be thrown here ...
        }
        
        i1.setImportedServiceRegistration(sr);
        
        
        
        
        assertEquals(i1, i1.getParent());
        assertEquals(i1, i2.getParent());
        assertEquals(i1, i3.getParent());
        
        assertEquals(ed, i1.getImportedEndpointDescription());
        assertEquals(ed, i2.getImportedEndpointDescription());
        assertEquals(ed, i3.getImportedEndpointDescription());
        
        c.verify();
        c.reset();
        
        rsac.removeImportRegistration(EasyMock.eq(i3));
        EasyMock.expectLastCall().once();
        
        c.replay();
        
        i3.close();
        i3.close(); // shouldn't change anything
        
        assertNull(i3.getImportedEndpointDescription());
        
        
        c.verify();
        c.reset();
        
        rsac.removeImportRegistration(EasyMock.eq(i1));
        EasyMock.expectLastCall().once();
        
        c.replay();
        
        
        i1.close();
        
        c.verify();
        c.reset();
        
        rsac.removeImportRegistration(EasyMock.eq(i2));
        EasyMock.expectLastCall().once();
        
        sr.unregister();
        EasyMock.expectLastCall().once();
        
        c.replay();
        
        i2.close();

        c.verify();
        
        
    }
    
    @Test
    public void testCloseAll() {
        IMocksControl c = EasyMock.createControl();
        EndpointDescription ed = c.createMock(EndpointDescription.class);
        RemoteServiceAdminCore rsac = c.createMock(RemoteServiceAdminCore.class);
        
        c.replay();
        
        ImportRegistrationImpl i1 = new ImportRegistrationImpl(ed, rsac);
        
        ImportRegistrationImpl i2 = new ImportRegistrationImpl(i1);
        
        ImportRegistrationImpl i3 = new ImportRegistrationImpl(i2);
        
        
        assertEquals(i1, i1.getParent());
        assertEquals(i1, i2.getParent());
        assertEquals(i1, i3.getParent());
        
        c.verify();
        c.reset();
        
        rsac.removeImportRegistration(EasyMock.eq(i2));
        EasyMock.expectLastCall().once();
        
        c.replay();
        
        i2.close();

        c.verify();
        c.reset();
        
        rsac.removeImportRegistration(EasyMock.eq(i1));
        EasyMock.expectLastCall().once();
        rsac.removeImportRegistration(EasyMock.eq(i3));
        EasyMock.expectLastCall().once();
        
        c.replay();
        i3.closeAll();
        c.verify();
    }
    
}
