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
package org.apache.aries.rsa.provider.tcp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.api.DistributionProvider;
import org.apache.cxf.dosgi.dsw.api.Endpoint;
import org.apache.cxf.dosgi.dsw.api.IntentUnsatisfiedException;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

@SuppressWarnings("rawtypes")
public class TCPProvider implements DistributionProvider {

    private static final String TCP_CONFIG_TYPE = "aries.tcp";

    @Override
    public String[] getSupportedTypes() {
        return new String[] {TCP_CONFIG_TYPE};
    }

    @Override
    public Endpoint exportService(Object serviceO, 
                                  BundleContext serviceContext,
                                  Map<String, Object> effectiveProperties,
                                  Class[] exportedInterfaces) {
        effectiveProperties.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, getSupportedTypes());
        return new TcpEndpoint(serviceO, effectiveProperties);
    }

    @Override
    public Object importEndpoint(ClassLoader cl, 
                                 BundleContext consumerContext, 
                                 Class[] interfaces,
                                 EndpointDescription endpoint)
        throws IntentUnsatisfiedException {
        try {
            URI address = new URI(endpoint.getId());
            InvocationHandler handler = new TcpInvocationHandler(cl, address.getHost(), address.getPort());
            return Proxy.newProxyInstance(cl, interfaces, handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
