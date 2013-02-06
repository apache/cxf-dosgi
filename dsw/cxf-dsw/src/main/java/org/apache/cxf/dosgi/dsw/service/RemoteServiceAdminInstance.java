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

import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointPermission;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class RemoteServiceAdminInstance implements RemoteServiceAdmin {
    private BundleContext bctx;
    private RemoteServiceAdminCore rsaCore;

    private boolean closed;

    public RemoteServiceAdminInstance(BundleContext bc, RemoteServiceAdminCore core) {
        bctx = bc;
        rsaCore = core;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List /* ExportRegistration */exportService(final ServiceReference ref, final Map properties)
        throws IllegalArgumentException, UnsupportedOperationException {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            EndpointPermission epp = new EndpointPermission("*", EndpointPermission.EXPORT);
            sm.checkPermission(epp);
        }

        return AccessController.doPrivileged(new PrivilegedAction<List>() {
            public List<ExportRegistration> run() {
                if (closed) {
                    return Collections.emptyList();
                }

                return rsaCore.exportService(ref, properties);
            }
        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Collection getExportedServices() {

        SecurityManager sm = System.getSecurityManager();
        EndpointPermission epp = new EndpointPermission("*", EndpointPermission.READ);
        if (sm != null) {
            sm.checkPermission(epp);
        }

        if (closed) {
            return null;
        }
        return rsaCore.getExportedServices();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Collection getImportedEndpoints() {

        SecurityManager sm = System.getSecurityManager();
        EndpointPermission epp = new EndpointPermission("*", EndpointPermission.READ);
        if (sm != null) {
            sm.checkPermission(epp);
        }

        if (closed) {
            return null;
        }
        return rsaCore.getImportedEndpoints();
    }

    public ImportRegistration importService(EndpointDescription endpoint) {

        final EndpointDescription epd = endpoint;

        SecurityManager sm = System.getSecurityManager();
        EndpointPermission epp = new EndpointPermission(epd, OsgiUtils.getUUID(bctx),
                                                        EndpointPermission.IMPORT);
        if (sm != null) {
            sm.checkPermission(epp);
        }

        return AccessController.doPrivileged(new PrivilegedAction<ImportRegistration>() {
            public ImportRegistration run() {

                if (closed) {
                    return null;
                }

                return rsaCore.importService(epd);
            }
        });
    }

    public void close() {
        closed = true;

        rsaCore.removeExportRegistrations(bctx);
    }
}
