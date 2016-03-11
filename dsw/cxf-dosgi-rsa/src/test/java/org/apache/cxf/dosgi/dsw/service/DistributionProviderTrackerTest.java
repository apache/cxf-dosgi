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

import java.util.Dictionary;

import org.apache.cxf.dosgi.dsw.api.DistributionProvider;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class DistributionProviderTrackerTest {

    @SuppressWarnings({
     "unchecked", "rawtypes"
    })
    @Test
    public void testAddingRemoved() throws InvalidSyntaxException {
        IMocksControl c = EasyMock.createControl();
        DistributionProvider provider = c.createMock(DistributionProvider.class);
        
        ServiceReference<DistributionProvider> providerRef = c.createMock(ServiceReference.class);
        EasyMock.expect(providerRef.getProperty(RemoteConstants.REMOTE_INTENTS_SUPPORTED)).andReturn("");
        EasyMock.expect(providerRef.getProperty(RemoteConstants.REMOTE_CONFIGS_SUPPORTED)).andReturn("");

        BundleContext context = c.createMock(BundleContext.class);
        Filter filter = FrameworkUtil.createFilter("(objectClass=org.apache.cxf.dosgi.dsw.api.DistributionProvider)");
        EasyMock.expect(context.createFilter("(objectClass=org.apache.cxf.dosgi.dsw.api.DistributionProvider)"))
            .andReturn(filter);
        EasyMock.expect(context.getService(providerRef)).andReturn(provider);
        ServiceRegistration rsaReg = c.createMock(ServiceRegistration.class);
        EasyMock.expect(context.registerService(EasyMock.isA(String.class), EasyMock.isA(ServiceFactory.class), 
                                                EasyMock.isA(Dictionary.class)))
            .andReturn(rsaReg).atLeastOnce();

        context.addServiceListener(EasyMock.isA(ServiceListener.class), EasyMock.isA(String.class));
        EasyMock.expectLastCall();
        
        final BundleContext apiContext = c.createMock(BundleContext.class);
        c.replay();
        DistributionProviderTracker tracker = new DistributionProviderTracker(context) {
            protected BundleContext getAPIContext() {
                return apiContext;
            };
        };
        tracker.addingService(providerRef);
        c.verify();
        
        c.reset();
        rsaReg.unregister();
        EasyMock.expectLastCall();
        EasyMock.expect(context.ungetService(providerRef)).andReturn(true);
        c.replay();
        tracker.removedService(providerRef, rsaReg);
        c.verify();
    }
}
