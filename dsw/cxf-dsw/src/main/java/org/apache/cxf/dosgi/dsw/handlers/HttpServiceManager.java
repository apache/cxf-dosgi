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

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServiceManager {
    private static final Logger LOG = LoggerFactory.getLogger(HttpServiceManager.class);
    private ServiceTracker tracker;
    private BundleContext bundleContext;
    private Map<Long, String> exportedAliases = Collections.synchronizedMap(new HashMap<Long, String>());
    private String httpBase;
    private String cxfServletAlias;

    public HttpServiceManager(BundleContext bundleContext, String httpBase, String cxfServletAlias) {
        this(bundleContext, httpBase, cxfServletAlias, new ServiceTracker(bundleContext, HttpService.class.getName(), null));
        this.tracker.open();
    }

    // Only for tests
    public HttpServiceManager(BundleContext bundleContext, String httpBase, String cxfServletAlias, ServiceTracker tracker) {
        this.bundleContext = bundleContext;
        this.tracker = tracker;
        this.httpBase = getWithDefault(httpBase, "http://" + LocalHostUtil.getLocalIp() + ":8181");
        this.cxfServletAlias = getWithDefault(cxfServletAlias, "/cxf");
    }
    
    private String getWithDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
    
    public Bus registerServletAndGetBus(String contextRoot, BundleContext callingContext,
            ServiceReference sref) {
        CXFNonSpringServlet cxf = new CXFNonSpringServlet();
        try {
            HttpService httpService = getHttpService();
            httpService.registerServlet(contextRoot, cxf, new Hashtable<String, String>(), 
                                       getHttpContext(callingContext, httpService));
            registerUnexportHook(sref, contextRoot);
            
            LOG.info("Successfully registered CXF DOSGi servlet at " + contextRoot);
        } catch (Exception e) {
            throw new ServiceException("CXF DOSGi: problem registering CXF HTTP Servlet", e);
        }
        return cxf.getBus();
    }

    protected HttpService getHttpService() {
        Object service = tracker.getService();
        if (service == null) {
            throw new RuntimeException("No HTTPService found");
        }
        return (HttpService) service;
    }

    public String getServletContextRoot(Map<?, ?> sd, Class<?> iClass) {
        return OsgiUtils.getFirstNonEmptyStringProperty(sd, 
                Constants.WS_HTTP_SERVICE_CONTEXT,
                Constants.WS_HTTP_SERVICE_CONTEXT_OLD,
                Constants.WSDL_HTTP_SERVICE_CONTEXT,
                Constants.RS_HTTP_SERVICE_CONTEXT);
    }

    private HttpContext getHttpContext(BundleContext bc, HttpService httpService) {
        HttpContext httpContext = httpService.createDefaultHttpContext();
        return new SecurityDelegatingHttpContext(bc, httpContext);
    }
    
    /**
     * This listens for service removal events and "un-exports" the service
     * from the HttpService.
     * 
     * @param reference The service reference to track
     * @param alias The HTTP servlet context alias
     */
    private void registerUnexportHook(ServiceReference sref, String alias) {
        final Long sid = (Long) sref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
        LOG.debug("Registering service listener for service with ID {}", sid);
     
        String previous = exportedAliases.put(sid, alias);
        if (previous != null) {
            LOG.warn("Overwriting service export for service with ID {}", sid);
        }
        
        try {
            Filter f = bundleContext.createFilter("(" + org.osgi.framework.Constants.SERVICE_ID + "=" + sid + ")");
            
            if (f != null) {
                bundleContext.addServiceListener(new UnregisterListener(), f.toString());
            } else {
                LOG.warn("Service listener could not be started. The service will not be automatically unexported.");
            }
        } catch (InvalidSyntaxException e) {
            LOG.warn("Service listener could not be started. The service will not be automatically unexported.", e);
        }
    }
    
    protected String getDefaultAddress(Class<?> type) {
        return "/" + type.getName().replace('.', '/');
    }

    protected String getAbsoluteAddress(BundleContext ctx, String contextRoot, String relativeEndpointAddress) {
        if (relativeEndpointAddress.startsWith("http")) {
            return relativeEndpointAddress;
        }
        String effContextRoot = contextRoot == null ? cxfServletAlias : contextRoot; 
        return this.httpBase + effContextRoot + relativeEndpointAddress;
    }
    
    public void close() {
        tracker.close();
    }
    
    private final class UnregisterListener implements ServiceListener {
        public void serviceChanged(ServiceEvent event) {

            if (!(event.getType() == ServiceEvent.UNREGISTERING)) {
                return;
            }
            final ServiceReference sref = event.getServiceReference();
            final Long sid = (Long) sref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
            final String alias = exportedAliases.remove(sid);
            if (alias == null) {
                LOG.error(
                        "Unable to unexport HTTP servlet for service class ''{0}'',"
                        + " service-id {1}: no servlet alias found",
                        new Object[] {sref.getProperty(org.osgi.framework.Constants.OBJECTCLASS), sid});
                return;
            }
            LOG.debug("Unexporting HTTP servlet for alias '{}'", alias);
            try {
                HttpService http = getHttpService();
                http.unregister(alias);
            } catch (Exception e) {
                LOG.warn("An exception occurred while unregistering service for HTTP servlet alias '" 
                        + alias + "'", e);
            }
        }
    }
}
