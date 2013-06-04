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
package org.apache.cxf.dosgi.topologymanager.exporter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.dosgi.topologymanager.rsatracker.RemoteServiceAdminLifeCycleListener;
import org.apache.cxf.dosgi.topologymanager.rsatracker.RemoteServiceAdminTracker;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class ExportServiceTest {

    /**
     * This tests if the topology manager handles a service marked to be
     * exported correctly by exporting it to an available RemoteServiceAdmin
     * and notifying an EndpointListener afterwards.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testServiceExport() throws Exception {
        IMocksControl c = EasyMock.createControl();

        BundleContext bctx = c.createMock(BundleContext.class);
        RemoteServiceAdmin rsa = c.createMock(RemoteServiceAdmin.class);
        final EndpointListenerNotifier mockEpListenerNotifier = c.createMock(EndpointListenerNotifier.class);
        mockEpListenerNotifier.start();
        EasyMock.expectLastCall().once();

        final ServiceReference sref = createUserServiceBundle(c);

        EasyMock
            .expect(bctx.getServiceReferences(EasyMock.<String> anyObject(), EasyMock.<String> anyObject()))
            .andReturn(null).atLeastOnce();

        RemoteServiceAdminTracker rsaTracker = createSingleRsaTracker(c, rsa);

        EndpointDescription endpoint = createEndpoint(c);
        ExportRegistration exportRegistration = createExportRegistration(c, endpoint);

        // Main assertions
        simulateUserServicePublished(bctx, sref);
        EasyMock.expect(rsa.exportService(EasyMock.same(sref), (Map<String, Object>)EasyMock.anyObject()))
            .andReturn(Collections.singletonList(exportRegistration)).once();
        mockEpListenerNotifier.notifyListeners(EasyMock.eq(true), EasyMock.eq(Collections.singletonList(endpoint)));
        EasyMock.expectLastCall().once();

        c.replay();

        TopologyManagerExport topManager = new TopologyManagerExport(bctx, rsaTracker, mockEpListenerNotifier) {

            /**
             * To avoid async call
             */
            @Override
            protected void triggerExport(ServiceReference sref) {
                doExportService(sref);
            }

        };
        topManager.start();
        c.verify();

    }

    private void simulateUserServicePublished(BundleContext bctx, final ServiceReference sref) {
        bctx.addServiceListener((ServiceListener)EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                System.out.println("Simulating publishing the user service");
                ServiceListener sl = (ServiceListener)EasyMock.getCurrentArguments()[0];
                ServiceEvent se = new ServiceEvent(ServiceEvent.REGISTERED, sref);
                sl.serviceChanged(se);
                return null;
            }
        }).once();
    }

    private RemoteServiceAdminTracker createSingleRsaTracker(IMocksControl c, RemoteServiceAdmin rsa) {
        RemoteServiceAdminTracker rsaTracker = c.createMock(RemoteServiceAdminTracker.class);
        rsaTracker.addListener(EasyMock.<RemoteServiceAdminLifeCycleListener> anyObject());
        EasyMock.expectLastCall().once();
        EasyMock.expect(rsaTracker.getList()).andReturn(Collections.singletonList(rsa));
        return rsaTracker;
    }

    private ExportRegistration createExportRegistration(IMocksControl c, EndpointDescription endpoint) {
        ExportRegistration exportRegistration = c.createMock(ExportRegistration.class);
        ExportReference exportReference = c.createMock(ExportReference.class);
        EasyMock.expect(exportRegistration.getExportReference()).andReturn(exportReference).anyTimes();
        EasyMock.expect(exportReference.getExportedEndpoint()).andReturn(endpoint).anyTimes();
        return exportRegistration;
    }

    private EndpointDescription createEndpoint(IMocksControl c) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.ENDPOINT_ID, "1");
        props.put(Constants.OBJECTCLASS, new String[] {"abc"});
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "cxf");
        return new EndpointDescription(props);
    }

    private ServiceReference createUserServiceBundle(IMocksControl c) {
        final ServiceReference sref = c.createMock(ServiceReference.class);
        EasyMock.expect(sref.getProperty(EasyMock.same(RemoteConstants.SERVICE_EXPORTED_INTERFACES)))
            .andReturn("*").anyTimes();
        Bundle srefBundle = c.createMock(Bundle.class);
        EasyMock.expect(sref.getBundle()).andReturn(srefBundle).atLeastOnce();
        EasyMock.expect(srefBundle.getSymbolicName()).andReturn("serviceBundleName").atLeastOnce();
        return sref;
    }

}
