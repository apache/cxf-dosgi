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
package org.apache.cxf.dosgi.common.policy;

import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;

import java.util.HashMap;
import java.util.Map;

import org.apache.aries.rsa.spi.ExportPolicy;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

@Component //
(//
    immediate = true, //
    property = "name=cxf" //
)
public class Exporter implements ExportPolicy {

    @Override
    public Map<String, String> additionalParameters(ServiceReference<?> sref) {
        Map<String, String> params = new HashMap<>();
        if (sref.getProperty("org.apache.cxf.rs.address") != null) {
            setDefault(sref, params, SERVICE_EXPORTED_INTERFACES, "*");
            setDefault(sref, params, SERVICE_EXPORTED_CONFIGS, "org.apache.cxf.rs");
        }
        if (sref.getProperty("org.apache.cxf.ws.address") != null) {
            setDefault(sref, params, SERVICE_EXPORTED_INTERFACES, "*");
            setDefault(sref, params, SERVICE_EXPORTED_CONFIGS, "org.apache.cxf.ws");
        }
        return params;
    }

    private void setDefault(ServiceReference<?> sref, Map<String, String> params, String key, String value) {
        if (sref.getProperty(key) == null) {
            params.put(key, value);
        }
    }

}
