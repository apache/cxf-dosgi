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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;


public class ExportServiceTest {

    
    /**
     * This tests if the topology manager handles a service marked to be exported correctly by exporting it to
     * an available RemoteServiceAdmin and by notifying an EndpointListener Afterwards
     * @throws Exception 
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testServiceExport() throws Exception {

        final Semaphore sema = new Semaphore(1);
        sema.acquire(); 
        
        String scope = "(objectClass=abc)";
     
        IMocksControl c = EasyMock.createNiceControl();
        
        
        BundleContext bctx = c.createMock(BundleContext.class);
        
        Bundle topMgrBundle = c.createMock(Bundle.class);
        
        RemoteServiceAdmin rsa = c.createMock(RemoteServiceAdmin.class);
        final ServiceReference rsaSref = c.createMock(ServiceReference.class);
        EndpointListener epl = c.createMock(EndpointListener.class);
        final ServiceReference eplSref = c.createMock(ServiceReference.class);
        EasyMock.expect(eplSref.getProperty(EasyMock.same(EndpointListener.ENDPOINT_LISTENER_SCOPE)))
            .andReturn(scope).anyTimes();
        EasyMock.expect(eplSref.getBundle()).andReturn(topMgrBundle).anyTimes();

        final ServiceReference sref = c.createMock(ServiceReference.class);
        EasyMock.expect(sref.getProperty(EasyMock.same(RemoteConstants.SERVICE_EXPORTED_INTERFACES)))
            .andReturn("*").anyTimes();
        Bundle srefBundle = c.createMock(Bundle.class);
        EasyMock.expect(sref.getBundle()).andReturn(srefBundle).anyTimes();
        
        
        EndpointDescription endpoint = c.createMock(EndpointDescription.class);

        Map<String, Object> props = new HashMap<String, Object>();
        String[] objs = new String[1];
        objs[0] = "abc";
        props.put("objectClass", objs);
        EasyMock.expect(endpoint.getProperties()).andReturn(props).anyTimes();

        ExportRegistration exportRegistration = c.createMock(ExportRegistration.class);
        ExportReference exportReference = c.createMock(ExportReference.class);

        EasyMock.expect(exportRegistration.getExportReference()).andReturn(exportReference).anyTimes();
        EasyMock.expect(exportReference.getExportedEndpoint()).andReturn(endpoint).anyTimes();

        List<ExportRegistration> ret = new ArrayList<ExportRegistration>();
        ret.add(exportRegistration);
        EasyMock.expect(rsa.exportService(EasyMock.same(sref),
                                          (Map<String, Object>)EasyMock.anyObject())).andReturn(ret)
            .once();

        epl.endpointAdded((EndpointDescription)EasyMock.anyObject(), (String)EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            public Object answer() throws Throwable {
                System.out.println("Call made !!!");
                sema.release();
                return null;
            }
            
        }).once();

        //BCTX
        bctx.addServiceListener((ServiceListener)EasyMock.anyObject(), (String)EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            public Object answer() throws Throwable {
                System.out.println("->   addServiceListener: "
                                   + EasyMock.getCurrentArguments()[1]);
                ServiceListener sl = (ServiceListener)EasyMock.getCurrentArguments()[0];

                if ("(objectClass=org.osgi.service.remoteserviceadmin.RemoteServiceAdmin)"
                    .equals(EasyMock.getCurrentArguments()[1])) {
                    ServiceEvent se = new ServiceEvent(ServiceEvent.REGISTERED, rsaSref);
                    sl.serviceChanged(se);
                } else if ("(objectClass=org.osgi.service.remoteserviceadmin.EndpointListener)"
                    .equals(EasyMock.getCurrentArguments()[1])) {
                    ServiceEvent se = new ServiceEvent(ServiceEvent.REGISTERED, eplSref);
                    sl.serviceChanged(se);
                }

                return null;
            }
        }).anyTimes();

        bctx.addServiceListener((ServiceListener)EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            public Object answer() throws Throwable {
                System.out.println("->   addServiceListener ");

                ServiceListener sl = (ServiceListener)EasyMock.getCurrentArguments()[0];

                ServiceEvent se = new ServiceEvent(ServiceEvent.REGISTERED, sref);
                sl.serviceChanged(se);
                se = new ServiceEvent(ServiceEvent.REGISTERED, eplSref);
                sl.serviceChanged(se);
                se = new ServiceEvent(ServiceEvent.REGISTERED, rsaSref);
                sl.serviceChanged(se);

                return null;
            }
        }).anyTimes();

        EasyMock.expect(bctx.getService(EasyMock.same(rsaSref))).andReturn(rsa).anyTimes();
        EasyMock.expect(bctx.getService(EasyMock.same(eplSref))).andReturn(epl).atLeastOnce();

        ServiceReference[] refs = new ServiceReference[1];
        refs[0] = eplSref;
        EasyMock
            .expect(
                    bctx.getServiceReferences(EasyMock.same(EndpointListener.class.getName()),
                                              EasyMock
                                                  .same("("
                                                        + EndpointListener.ENDPOINT_LISTENER_SCOPE
                                                        + "=*)"))).andReturn(refs).anyTimes();
        
        EasyMock.expect(bctx.createFilter(EasyMock.same(scope)))
            .andReturn(FrameworkUtil.createFilter(scope)).anyTimes();

        
        c.replay();

        //        TopologyManager tm = new TopologyManager(bctx);
        //        tm.start();

        Activator a = new Activator();
        a.start(bctx);
        
        // Wait until the EndpointListener.endpointAdded call was made as the controlflow is asynchronous
        sema.tryAcquire(30, TimeUnit.SECONDS);
        
        c.verify();

    }

}
