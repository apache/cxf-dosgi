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
package org.apache.cxf.dosgi.topologymanager;

import org.apache.cxf.dosgi.topologymanager.exporter.TopologyManagerExport;
import org.apache.cxf.dosgi.topologymanager.importer.TopologyManagerImport;
import org.apache.cxf.dosgi.topologymanager.util.SimpleServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private TopologyManagerExport topologyManagerExport;
    private TopologyManagerImport topologyManagerImport;
    private SimpleServiceTracker<RemoteServiceAdmin> rsaTracker;

    public void start(BundleContext bc) throws Exception {
        LOG.debug("TopologyManager: start()");
        rsaTracker = new SimpleServiceTracker<RemoteServiceAdmin>(bc, RemoteServiceAdmin.class);
        topologyManagerExport = new TopologyManagerExport(bc, rsaTracker);
        topologyManagerImport = new TopologyManagerImport(bc, rsaTracker);

        rsaTracker.open();
        topologyManagerExport.start();
        topologyManagerImport.start();
    }

    public void stop(BundleContext bc) throws Exception {
        LOG.debug("TopologyManager: stop()");
        topologyManagerExport.stop();
        topologyManagerImport.stop();
        rsaTracker.close();
    }
}
