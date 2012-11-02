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
package org.apache.cxf.dosgi.topologymanager.rsatracker;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Provides a list of RemoteServiceAdmin services and triggers RemoteServiceAdminLifeCycleListeners
 * when RemoteServiceAdmin services are added or removed
 */
public class RemoteServiceAdminTracker extends ServiceTracker {
    private List<RemoteServiceAdminLifeCycleListener> listeners;

    public RemoteServiceAdminTracker(BundleContext bc) {
        super(bc, RemoteServiceAdmin.class.getName(), null);
        this.listeners = new ArrayList<RemoteServiceAdminLifeCycleListener>();
    }
    
    public void addListener(RemoteServiceAdminLifeCycleListener listener) {
        listeners.add(listener);
    }

    @Override
    public Object addingService(ServiceReference reference) {
        for (RemoteServiceAdminLifeCycleListener listener : listeners) {
            listener.added((RemoteServiceAdmin) context.getService(reference));
        }
        return super.addingService(reference);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        for (RemoteServiceAdminLifeCycleListener listener : listeners) {
            listener.removed((RemoteServiceAdmin) context.getService(reference));
        }
        super.removedService(reference, service);
    }

    public List<RemoteServiceAdmin> getList() {
        ArrayList<RemoteServiceAdmin> list = new ArrayList<RemoteServiceAdmin>();
        ServiceReference[] refs = getServiceReferences();
        if (refs != null) {
            for (ServiceReference ref : refs) {
                list.add((RemoteServiceAdmin) context.getService(ref));
            }
        }
        return list;
    }

}
