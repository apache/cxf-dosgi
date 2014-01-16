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
package org.apache.cxf.dosgi.topologymanager.util;

/**
 * Callback interface for notifications of services that are
 * added to or removed from tracking by a {@link SimpleServiceTracker}.
 *
 * @param <T> the service interface type
 */
public interface SimpleServiceTrackerListener<T> {

    /**
     * Called when a new service is added to the tracked services.
     *
     * @param service the newly added service
     */
    void added(T service);

    /**
     * Called when a service is removed from the tracked services.
     *
     * @param service the removed service
     */
    void removed(T service);
}
