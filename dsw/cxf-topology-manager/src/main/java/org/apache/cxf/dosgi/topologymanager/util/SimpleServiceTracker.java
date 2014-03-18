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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A {@link ServiceTracker} extension that simplifies its usage.
 * <p>
 * It enhances {@code ServiceTracker} by adding:
 * <ul>
 * <li>Multiple event listeners for service add/remove events
 * <li>Simpler event callbacks that do not need to deal with getting/ungetting
 *     services, calling super methods or returning service objects
 * <li>Generics support, which means the callback and {@code getList()} methods
 *     are type-safe and require no casting
 * <li>A {@link #getAllServices()} method which returns all currently tracked services;
 *     Unlike {@link ServiceTracker#getServices()}, if it is called from within a service
 *     {@link SimpleServiceTrackerListener#added added} event handler, the returned list
 *     will include the newly added service (this is the source of several bugs when using
 *     the original {@code getServices()})
 * </ul>
 *
 * @param <T> the service interface type
 */
public class SimpleServiceTracker<T> extends ServiceTracker<T, T> {

    // we must use a map with references as keys, so as not to invoke equals remotely on service objects
    private final ConcurrentMap<ServiceReference<T>, T> services = new ConcurrentHashMap<ServiceReference<T>, T>();
    private final List<SimpleServiceTrackerListener<T>> listeners =
        new CopyOnWriteArrayList<SimpleServiceTrackerListener<T>>();

    /**
     * Create a {@code SimpleServiceTracker} on the specified class name.
     * <p>
     * Services registered under the specified class name will be tracked by
     * this {@code SimpleServiceTracker}.
     *
     * @param context the {@code BundleContext} against which the tracking is done
     * @param clazz the class of the services to be tracked
     */
    public SimpleServiceTracker(BundleContext context, Class<T> clazz) {
        super(context, clazz.getName(), null);
    }

    /**
     * Create a {@code SimpleServiceTracker} on the specified {@code Filter} object.
     * <p>
     * Services which match the specified {@code Filter} object will be tracked by
     * this {@code SimpleServiceTracker}.
     *
     * @param context the {@code BundleContext} against which the tracking is done
     * @param filter The {@code Filter} to select the services to be tracked
     */
    public SimpleServiceTracker(BundleContext context, Filter filter) {
        super(context, filter, null);
    }

    /**
     * Adds a listener to be notified of services added or removed.
     *
     * @param listener the listener to add
     */
    public void addListener(SimpleServiceTrackerListener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public T addingService(ServiceReference<T> reference) {
        T service = (T) super.addingService(reference);
        services.put(reference, service);
        for (SimpleServiceTrackerListener<T> listener : listeners) {
            listener.added(reference, service);
        }
        return service;
    }

    @Override
    public void modifiedService(ServiceReference<T> reference, T service) {
        for (SimpleServiceTrackerListener<T> listener : listeners) {
            listener.modified(reference, service);
        }
        super.modifiedService(reference, service);
    }

    @Override
    public void removedService(ServiceReference<T> reference, T service) {
        services.remove(reference, service);
        for (SimpleServiceTrackerListener<T> listener : listeners) {
            listener.removed(reference, service);
        }
        super.removedService(reference, service);
    }

    @Override
    public void close() {
        super.close();
        services.clear();
    }

    /**
     * Returns all currently tracked services.
     * <p>
     * Unlike {@link ServiceTracker#getServices()}, if it is called from within a service
     * {@link SimpleServiceTrackerListener#added added} event handler, the returned list
     * will include the newly added service.
     *
     * @return all currently tracked services
     */
    public List<T> getAllServices() {
        return new ArrayList<T>(services.values());
    }

    /**
     * Returns all currently tracked service references.
     * <p>
     * Unlike {@link ServiceTracker#getServiceReferences()}, if it is called from within a service
     * {@link SimpleServiceTrackerListener#added added} event handler, the returned list
     * will include the newly added service reference.
     *
     * @return all currently tracked service references
     */
    public List<ServiceReference<T>> getAllServiceReferences() {
        return new ArrayList<ServiceReference<T>>(services.keySet());
    }
}
