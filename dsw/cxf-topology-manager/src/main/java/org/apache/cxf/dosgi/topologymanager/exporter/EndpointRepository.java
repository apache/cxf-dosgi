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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds all endpoints that are exported by a TopologyManager.
 * For each ServiceReference that is exported a map is maintained which contains information
 * on the endpoints for each RemoteAdminService that created the endpoints 
 */
public class EndpointRepository {
	private static final Logger LOG = LoggerFactory.getLogger(EndpointRepository.class);

    private final Map<ServiceReference, 
                      Map<RemoteServiceAdmin, Collection<EndpointDescription>>> exportedServices = 
        new LinkedHashMap<ServiceReference, Map<RemoteServiceAdmin, Collection<EndpointDescription>>>();
    
    /**
     * Remove all services exported by the given rsa and notify listeners
     * @param rsa
     * @return list of removed endpoints
     */
    synchronized List<EndpointDescription> removeRemoteServiceAdmin(RemoteServiceAdmin rsa) {
    	List<EndpointDescription> removedEndpoints = new ArrayList<EndpointDescription>();
    	for (Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports : exportedServices
    			.values()) {
    		if (exports.containsKey(rsa)) {
    			removedEndpoints.addAll(exports.get(rsa));
    			exports.remove(rsa);
    		}
    	}
    	return removedEndpoints;
    }
    
    synchronized List<EndpointDescription> removeService(ServiceReference sref) {
    	List<EndpointDescription> removedEndpoints = new ArrayList<EndpointDescription>();
    	if (exportedServices.containsKey(sref)) {
    		Map<RemoteServiceAdmin, Collection<EndpointDescription>> rsas = exportedServices.get(sref);
    		for (Map.Entry<RemoteServiceAdmin, Collection<EndpointDescription>> entry : rsas.entrySet()) {
    			if (entry.getValue() != null) {
    				removedEndpoints.addAll(entry.getValue());
    			}
    		}
    		exportedServices.remove(sref);
    	}
    	return removedEndpoints;
    }
    
    synchronized void addService(ServiceReference sref) {
    	LOG.info("TopologyManager: adding service to exportedServices list to export it --- from bundle:  "
    			+ sref.getBundle().getSymbolicName());
    	exportedServices.put(sref,
    			new LinkedHashMap<RemoteServiceAdmin, Collection<EndpointDescription>>());
    }
    
    synchronized void addEndpoints(ServiceReference sref, RemoteServiceAdmin rsa, List<EndpointDescription> endpoints) {
    	Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports = exportedServices.get(sref);
    	exports.put(rsa, endpoints);
    }
    
    synchronized boolean isAlreadyExportedForRsa(ServiceReference sref, RemoteServiceAdmin rsa) {
    	Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports = exportedServices.get(sref);
    	return exports.containsKey(rsa);
    }
    
    synchronized Collection<EndpointDescription> getAllEndpoints() {
    	List<EndpointDescription> endpoints = new ArrayList<EndpointDescription>();
    	for (Map<RemoteServiceAdmin, Collection<EndpointDescription>> exports : exportedServices.values()) {
    		for (Collection<EndpointDescription> regs : exports.values()) {
    			if (regs != null) {
    				endpoints.addAll(regs);
    			}
    		}
    	}
    	return endpoints;
    }

    synchronized Set<ServiceReference> getServices() {
    	return exportedServices.keySet();
	}

}
