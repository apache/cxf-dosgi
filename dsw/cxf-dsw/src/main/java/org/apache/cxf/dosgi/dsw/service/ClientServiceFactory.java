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

import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.qos.IntentUnsatifiedException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientServiceFactory implements ServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ClientServiceFactory.class);

    private BundleContext dswContext;
    private Class<?> iClass;
    private EndpointDescription sd;
    private ConfigurationTypeHandler handler;

    private ImportRegistrationImpl importRegistartion;

    private boolean closeable;
    private int serviceCounter;

    public ClientServiceFactory(BundleContext dswContext, Class<?> iClass, EndpointDescription sd,
                                ConfigurationTypeHandler handler, ImportRegistrationImpl ir) {
        this.dswContext = dswContext;
        this.iClass = iClass;
        this.sd = sd;
        this.handler = handler;
        importRegistartion = ir;
    }

    public Object getService(final Bundle requestingBundle, final ServiceRegistration sreg) {
        String interfaceName = sd.getInterfaces() != null && sd.getInterfaces().size() > 0 ? (String)sd
            .getInterfaces().toArray()[0] : null;

        LOG.debug("getService() from serviceFactory for {}", interfaceName);

        try {
            Object proxy = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    return handler.createProxy(sreg.getReference(), dswContext, requestingBundle
                                                       .getBundleContext(), iClass, sd);
                }
            });

            synchronized (this) {
                ++serviceCounter;
            }
            return proxy;
        } catch (IntentUnsatifiedException iue) {
            LOG.info("Did not create proxy for " + interfaceName + " because intent " + iue.getIntent()
                     + " could not be satisfied");
        } catch (Exception ex) {
            LOG.warn("Problem creating a remote proxy for " + interfaceName
                                   + " from CXF FindHook: ", ex);
        }

        return null;
    }

    public void ungetService(Bundle requestingBundle, ServiceRegistration sreg, Object serviceObject) {

        StringBuilder sb = new StringBuilder();
        sb.append("Releasing a client object");
        Object objectClass = sreg.getReference().getProperty(org.osgi.framework.Constants.OBJECTCLASS);
        if (objectClass != null) {
            sb.append(", interfaces : ");
            for (String s : (String[])objectClass) {
                sb.append(" " + s);
            }
        }
        LOG.info(sb.toString());

        synchronized (this) {
            --serviceCounter;
            LOG.debug("Services still provided by this ServiceFactory: {}", serviceCounter);

            if (serviceCounter <= 0 && closeable) {
                remove();
            }
        }
    }

    private void remove() {
        importRegistartion.closeAll();
    }

    public void setCloseable(boolean closeable) {
        synchronized (this) {
            this.closeable = closeable;
            if (serviceCounter <= 0 && closeable) {
                remove();
            }
        }
    }

    public boolean isCloseable() {
        return closeable;
    }

}
