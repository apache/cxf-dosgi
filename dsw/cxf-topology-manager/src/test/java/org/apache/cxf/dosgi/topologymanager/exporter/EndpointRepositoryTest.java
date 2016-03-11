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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class EndpointRepositoryTest {

    @Test
    public void testAddRemove() throws InvalidSyntaxException {
        EndpointDescription ep1 = createEndpoint("my");
        
        IMocksControl c = EasyMock.createControl();
        ServiceReference<?> sref = createService(c);
        RemoteServiceAdmin rsa = c.createMock(RemoteServiceAdmin.class);
        EndpointListener notifier = c.createMock(EndpointListener.class);
        
        notifier.endpointAdded(ep1, null);
        EasyMock.expectLastCall();
        
        c.replay();
        EndpointRepository repo = new EndpointRepository();
        repo.setNotifier(notifier);
        List<EndpointDescription> endpoints = Arrays.asList(ep1);
        repo.addEndpoints(sref, rsa, endpoints);
        c.verify();

        c.reset();
        notifier.endpointRemoved(ep1, null);
        EasyMock.expectLastCall();

        c.replay();
        repo.removeRemoteServiceAdmin(rsa);
        c.verify();
    }

    private ServiceReference<?> createService(IMocksControl c) {
        ServiceReference<?> sref = c.createMock(ServiceReference.class);
        Bundle bundle = c.createMock(Bundle.class);
        EasyMock.expect(bundle.getSymbolicName()).andReturn("myBundle");
        EasyMock.expect(sref.getBundle()).andReturn(bundle);
        return sref;
    }

    public EndpointDescription createEndpoint(String iface) {
        Map<String, Object> props = new Hashtable<String, Object>(); 
        props.put("objectClass", new String[]{iface});
        props.put(RemoteConstants.ENDPOINT_ID, iface);
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "any");
        return new EndpointDescription(props);
    }
}
