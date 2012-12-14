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
package org.apache.cxf.dosgi.samples.security;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Registers a REST endpoint and a servlet filter to control access to the
 * endpoint.
 */
public class Activator implements BundleActivator {
    private ServiceRegistration restRegistration;
    private ServiceRegistration filterRegistration;

    public void start(BundleContext bundleContext) throws Exception {
        // Register a rest endpoint
        Dictionary<String, Object> restProps = new Hashtable<String, Object>();
        restProps.put("service.exported.interfaces", SecureRestEndpoint.class.getName());
        restProps.put("service.exported.configs", "org.apache.cxf.rs");
        restProps.put("org.apache.cxf.rs.httpservice.context", "/secure");
        restRegistration = bundleContext.registerService(SecureRestEndpoint.class.getName(),
                                                         new SecureRestEndpoint(), restProps);

        // Register a servlet filter (this could be done in another OSGi bundle,
        // too)
        Dictionary<String, Object> filterProps = new Hashtable<String, Object>();
        filterProps.put("org.apache.cxf.httpservice.filter", Boolean.TRUE);
        // Pax-Web whiteboard (if deployed) will attempt to apply this filter to
        // servlets by name or URL, and will complain
        // if neither servletName or urlPatterns are specified. The felix http
        // service whiteboard may do something similar.
        filterProps.put("servletNames", "none");
        filterRegistration = bundleContext.registerService(Filter.class.getName(),
                                                           new SampleSecurityFilter(), filterProps);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        restRegistration.unregister();
        filterRegistration.unregister();
    }
}
