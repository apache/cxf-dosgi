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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.cxf.dosgi.dsw.api.DistributionProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class DistributionProviderTracker extends ServiceTracker<DistributionProvider, ServiceRegistration> {
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    public DistributionProviderTracker(BundleContext context) {
        super(context, DistributionProvider.class, null);
    }

    @Override
    public ServiceRegistration addingService(ServiceReference<DistributionProvider> reference) {
        LOG.debug("RemoteServiceAdmin Implementation is starting up");
        DistributionProvider provider = context.getService(reference);
        BundleContext apiContext = getAPIContext();
        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(context, 
                                                                    apiContext, 
                                                                    provider);
        RemoteServiceadminFactory rsaf = new RemoteServiceadminFactory(rsaCore);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("remote.intents.supported", reference.getProperty("remote.intents.supported"));
        props.put("remote.configs.supported", reference.getProperty("remote.configs.supported"));
        LOG.info("Registering RemoteServiceAdmin for provider " + provider.getClass().getName());
        return context.registerService(RemoteServiceAdmin.class.getName(), rsaf, props);
    }

    protected BundleContext getAPIContext() {
        Bundle apiBundle = FrameworkUtil.getBundle(DistributionProvider.class);
        BundleContext apiContext = apiBundle.getBundleContext();
        return apiContext;
    }
    
    @Override
    public void removedService(ServiceReference<DistributionProvider> reference,
                               ServiceRegistration reg) {
        LOG.debug("RemoteServiceAdmin Implementation is shutting down now");
        reg.unregister();
        super.removedService(reference, reg);
    }

}
