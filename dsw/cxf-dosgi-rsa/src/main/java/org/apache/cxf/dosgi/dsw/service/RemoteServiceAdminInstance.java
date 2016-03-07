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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointPermission;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class RemoteServiceAdminInstance implements RemoteServiceAdmin {

    private final BundleContext bctx;
    private final RemoteServiceAdminCore rsaCore;

    private boolean closed;

    public RemoteServiceAdminInstance(BundleContext bc, RemoteServiceAdminCore core) {
        bctx = bc;
        rsaCore = core;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<ExportRegistration> exportService(final ServiceReference ref, final Map properties) {
        checkPermission(new EndpointPermission("*", EndpointPermission.EXPORT));
        return AccessController.doPrivileged(new PrivilegedAction<List<ExportRegistration>>() {
            public List<ExportRegistration> run() {
                return closed ? Collections.<ExportRegistration>emptyList() : rsaCore.exportService(ref, properties);
            }
        });
    }

    @Override
    public Collection<ExportReference> getExportedServices() {
        checkPermission(new EndpointPermission("*", EndpointPermission.READ));
        return closed ? null : rsaCore.getExportedServices();
    }

    @Override
    public Collection<ImportReference> getImportedEndpoints() {
        checkPermission(new EndpointPermission("*", EndpointPermission.READ));
        return closed ? null : rsaCore.getImportedEndpoints();
    }

    @Override
    public ImportRegistration importService(final EndpointDescription endpoint) {
        String frameworkUUID = bctx.getProperty(Constants.FRAMEWORK_UUID);
        checkPermission(new EndpointPermission(endpoint, frameworkUUID, EndpointPermission.IMPORT));
        return AccessController.doPrivileged(new PrivilegedAction<ImportRegistration>() {
            public ImportRegistration run() {
                return closed ? null : rsaCore.importService(endpoint);
            }
        });
    }

    public void close(boolean closeAll) {
        closed = true;
        rsaCore.removeExportRegistrations(bctx.getBundle());
        if (closeAll) {
            rsaCore.close();
        }
    }

    private void checkPermission(EndpointPermission permission) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(permission);
        }
    }
}
