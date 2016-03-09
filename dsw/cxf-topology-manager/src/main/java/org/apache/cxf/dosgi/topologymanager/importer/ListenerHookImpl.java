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
package org.apache.cxf.dosgi.topologymanager.importer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.dosgi.topologymanager.util.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for service listeners and informs ServiceInterestListener about added and removed interest
 * in services
 */
public class ListenerHookImpl implements ListenerHook {

    private static final Logger LOG = LoggerFactory.getLogger(ListenerHookImpl.class);

    // From the old impl.
    private static final Set<String> SYSTEM_PACKAGES;
    static {
        SYSTEM_PACKAGES = new HashSet<String>();
        SYSTEM_PACKAGES.add("org.osgi.service");
        SYSTEM_PACKAGES.add("org.apache.felix");
        SYSTEM_PACKAGES.add("org.ops4j.pax.logging");
        SYSTEM_PACKAGES.add("ch.ethz.iks.slp");
        SYSTEM_PACKAGES.add("org.ungoverned.osgi.service");
        SYSTEM_PACKAGES.add("org.springframework.osgi.context.event.OsgiBundleApplicationContextListener");
        SYSTEM_PACKAGES.add("java.net.ContentHandler");
    }

    private final BundleContext bctx;
    private final ServiceInterestListener serviceInterestListener;

    public ListenerHookImpl(BundleContext bc, ServiceInterestListener serviceInterestListener) {
        this.bctx = bc;
        this.serviceInterestListener = serviceInterestListener;
    }

    public void added(Collection<ListenerInfo> listeners) {
        LOG.debug("added listeners {}", listeners);
        for (ListenerInfo listenerInfo : listeners) {
            LOG.debug("Filter {}", listenerInfo.getFilter());

            String className = Utils.getObjectClass(listenerInfo.getFilter());

            if (listenerInfo.getBundleContext().getBundle().equals(bctx.getBundle())) {
                LOG.debug("ListenerHookImpl: skipping request from myself");
                continue;
            }

            if (listenerInfo.getFilter() == null) {
                LOG.debug("skipping empty filter");
                continue;
            }

            if (isClassExcluded(className)) {
                LOG.debug("Skipping import request for excluded class [{}]", className);
                continue;
            }
            String exFilter = extendFilter(listenerInfo.getFilter(), bctx);
            serviceInterestListener.addServiceInterest(exFilter);
        }
    }

    public void removed(Collection<ListenerInfo> listeners) {
        LOG.debug("removed listeners {}", listeners);

        for (ListenerInfo listenerInfo : listeners) {
            LOG.debug("Filter {}", listenerInfo.getFilter());

            // TODO: determine if service was handled?
            String exFilter = extendFilter(listenerInfo.getFilter(), bctx);
            serviceInterestListener.removeServiceInterest(exFilter);
        }
    }

    private static boolean isClassExcluded(String className) {
        if (className == null) {
            return true;
        }

        for (String p : SYSTEM_PACKAGES) {
            if (className.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    static String extendFilter(String filter, BundleContext bctx) {
        String uuid = bctx.getProperty(Constants.FRAMEWORK_UUID);
        return "(&" + filter + "(!(" + RemoteConstants.ENDPOINT_FRAMEWORK_UUID + "=" + uuid + ")))";
    }
}
