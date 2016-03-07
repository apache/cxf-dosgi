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

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventProducer {

    private static final Logger LOG = LoggerFactory.getLogger(EventProducer.class);
    private final BundleContext bctx;
    private final EventAdminHelper eaHelper;

    public EventProducer(BundleContext bc) {
        bctx = bc;
        eaHelper = new EventAdminHelper(bctx);
    }

    protected void publishNotification(List<ExportRegistration> erl) {
        for (ExportRegistration exportRegistration : erl) {
            publishNotification(exportRegistration);
        }
    }

    protected void publishNotification(ExportRegistration er) {
        int type = er.getException() == null
            ? RemoteServiceAdminEvent.EXPORT_REGISTRATION
            : RemoteServiceAdminEvent.EXPORT_ERROR;
        notify(type, null, er);
    }

    protected void publishNotification(ImportRegistration ir) {
        int type = ir.getException() == null
            ? RemoteServiceAdminEvent.IMPORT_REGISTRATION
            : RemoteServiceAdminEvent.IMPORT_ERROR;
        notify(type, ir, null);
    }

    public void notifyRemoval(ExportRegistration er) {
        notify(RemoteServiceAdminEvent.EXPORT_UNREGISTRATION, null, er);
    }

    public void notifyRemoval(ImportRegistration ir) {
        notify(RemoteServiceAdminEvent.IMPORT_UNREGISTRATION, ir, null);
    }

    // only one of ir or er must be set, and the other must be null
    private void notify(int type, ImportRegistration ir, ExportRegistration er) {
        try {
            RemoteServiceAdminEvent event = ir != null
                ? new RemoteServiceAdminEvent(type, bctx.getBundle(), ir.getImportReference(), ir.getException())
                : new RemoteServiceAdminEvent(type, bctx.getBundle(), er.getExportReference(), er.getException());
            notifyListeners(event);
            eaHelper.notifyEventAdmin(event);
        } catch (IllegalStateException ise) {
            LOG.debug("can't send notifications since bundle context is no longer valid");
        }
    }

    @SuppressWarnings({
     "rawtypes", "unchecked"
    })
    private void notifyListeners(RemoteServiceAdminEvent rsae) {
        try {
            ServiceReference[] listenerRefs = bctx.getServiceReferences(
                    RemoteServiceAdminListener.class.getName(), null);
            if (listenerRefs != null) {
                for (ServiceReference sref : listenerRefs) {
                    RemoteServiceAdminListener rsal = (RemoteServiceAdminListener)bctx.getService(sref);
                    if (rsal != null) {
                        try {
                            Bundle bundle = sref.getBundle();
                            if (bundle != null) {
                                LOG.debug("notify RemoteServiceAdminListener {} of bundle {}",
                                        rsal, bundle.getSymbolicName());
                                rsal.remoteAdminEvent(rsae);
                            }
                        } finally {
                            bctx.ungetService(sref);
                        }
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
