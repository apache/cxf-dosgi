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
package org.apache.cxf.dosgi.common.httpservice;

import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({
    "unchecked", "rawtypes"
   })
public class SecurityDelegatingHttpContextTest {

    protected HttpContext defaultHttpContext;
    protected SecurityDelegatingHttpContext httpContext;
    protected CommitResponseFilter commitFilter;
    protected DoNothingFilter doNothingFilter;
    protected AccessDeniedFilter accessDeniedFilter;
    protected String mimeType;
    protected URL url; // does not need to exist

    @Before
    public void setUp() throws Exception {
        mimeType = "text/xml";
        url = new URL("file:test.xml"); // does not need to exist

        // Sample filters
        commitFilter = new CommitResponseFilter();
        doNothingFilter = new DoNothingFilter();
        accessDeniedFilter = new AccessDeniedFilter();

        // Mock up the default http context
        defaultHttpContext = EasyMock.createNiceMock(HttpContext.class);
        EasyMock.expect(defaultHttpContext.getMimeType((String)EasyMock.anyObject())).andReturn(mimeType);
        EasyMock.expect(defaultHttpContext.getResource((String)EasyMock.anyObject())).andReturn(url);
        EasyMock.replay(defaultHttpContext);
    }

    @Test
    public void testFilterRequired() throws Exception {
        // Mock up the service references
        ServiceReference[] serviceReferences = {};

        // Mock up the bundle context
        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContext.getServiceReferences(Filter.class.getName(),
                                                           "(org.apache.cxf.httpservice.filter=true)"))
            .andReturn(serviceReferences);
        EasyMock.replay(bundleContext);

        // Set up the secure http context
        httpContext = new SecurityDelegatingHttpContext(bundleContext, defaultHttpContext);
        httpContext.requireFilter = true;

        // Ensure that the httpContext doesn't allow the request to be processed, since there are no registered servlet
        // filters
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.replay(request);
        HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
        EasyMock.replay(response);
        boolean requestAllowed = httpContext.handleSecurity(request, response);
        assertFalse(requestAllowed);

        // Ensure that the httpContext returns true if there is no requirement for registered servlet filters
        httpContext.requireFilter = false;
        requestAllowed = httpContext.handleSecurity(request, response);
        assertTrue(requestAllowed);
    }

    @Test
    public void testSingleCommitFilter() throws Exception {
        // Mock up the service references
        ServiceReference filterReference = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.replay(filterReference);
        ServiceReference[] serviceReferences = {filterReference};

        // Mock up the bundle context
        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContext.getServiceReferences((String)EasyMock.anyObject(), (String)EasyMock.anyObject()))
            .andReturn(serviceReferences);
        EasyMock.expect(bundleContext.getService((ServiceReference)EasyMock.anyObject())).andReturn(commitFilter);
        EasyMock.replay(bundleContext);

        // Set up the secure http context
        httpContext = new SecurityDelegatingHttpContext(bundleContext, defaultHttpContext);

        // Ensure that the httpContext returns false, since the filter has committed the response
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.replay(request);
        HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
        EasyMock.expect(response.isCommitted()).andReturn(false); // the first call checks to see whether to invoke the
                                                                  // filter
        EasyMock.expect(response.isCommitted()).andReturn(true); // the second is called to determine the handleSecurity
                                                                 // return value
        EasyMock.expect(response.getWriter()).andReturn(new PrintWriter(System.out));
        EasyMock.replay(response);
        assertFalse(httpContext.handleSecurity(request, response));

        // Ensure that the appropriate filters were called
        assertTrue(commitFilter.called);
        assertFalse(doNothingFilter.called);
        assertFalse(accessDeniedFilter.called);
    }

    @Test
    public void testFilterChain() throws Exception {
        // Mock up the service references
        ServiceReference filterReference = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.replay(filterReference);
        ServiceReference[] serviceReferences = {filterReference, filterReference};

        // Mock up the bundle context
        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContext.getServiceReferences((String)EasyMock.anyObject(), (String)EasyMock.anyObject()))
            .andReturn(serviceReferences);
        EasyMock.expect(bundleContext.getService((ServiceReference)EasyMock.anyObject())).andReturn(doNothingFilter);
        EasyMock.expect(bundleContext.getService((ServiceReference)EasyMock.anyObject())).andReturn(commitFilter);
        EasyMock.replay(bundleContext);

        // Set up the secure http context
        httpContext = new SecurityDelegatingHttpContext(bundleContext, defaultHttpContext);

        // Ensure that the httpContext returns false, since the filter has committed the response
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.replay(request);
        HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
        EasyMock.expect(response.isCommitted()).andReturn(false); // doNothingFilter should not commit the response
        EasyMock.expect(response.getWriter()).andReturn(new PrintWriter(System.out));
        EasyMock.expect(response.isCommitted()).andReturn(false);
        EasyMock.expect(response.isCommitted()).andReturn(true); // the commit filter indicating that it committed the
                                                                 // response
        EasyMock.replay(response);
        assertFalse(httpContext.handleSecurity(request, response));

        // Ensure that the appropriate filters were called
        assertTrue(doNothingFilter.called);
        assertTrue(commitFilter.called);
        assertFalse(accessDeniedFilter.called);
    }

    @Test
    public void testAllowRequest() throws Exception {
        // Mock up the service references
        ServiceReference filterReference = EasyMock.createNiceMock(ServiceReference.class);
        EasyMock.replay(filterReference);
        ServiceReference[] serviceReferences = {filterReference};

        // Mock up the bundle context
        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bundleContext.getServiceReferences((String)EasyMock.anyObject(), (String)EasyMock.anyObject()))
            .andReturn(serviceReferences);
        EasyMock.expect(bundleContext.getService((ServiceReference)EasyMock.anyObject())).andReturn(doNothingFilter);
        EasyMock.replay(bundleContext);

        // Set up the secure http context
        httpContext = new SecurityDelegatingHttpContext(bundleContext, defaultHttpContext);

        // Ensure that the httpContext returns true, since the filter has not committed the response
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.replay(request);
        HttpServletResponse response = EasyMock.createNiceMock(HttpServletResponse.class);
        EasyMock.expect(response.isCommitted()).andReturn(false);
        EasyMock.replay(response);
        assertTrue(httpContext.handleSecurity(request, response));

        // Ensure that the appropriate filters were called
        assertTrue(doNothingFilter.called);
        assertFalse(commitFilter.called);
        assertFalse(accessDeniedFilter.called);
    }

    @Test
    public void testDelegation() {
        BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bundleContext);

        // Set up the secure http context
        httpContext = new SecurityDelegatingHttpContext(bundleContext, defaultHttpContext);

        // Ensure that it delegates non-security calls to the wrapped implementation (in this case, the mock)
        assertEquals(mimeType, httpContext.getMimeType(""));
        assertEquals(url, httpContext.getResource(""));
    }
}

class CommitResponseFilter implements Filter {

    boolean called;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws java.io.IOException {
        called = true;
        response.getWriter().write("committing the response");
    }
}

class DoNothingFilter implements Filter {

    boolean called;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws java.io.IOException, javax.servlet.ServletException {
        called = true;
        chain.doFilter(request, response);
    }
}

class AccessDeniedFilter implements Filter {

    boolean called;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws java.io.IOException {
        called = true;
        ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
