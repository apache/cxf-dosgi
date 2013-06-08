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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteServiceadminFactory implements ServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteServiceadminFactory.class);

    private final RemoteServiceAdminCore rsaCore;

    public RemoteServiceadminFactory(RemoteServiceAdminCore rsaCore) {
        this.rsaCore = rsaCore;
    }

    public Object getService(Bundle b, ServiceRegistration sr) {
        LOG.debug("new RemoteServiceAdmin ServiceInstance created for Bundle {}", b.getSymbolicName());
        return new RemoteServiceAdminInstance(b.getBundleContext(), rsaCore);
    }

    public void ungetService(Bundle b, ServiceRegistration sr, Object serviceObject) {
        LOG.debug("RemoteServiceAdmin ServiceInstance removed for Bundle {}", b.getSymbolicName());
        if (serviceObject instanceof RemoteServiceAdminInstance) {
            ((RemoteServiceAdminInstance)serviceObject).close();
        }
    }
}
