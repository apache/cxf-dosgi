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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

public class EventProducer {

    private final static Logger LOG = LogUtils.getL7dLogger(EventProducer.class);
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
        RemoteServiceAdminEvent rsae = null;
        if (er.getException() != null) {
            rsae = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.EXPORT_ERROR, bctx.getBundle(),(ExportReference)null, er
                .getException());
        } else {
            rsae = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.EXPORT_REGISTRATION, bctx.getBundle(),
                                               er.getExportReference(), er.getException());
        }

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
                    LOG.fine("notify RemoteServiceAdminListener " + rsal
                             + " of bundle " + sref.getBundle().getSymbolicName());
                    rsal.remoteAdminEvent(rsae);
                }
            }

        } catch (InvalidSyntaxException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    protected void publishNotifcation(ImportRegistration ir) {
        RemoteServiceAdminEvent rsae = null;
        if (ir.getException() != null) {
            rsae = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.IMPORT_ERROR, bctx.getBundle(), ir.getImportReference(), ir
                .getException());
        } else {
            rsae = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.IMPORT_REGISTRATION, bctx.getBundle(),
                                               ir.getImportReference(), ir.getException());
        }

        notifyListeners(rsae);
        eaHelper.notifyEventAdmin(rsae);
    }

    public void notifyRemoval(ExportRegistrationImpl eri) {
        RemoteServiceAdminEvent rsae = null;
        rsae = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.EXPORT_UNREGISTRATION, bctx.getBundle(), eri.getExportReference(), eri.getException());
        
        notifyListeners(rsae);
        eaHelper.notifyEventAdmin(rsae);
    }
     
    
    public void notifyRemoval(ImportRegistrationImpl eri) {
        RemoteServiceAdminEvent rsae = null;
        rsae = new RemoteServiceAdminEvent(RemoteServiceAdminEvent.IMPORT_UNREGISTRATION, bctx.getBundle(), eri.getImportReference(), eri.getException());
        
        notifyListeners(rsae);
        eaHelper.notifyEventAdmin(rsae);
    }
    
}
