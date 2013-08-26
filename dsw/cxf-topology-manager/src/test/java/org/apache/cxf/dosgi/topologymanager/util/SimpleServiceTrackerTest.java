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
package org.apache.cxf.dosgi.topologymanager.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.easymock.Capture;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SimpleServiceTrackerTest {

    private ServiceReference<RemoteServiceAdmin> createUserServiceBundle(IMocksControl c, BundleContext context) {
        @SuppressWarnings("unchecked")
        final ServiceReference<RemoteServiceAdmin> sref = c.createMock(ServiceReference.class);
        Bundle srefBundle = c.createMock(Bundle.class);
        expect(srefBundle.getBundleContext()).andReturn(context).anyTimes();
        expect(sref.getBundle()).andReturn(srefBundle).anyTimes();
        expect(srefBundle.getSymbolicName()).andReturn("serviceBundleName").anyTimes();
        return sref;
    }

    private static <T> void assertEqualsUnordered(Collection<T> c1, Collection<T> c2) {
        assertEquals(new HashSet<T>(c1), new HashSet<T>(c2));
    }
    
    @Test
    public void testTracker() throws InvalidSyntaxException {
        IMocksControl c = org.easymock.classextension.EasyMock.createControl();
        // create context mock
        BundleContext context = c.createMock(BundleContext.class);
        // capture service listener so we can invoke it
        Capture<ServiceListener> capturedListener = new Capture<ServiceListener>();
        context.addServiceListener(EasyMock.<ServiceListener>capture(capturedListener), (String)anyObject());
        expectLastCall().once();
        context.removeServiceListener((ServiceListener)anyObject());
        expectLastCall().once();
        // support context.createFilter
        Filter filter = c.createMock(Filter.class);
        expect(context.createFilter("(objectClass=org.osgi.service.remoteserviceadmin.RemoteServiceAdmin)"))
                .andReturn(filter).atLeastOnce();
        // support context.getServiceReferences based on our list
        final List<RemoteServiceAdmin> services = new ArrayList<RemoteServiceAdmin>();
        final List<ServiceReference<RemoteServiceAdmin>> srefs = new ArrayList<ServiceReference<RemoteServiceAdmin>>();
        expect(context.getServiceReferences((String)anyObject(), eq((String)null))).andAnswer(
                new IAnswer<ServiceReference<?>[]>() {
                @Override
                public ServiceReference<?>[] answer() {
                    return srefs.toArray(new ServiceReference[srefs.size()]);
                }
            });
        // create services
        ServiceReference<RemoteServiceAdmin> sref1 = createUserServiceBundle(c, context);
        ServiceReference<RemoteServiceAdmin> sref2 = createUserServiceBundle(c, context);
        RemoteServiceAdmin service1 = c.createMock(RemoteServiceAdmin.class);
        RemoteServiceAdmin service2 = c.createMock(RemoteServiceAdmin.class);
        expect(context.getService(sref1)).andReturn(service1).atLeastOnce();
        expect(context.getService(sref2)).andReturn(service2).atLeastOnce();
        expect(context.ungetService(sref1)).andReturn(false).atLeastOnce();
        expect(context.ungetService(sref2)).andReturn(false).atLeastOnce();

        replay(context);

        // start tracking
        final SimpleServiceTracker<RemoteServiceAdmin> tracker =
                new SimpleServiceTracker<RemoteServiceAdmin>(context, RemoteServiceAdmin.class);
        tracker.open();
        ServiceListener sl = capturedListener.getValue();
        // add our listener
        SimpleServiceTrackerListener<RemoteServiceAdmin> listener =
                new SimpleServiceTrackerListener<RemoteServiceAdmin>() {
                @SuppressWarnings({
                    "unchecked", "rawtypes"
                })
                @Override
                public void added(RemoteServiceAdmin service) {
                    // prove that original ServiceTracker fails here
                    Object[] trackerServices = (Object[])
                        (tracker.getServices() != null ? tracker.getServices() : new Object[0]);
                    assertFalse(new HashSet(services)
                                .equals(new HashSet(Arrays.asList(trackerServices))));
                    // but we succeed
                    assertEqualsUnordered(services, tracker.getAllServices());
                }

                @Override
                public void removed(RemoteServiceAdmin service) {
                    assertEqualsUnordered(services, tracker.getAllServices());
                }
            };
        tracker.addListener(listener);

        // add and remove services and verify that getAllServices() is up to date
        srefs.add(sref1);
        services.add(service1);
        sl.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, sref1));
        assertEqualsUnordered(services, tracker.getAllServices());

        srefs.add(sref2);
        services.add(service2);
        sl.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, sref2));
        assertEqualsUnordered(services, tracker.getAllServices());

        srefs.remove(sref1);
        services.remove(service1);
        sl.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, sref1));
        assertEqualsUnordered(services, tracker.getAllServices());

        srefs.remove(sref2);
        services.remove(service2);
        sl.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, sref2));
        assertEqualsUnordered(services, tracker.getAllServices());

        srefs.add(sref1);
        services.add(service1);
        sl.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, sref1));
        services.remove(service1);
        tracker.close();
        assertEqualsUnordered(services, tracker.getAllServices());

        verify(context);
    }
}
