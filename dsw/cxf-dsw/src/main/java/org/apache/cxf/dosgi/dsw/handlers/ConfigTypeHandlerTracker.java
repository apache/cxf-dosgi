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
package org.apache.cxf.dosgi.dsw.handlers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigTypeHandlerTracker extends ServiceTracker<ConfigurationTypeHandler, ConfigurationTypeHandler> {

    private static final Logger LOG = LoggerFactory.getLogger(PojoConfigurationTypeHandler.class);
    private Map<String, ConfigurationTypeHandler> handlers = new ConcurrentHashMap<String, ConfigurationTypeHandler>();

    public ConfigTypeHandlerTracker(BundleContext context) {
        super(context, ConfigurationTypeHandler.class, null);
    }

    @Override
    public ConfigurationTypeHandler addingService(ServiceReference<ConfigurationTypeHandler> reference) {
        ConfigurationTypeHandler service = super.addingService(reference);
        if (service == null) {
            return null;
        }
        String[] configTypes = service.getSupportedTypes();
        for (String type : configTypes) {
            LOG.info("Registering new configuration type {} provided by {}", type, service);
            ConfigurationTypeHandler existing = handlers.put(type, service);
            if (existing != null) {
                LOG.warn("Configuration type {} already existed. Replacing...", type);
            }
        }
        return service;
    }

    @Override
    public void removedService(ServiceReference<ConfigurationTypeHandler> reference, ConfigurationTypeHandler service) {
        String[] configTypes = service.getSupportedTypes();
        for (String type : configTypes) {
            LOG.info("Removing configuration type {} provided by {}", type, service);
            handlers.remove(type);
        }
        super.removedService(reference, service);
    }

    /**
     * tries to find a ConfigurationTypeHandler that supports at least one of the given configuration types
     * @param configurationTypes
     * @return a matching ConfigTypeHandler, or <code>null</code>
     */
    public ConfigurationTypeHandler getConfigTypeHandler(List<String> configurationTypes) {
        for (String type : configurationTypes) {
            ConfigurationTypeHandler handler = handlers.get(type);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    /**
     * returns a collection of all {@link ConfigurationTypeHandler}s that
     * have been contributed as an OSGi service
     * @return all contributed types
     */
    public Collection<String> getSupportedTypes() {
        return handlers.keySet();
    }

    @Override
    public void close() {
        handlers.clear();
        super.close();
    }

}
