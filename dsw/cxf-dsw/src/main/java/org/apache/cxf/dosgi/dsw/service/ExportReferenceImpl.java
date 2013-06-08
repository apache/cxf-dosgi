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

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;

public class ExportReferenceImpl implements ExportReference {

    private ServiceReference serviceReference;
    private EndpointDescription endpointDescription;

    public ExportReferenceImpl(ServiceReference serviceReference, EndpointDescription endpointDescription) {
        this.serviceReference = serviceReference;
        this.endpointDescription = endpointDescription;
    }

    public ExportReferenceImpl(ExportReference exportReference) {
        this(exportReference.getExportedService(), exportReference.getExportedEndpoint());
    }

    public EndpointDescription getExportedEndpoint() {
        return endpointDescription;
    }

    public ServiceReference getExportedService() {
        return serviceReference;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (endpointDescription == null ? 0 : endpointDescription.hashCode());
        result = prime * result + (serviceReference == null ? 0 : serviceReference.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ExportReferenceImpl other = (ExportReferenceImpl) obj;
        boolean ed = endpointDescription == null ? other.endpointDescription == null
                : endpointDescription.equals(other.endpointDescription);
        boolean sr = serviceReference == null ? other.serviceReference == null
                : serviceReference.equals(other.serviceReference);
        return ed && sr;
    }

    synchronized void close() {
        this.endpointDescription = null;
        this.serviceReference = null;
    }
}
