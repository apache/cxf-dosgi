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
package org.apache.cxf.dosgi.dsw.api;

import java.util.Map;

import org.osgi.framework.ServiceReference;


/**
 * SPI for TopologyManagerExport. Allows to export services that are
 * not marked for export as well customize the way services are exported.
 * 
 * Use cases:
 * - Mandate SSL and basic authoriziation by adding the respective intents and configs
 * - Add logging interceptor as intent
 * - Remove exported interfaces to filter out services
 */
public interface ExportPolicy {
    /**
     * Allows to supply additional properties for a service that are then
     * given to RemoteServiceAdmin. The service will be exported
     * if the original service or the additional properties contain the
     * non empty property service.exported.interfaces. 
     * 
     * @param service to export
     * @return additional properties for exported Service (must not be null)
     */
    Map<String, ?> additionalParameters(ServiceReference<?> sref);
}
