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
import org.osgi.service.remoteserviceadmin.ExportRegistration;

public class ExportReferenceImpl implements ExportReference {
    private final ExportRegistrationImpl exportRegistration;

    public ExportReferenceImpl(ExportRegistration exportRegistration) {
    	if (!(exportRegistration instanceof ExportRegistrationImpl)) {
    		throw new IllegalArgumentException("Can only create a reference from ExportRegistrationImpl");	
    	}
        this.exportRegistration = (ExportRegistrationImpl) exportRegistration;
    }

    public EndpointDescription getExportedEndpoint() {
        return exportRegistration.getEndpointDescription();
    }

    public ServiceReference getExportedService() {
        return exportRegistration.getExportedService();
    }

    protected EndpointDescription getExportedEndpointAlways() {
        return exportRegistration.getEndpointDescriptionAlways();
    }

    @Override
    public int hashCode() {
        return 31 * 1 + ((exportRegistration == null) ? 0 : exportRegistration.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExportReferenceImpl other = (ExportReferenceImpl) obj;
        if (exportRegistration == null) {
            if (other.exportRegistration != null)
                return false;
        } else if (!exportRegistration.equals(other.exportRegistration))
            return false;
        return true;
    }

}
