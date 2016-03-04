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
package org.apache.cxf.dosgi.dsw.api;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public interface DistributionProvider {

    String[] getSupportedTypes();

    /**
     * @param sref reference of the service to be exported
     * @param effectiveProperties combined properties of the service and additional properties from rsa
     * @param exportedInterface name of the interface to be exported
     * @return
     */
    Endpoint exportService(ServiceReference<?> sref, 
                           Map<String, Object> effectiveProperties,
                           String exportedInterface);

    /**
     * @param sref reference of the service offered to the requesting bundle
     * @param iClass interface of the service to proxy
     * @param endpoint description of the remote endpoint
     * @return service proxy to be given to the requesting bundle
     * @throws IntentUnsatisfiedException
     */
    Object importEndpoint(BundleContext consumerContext, 
                          Class<?> iClass, 
                          EndpointDescription endpoint)
        throws IntentUnsatisfiedException;
    
}
