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
    private BundleContext bctx;
    private EventAdminHelper eaHelper;

    public EventProducer(BundleContext bc) {
        bctx = bc;
        eaHelper = new EventAdminHelper(bctx);
    }

    protected void publishNotifcation(List<ExportRegistration> erl) {
        for (ExportRegistration exportRegistration : erl) {
            publishNotifcation(exportRegistration);
        }
    }

    protected void publishNotifcation(ExportRegistration er) {
        int type = er.getException() == null ? RemoteServiceAdminEvent.EXPORT_REGISTRATION : RemoteServiceAdminEvent.EXPORT_ERROR;
        RemoteServiceAdminEvent rsae = new RemoteServiceAdminEvent(type, bctx.getBundle(), er.getExportReference(),
                er.getException());

        notifyListeners(rsae);
        eaHelper.notifyEventAdmin(rsae);
    }

    private void notifyListeners(RemoteServiceAdminEvent rsae) {
        try {
            ServiceReference[] listenerRefs = bctx.getServiceReferences(RemoteServiceAdminListener.class
                .getName(), null);
            if (listenerRefs != null) {
                for (ServiceReference sref : listenerRefs) {
                    RemoteServiceAdminListener rsal = (RemoteServiceAdminListener)bctx.getService(sref);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("notify RemoteServiceAdminListener {} of bundle {}" + rsal,
                                  sref.getBundle().getSymbolicName());
                    }
                    rsal.remoteAdminEvent(rsae);
                }
            }

        } catch (InvalidSyntaxException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    protected void publishNotifcation(ImportRegistration ir) {
        RemoteServiceAdminEvent rsae = null;
        if (ir.getException() != null) {
            rsae = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.IMPORT_ERROR, bctx.getBundle(),
                                               ir.getImportReference(), ir.getException());
        } else {
            rsae = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.IMPORT_REGISTRATION, bctx.getBundle(),
                                               ir.getImportReference(), ir.getException());
        }

        notifyListeners(rsae);
        eaHelper.notifyEventAdmin(rsae);
    }

    public void notifyRemoval(ExportRegistration eri) {
        RemoteServiceAdminEvent rsae = null;
        rsae = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.EXPORT_UNREGISTRATION, bctx.getBundle(),
                                           eri.getExportReference(), eri.getException());

        notifyListeners(rsae);
        eaHelper.notifyEventAdmin(rsae);
    }


    public void notifyRemoval(ImportRegistration eri) {
        RemoteServiceAdminEvent rsae = null;
        rsae = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.IMPORT_UNREGISTRATION, bctx.getBundle(),
                                           eri.getImportReference(), eri.getException());

        notifyListeners(rsae);
        eaHelper.notifyEventAdmin(rsae);
    }

}
