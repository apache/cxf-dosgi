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

import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;

import org.junit.Test;

public class RemoteServiceAdminListenerTest {

    @Test
    public void testIncommingEvent() {

        int type = 0;

        for (; type < 100; ++type) {
            System.out.println("Type is : " + type);
            BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
            TopologyManager tm = EasyMock.createMock(TopologyManager.class);
            TopologyManagerImport tmi = EasyMock.createMock(TopologyManagerImport.class);

            RemoteServiceAdminEvent rsae = EasyMock.createNiceMock(RemoteServiceAdminEvent.class);

            EasyMock.expect(rsae.getType()).andReturn(type).anyTimes();

            // calls to the tm must only happen in these two cases:
            if (type == RemoteServiceAdminEvent.EXPORT_UNREGISTRATION) {
                tm.removeExportReference((ExportReference)EasyMock.anyObject());
                EasyMock.expectLastCall().once();
            } else if (type == RemoteServiceAdminEvent.IMPORT_UNREGISTRATION) {
                tmi.removeImportReference((ImportReference)EasyMock.anyObject());
                EasyMock.expectLastCall().once();
            }

            EasyMock.replay(bc);
            EasyMock.replay(tm);
            EasyMock.replay(tmi);
            EasyMock.replay(rsae);

            RemoteServiceAdminListenerImpl rsai = new RemoteServiceAdminListenerImpl(bc, tm,tmi);
            rsai.remoteAdminEvent(rsae);

            EasyMock.verify(tm);

        }
    }
}
