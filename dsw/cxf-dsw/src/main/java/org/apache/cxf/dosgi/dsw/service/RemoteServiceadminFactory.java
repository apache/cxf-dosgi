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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.jaxws.javaee.ResAuthType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class RemoteServiceadminFactory implements ServiceFactory {

    private Logger LOG = Logger.getLogger(RemoteServiceadminFactory.class.getName());
    private BundleContext bctx;
    private List<RemoteServiceAdminInstance> rsaServiceInstances = new ArrayList<RemoteServiceAdminInstance>();

    private RemoteServiceAdminCore rsaCore;
    
    public RemoteServiceadminFactory(BundleContext bc) {
        bctx = bc;
        rsaCore = new RemoteServiceAdminCore(bc);
    }

    public Object getService(Bundle b, ServiceRegistration sr) {
        LOG.finest("new RemoteServiceAdmin ServiceInstance created for Bundle "+b.getSymbolicName());
        RemoteServiceAdminInstance rsai = new RemoteServiceAdminInstance(bctx,rsaCore);
        rsaServiceInstances.add(rsai);
        return rsai;
    }

    public void ungetService(Bundle b, ServiceRegistration sr, Object serviceObject) {
        LOG.finest("RemoteServiceAdmin ServiceInstance removed for Bundle "+b.getSymbolicName());
        if (serviceObject instanceof RemoteServiceAdminInstance) {
            RemoteServiceAdminInstance rsai = (RemoteServiceAdminInstance)serviceObject;
            rsai.close();
            rsaServiceInstances.remove(rsai);
        }
    }

}
