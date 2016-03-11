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
package org.apache.cxf.dosgi.topologymanager.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds all endpoints that are exported by a TopologyManager. For each ServiceReference that is exported a
 * map is maintained which contains information on the endpoints for each RemoteAdminService that created the
 * endpoints.
 */
@SuppressWarnings("rawtypes")
public class EndpointRepository {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointRepository.class);

    private final Map<ServiceReference, Map<RemoteServiceAdmin, Collection<EndpointDescription>>> exportedServices
        = new LinkedHashMap<ServiceReference, Map<RemoteServiceAdmin, Collection<EndpointDescription>>>();

    private EndpointListener notifier;
    
    public void setNotifier(EndpointListener notifier) {
        this.notifier = notifier;
    }
    
    
    /**
     * Remove all services exported by the given rsa.
     *
     * @param rsa the RemoteServiceAdmin to remove
     * @return list of removed endpoints
     */
    public synchronized List<EndpointDescription> removeRemoteServiceAdmin(RemoteServiceAdmin rsa) {
        LOG.debug("RemoteServiceAdmin removed: {}", rsa.getClass().getName());
        List<EndpointDescription> removedEndpoints = new ArrayList<EndpointDescription>();
        for (Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports : exportedServices.values()) {
            Collection<EndpointDescription> endpoints = exports.get(rsa);
            if (endpoints != null) {
                removedEndpoints.addAll(endpoints);
                exports.remove(rsa);
            }
        }
        endpointsRemoved(removedEndpoints);
        return removedEndpoints;
    }

    public synchronized void removeService(ServiceReference sref) {
        List<EndpointDescription> removedEndpoints = new ArrayList<EndpointDescription>();
        Map<RemoteServiceAdmin, Collection<EndpointDescription>> rsaToEndpoints = exportedServices.get(sref);
        if (rsaToEndpoints != null) {
            for (Collection<EndpointDescription> endpoints : rsaToEndpoints.values()) {
                removedEndpoints.addAll(endpoints);
            }
            exportedServices.remove(sref);
        }
        endpointsRemoved(removedEndpoints);
    }

    public synchronized void addService(ServiceReference sref) {
        if (!exportedServices.containsKey(sref)) {
            LOG.info("Marking service from bundle {} for export", sref.getBundle().getSymbolicName());
            exportedServices.put(sref, new LinkedHashMap<RemoteServiceAdmin, Collection<EndpointDescription>>());
        }
    }

    public synchronized void addEndpoints(ServiceReference sref, RemoteServiceAdmin rsa,
                                   List<EndpointDescription> endpoints) {
        addService(sref);
        Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports = exportedServices.get(sref);
        exports.put(rsa, endpoints);
        endpointsAdded(endpoints);
    }

    synchronized boolean isAlreadyExportedForRsa(ServiceReference sref, RemoteServiceAdmin rsa) {
        Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports = exportedServices.get(sref);
        return exports != null && exports.containsKey(rsa);
    }

    public synchronized Collection<EndpointDescription> getAllEndpoints() {
        List<EndpointDescription> allEndpoints = new ArrayList<EndpointDescription>();
        for (Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports : exportedServices.values()) {
            for (Collection<EndpointDescription> endpoints : exports.values()) {
                allEndpoints.addAll(endpoints);
            }
        }
        return allEndpoints;
    }

    public synchronized Set<ServiceReference> getServicesToBeExportedFor(RemoteServiceAdmin rsa) {
        Set<ServiceReference> servicesToBeExported = new HashSet<ServiceReference>();
        for (Map.Entry<ServiceReference, Map<RemoteServiceAdmin, Collection<EndpointDescription>>> entry
                : exportedServices.entrySet()) {
            if (!entry.getValue().containsKey(rsa)) {
                servicesToBeExported.add(entry.getKey());
            }
        }
        return servicesToBeExported;
    }

    private void endpointsAdded(List<EndpointDescription> endpoints) {
        for (EndpointDescription epd : endpoints) {
            notifier.endpointAdded(epd, null);
        }
    }
    
    private void endpointsRemoved(List<EndpointDescription> endpoints) {
        for (EndpointDescription epd : endpoints) {
            notifier.endpointRemoved(epd, null);
        }
    }
}
