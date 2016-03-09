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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({
    "rawtypes", "unchecked"
   })
public class EndpointListenerNotifierTest {

    @Test
    public void testNotifyListenersOfRemovalIfAppropriate() throws InvalidSyntaxException {
        IMocksControl c = EasyMock.createNiceControl();

        BundleContext bc = c.createMock(BundleContext.class);
        ServiceReference sref = c.createMock(ServiceReference.class);
        EndpointListener epl = EasyMock.createMock(EndpointListener.class);
        EndpointDescription endpoint = c.createMock(EndpointDescription.class);
        EndpointDescription endpoint2 = c.createMock(EndpointDescription.class);

        Map<String, Object> props = new HashMap<String, Object>();
        String[] oc = new String[1];
        oc[0] = "myClass";
        props.put("objectClass", oc);

        Map<String, Object> props2 = new HashMap<String, Object>();
        oc = new String[1];
        oc[0] = "notMyClass";
        props2.put("objectClass", oc);

        EasyMock.expect(bc.getService(EasyMock.eq(sref))).andReturn(epl).anyTimes();
        EasyMock.expect(bc.createFilter((String)EasyMock.anyObject())).andAnswer(new IAnswer<Filter>() {
            public Filter answer() throws Throwable {
                return FrameworkUtil.createFilter((String)EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.expect(sref.getProperty(EasyMock.eq(EndpointListener.ENDPOINT_LISTENER_SCOPE)))
            .andReturn("(objectClass=myClass)").anyTimes();

        EasyMock.expect(endpoint.getProperties()).andReturn(props).anyTimes();
        EasyMock.expect(endpoint2.getProperties()).andReturn(props2).anyTimes();

        // must only be called for the first EndpointDescription!
        epl.endpointRemoved(EasyMock.eq(endpoint), EasyMock.eq("(objectClass=myClass)"));
        EasyMock.expectLastCall().once();

        EndpointRepository exportRepository = EasyMock.createMock(EndpointRepository.class);

        c.replay();
        EasyMock.replay(epl);

        EndpointListenerNotifier tm = new EndpointListenerNotifier(bc, exportRepository);

        List<EndpointDescription> endpoints = new ArrayList<EndpointDescription>();
        endpoints.add(endpoint);
        endpoints.add(endpoint2);

        tm.notifyListener(false, sref, endpoints);

        c.verify();
        EasyMock.verify(epl);
    }

    @Test
    public void testNormalizeScopeForSingleString() {
        try {
            ServiceReference sr = EasyMock.createMock(ServiceReference.class);
            EasyMock.expect(sr.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE))
                .andReturn("Filterstring");

            Filter f = EasyMock.createNiceMock(Filter.class);

            BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
            EasyMock.expect(bc.createFilter((String)EasyMock.anyObject())).andReturn(f);

            EasyMock.replay(sr);
            EasyMock.replay(bc);

            List<Filter> res = EndpointListenerNotifier.getFiltersFromEndpointListenerScope(sr, bc);

            assertEquals(1, res.size());
            assertEquals(f, res.get(0));

            EasyMock.verify(sr);
            EasyMock.verify(bc);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNormalizeScopeForStringArray() {
        try {
            String[] filterStrings = {"f1", "f2", "f3"};

            ServiceReference sr = EasyMock.createMock(ServiceReference.class);
            EasyMock.expect(sr.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE))
                .andReturn(filterStrings);

            Filter f = EasyMock.createNiceMock(Filter.class);

            BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
            EasyMock.expect(bc.createFilter((String)EasyMock.anyObject())).andReturn(f).times(filterStrings.length);

            EasyMock.replay(sr);
            EasyMock.replay(bc);

            List<Filter> res = EndpointListenerNotifier.getFiltersFromEndpointListenerScope(sr, bc);

            assertEquals(filterStrings.length, res.size());
            assertEquals(f, res.get(0));

            EasyMock.verify(sr);
            EasyMock.verify(bc);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNormalizeScopeForCollection() {
        try {
            Collection<String> collection = new ArrayList<String>();
            collection.add("f1");
            collection.add("f2");
            collection.add("f3");

            ServiceReference sr = EasyMock.createMock(ServiceReference.class);
            EasyMock.expect(sr.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE))
                .andReturn(collection);

            Filter f = EasyMock.createNiceMock(Filter.class);

            BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
            EasyMock.expect(bc.createFilter((String)EasyMock.anyObject())).andReturn(f).times(collection.size());

            EasyMock.replay(sr);
            EasyMock.replay(bc);

            List<Filter> res = EndpointListenerNotifier.getFiltersFromEndpointListenerScope(sr, bc);

            assertEquals(collection.size(), res.size());
            assertEquals(f, res.get(0));

            EasyMock.verify(sr);
            EasyMock.verify(bc);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }
}
