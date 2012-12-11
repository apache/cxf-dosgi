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

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An HttpContext that delegates to another HttpContext for all things other than security. This implementation handles
 * security by delegating to a {@link FilterChain} based on the set of {@link Filter}s registered with a
 * {@link #FILTER_PROP} property.
 * </p>
 * <p>
 * If the {@link BundleContext} contains a {@link #FILTER_REQUIRED_PROP} property with value "true", requests will not
 * be allowed until at least one {@link Filter} with a {@link #FILTER_PROP} property is registered.
 * </p>
 */
public class SecurityDelegatingHttpContext implements HttpContext {
    public static final String FILTER_PROP = "org.apache.cxf.httpservice.filter";
    public static final String FILTER_REQUIRED_PROP = "org.apache.cxf.httpservice.requirefilter";
    private static final Logger LOG = LoggerFactory.getLogger(SecurityDelegatingHttpContext.class);
    private static final String FILTER_FILTER = "(" + FILTER_PROP + "=*)";

    BundleContext bundleContext;
    HttpContext delegate;
    boolean requireFilter;

    public SecurityDelegatingHttpContext(BundleContext bundleContext, HttpContext delegate) {
        this.bundleContext = bundleContext;
        this.delegate = delegate;
        requireFilter = Boolean.TRUE.toString().equalsIgnoreCase(bundleContext.getProperty(FILTER_REQUIRED_PROP));
    }

    public String getMimeType(String name) {
        return delegate.getMimeType(name);
    }

    public URL getResource(String name) {
        return delegate.getResource(name);
    }

    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServiceReference[] refs;
        try {
            refs = bundleContext.getServiceReferences(Filter.class.getName(), FILTER_FILTER);
        } catch (InvalidSyntaxException e) {
            LOG.warn(e.getMessage(), e);
            return false;
        }
        if (refs == null || refs.length == 0) {
            LOG.info("No filter registered.");
            return !requireFilter;
        }
        Filter[] filters = new Filter[refs.length];
        for (int i = 0; i < refs.length; i++) {
            filters[i] = (Filter)bundleContext.getService(refs[i]);
        }
        try {
            new Chain(filters).doFilter(request, response);
            return !response.isCommitted();
        } catch (ServletException e) {
            LOG.warn(e.getMessage(), e);
            return false;
        }
    }
}

/**
 * A {@link FilterChain} composed of {@link Filter}s with the
 */
class Chain implements FilterChain {
    private static final Logger LOG = LoggerFactory.getLogger(Chain.class);

    int current = 0;
    Filter[] filters;

    Chain(Filter[] filters) {
        this.filters = filters;
    }

    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (current < filters.length && !response.isCommitted()) {
            Filter filter = filters[current++];
            LOG.info("doFilter() on {}", filter);
            filter.doFilter(request, response, this);
        }
    }
}
