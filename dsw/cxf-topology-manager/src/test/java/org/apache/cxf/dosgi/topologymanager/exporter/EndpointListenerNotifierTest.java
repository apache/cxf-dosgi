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

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;

import org.apache.cxf.dosgi.topologymanager.exporter.EndpointListenerNotifier.NotifyType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@SuppressWarnings({
    "rawtypes", "unchecked"
   })
public class EndpointListenerNotifierTest {

    @Test
    public void testNotifyListener() throws InvalidSyntaxException {
        EndpointDescription endpoint1 = createEndpoint("myClass");
        EndpointDescription endpoint2 = createEndpoint("notMyClass");

        // Expect listener to be called for endpoint1 but not for endpoint2 
        EndpointListener epl = listenerExpects(endpoint1, "(objectClass=myClass)");

        EndpointRepository exportRepository = new EndpointRepository();
        EndpointListenerNotifier tm = new EndpointListenerNotifier(exportRepository);

        EasyMock.replay(epl);
        List<EndpointDescription> endpoints = Arrays.asList(endpoint1, endpoint2);
        Set<Filter> filters = new HashSet<Filter>();
        filters.add(FrameworkUtil.createFilter("(objectClass=myClass)"));
        tm.notifyListener(NotifyType.ADDED, epl, filters, endpoints);
        tm.notifyListener(NotifyType.REMOVED, epl, filters, endpoints);
        EasyMock.verify(epl);
    }

    private EndpointListener listenerExpects(EndpointDescription endpoint, String filter) {
        EndpointListener epl = EasyMock.createStrictMock(EndpointListener.class);
        epl.endpointAdded(EasyMock.eq(endpoint), EasyMock.eq(filter));
        EasyMock.expectLastCall().once();
        epl.endpointRemoved(EasyMock.eq(endpoint), EasyMock.eq(filter));
        EasyMock.expectLastCall().once();
        return epl;
    }
    
    @Test
    public void testNotifyListeners() throws InvalidSyntaxException {
        EndpointDescription endpoint1 = createEndpoint("myClass");
        
        EndpointListener epl = EasyMock.createStrictMock(EndpointListener.class);
        epl.endpointAdded(EasyMock.eq(endpoint1), EasyMock.eq("(objectClass=myClass)"));
        EasyMock.expectLastCall().once();
        epl.endpointRemoved(EasyMock.eq(endpoint1), EasyMock.eq("(objectClass=myClass)"));
        EasyMock.expectLastCall().once();

        EndpointRepository exportRepository = new EndpointRepository();
        EndpointListenerNotifier tm = new EndpointListenerNotifier(exportRepository);

        EasyMock.replay(epl);
        Set<Filter> filters = new HashSet<Filter>();
        filters.add(FrameworkUtil.createFilter("(objectClass=myClass)"));
        tm.add(epl, filters);
        tm.notifyListeners(NotifyType.ADDED, asList(endpoint1));
        tm.notifyListeners(NotifyType.REMOVED, asList(endpoint1));
        tm.remove(epl);
        EasyMock.verify(epl);
    }
    
    public EndpointDescription createEndpoint(String iface) {
        Map<String, Object> props = new Hashtable<String, Object>(); 
        props.put("objectClass", new String[]{iface});
        props.put(RemoteConstants.ENDPOINT_ID, iface);
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "any");
        return new EndpointDescription(props);
    }

    @Test
    public void testNormalizeScopeForSingleString() {
        ServiceReference sr = createListenerServiceWithFilter("(myProp=A)");
        Set<Filter> res = EndpointListenerNotifier.getFiltersFromEndpointListenerScope(sr);
        assertEquals(1, res.size());
        Filter filter = res.iterator().next();
        filterMatches(filter);
    }

    @Test
    public void testNormalizeScopeForStringArray() {
        String[] filters = {"(myProp=A)", "(otherProp=B)"};
        ServiceReference sr = createListenerServiceWithFilter(filters); 
        Set<Filter> res = EndpointListenerNotifier.getFiltersFromEndpointListenerScope(sr);
        assertEquals(filters.length, res.size());
        Iterator<Filter> it = res.iterator();
        Filter filter1 = it.next();
        Filter filter2 = it.next();
        Dictionary<String, String> props = new Hashtable();
        props.put("myProp", "A");
        assertThat(filter1.match(props) || filter2.match(props), is(true));
    }

    @Test
    public void testNormalizeScopeForCollection() {
        Collection<String> collection = Arrays.asList("(myProp=A)", "(otherProp=B)");
        ServiceReference sr = createListenerServiceWithFilter(collection);
        Set<Filter> res = EndpointListenerNotifier.getFiltersFromEndpointListenerScope(sr);
        Iterator<Filter> it = res.iterator();
        Filter filter1 = it.next();
        Filter filter2 = it.next();
        Dictionary<String, String> props = new Hashtable();
        props.put("myProp", "A");
        Assert.assertThat(filter1.match(props) || filter2.match(props), is(true));
    }
    
    private void filterMatches(Filter filter) {
        Dictionary<String, String> props = new Hashtable();
        props.put("myProp", "A");
        Assert.assertTrue("Filter should match", filter.match(props));
    }

    private ServiceReference createListenerServiceWithFilter(Object filters) {
        ServiceReference sr = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sr.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE)).andReturn(filters);
        EasyMock.replay(sr);
        return sr;
    }
}
