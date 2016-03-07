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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.dsw.api.DistributionProvider;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.isA;


public class ClientServiceFactoryTest extends TestCase {

    @SuppressWarnings({
     "rawtypes", "unchecked"
    })
    public void testGetService() throws ClassNotFoundException {
        final Object myTestProxyObject = new Object();

        IMocksControl control = EasyMock.createControl();
        EndpointDescription endpoint = createTestEndpointDesc();
        ImportRegistrationImpl iri = new ImportRegistrationImpl(endpoint, null);

        BundleContext requestingContext = control.createMock(BundleContext.class);
        Bundle requestingBundle = control.createMock(Bundle.class);
        EasyMock.expect(requestingBundle.loadClass(String.class.getName())).andReturn((Class)String.class);
        EasyMock.expect(requestingBundle.getBundleContext()).andReturn(requestingContext);
        ServiceRegistration sreg = control.createMock(ServiceRegistration.class);


        DistributionProvider handler = mockDistributionProvider(myTestProxyObject);
        control.replay();

        ClientServiceFactory csf = new ClientServiceFactory(endpoint, handler, iri);
        assertSame(myTestProxyObject, csf.getService(requestingBundle, sreg));
    }

    /**
     * Creating dummy class as I was not able to really mock it
     * @param myTestProxyObject
     * @return
     */
    private DistributionProvider mockDistributionProvider(final Object proxy) {
        DistributionProvider handler = EasyMock.createMock(DistributionProvider.class);
        EasyMock.expect(handler.importEndpoint(anyObject(BundleContext.class), 
                                               isA(Class[].class), 
                                               anyObject(EndpointDescription.class))).andReturn(proxy);
        EasyMock.replay(handler);
        return handler;
    }

    private EndpointDescription createTestEndpointDesc() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RemoteConstants.ENDPOINT_ID, "http://google.de");
        map.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "myGreatConfiguration");
        map.put(Constants.OBJECTCLASS, new String[]{String.class.getName()});
        EndpointDescription endpoint = new EndpointDescription(map);
        return endpoint;
    }
}
