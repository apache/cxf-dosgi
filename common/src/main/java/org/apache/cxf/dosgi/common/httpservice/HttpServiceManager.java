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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
           configurationPid = "cxf-dsw",
           service = HttpServiceManager.class
           )
public class HttpServiceManager {
    /**
     * Prefix to create an absolute URL from a relative URL.
     * See HttpServiceManager.getAbsoluteAddress
     *
     * Defaults to: http://<host name>:8181
     */
    public static final String KEY_HTTP_BASE = "httpBase";
    public static final String KEY_CXF_SERVLET_ALIAS = "cxfServletAlias";
    public static final String DEFAULT_CXF_SERVLET_ALIAS = "/cxf";
    private static final Logger LOG = LoggerFactory.getLogger(HttpServiceManager.class);
    
    private Map<Long, String> exportedAliases = Collections.synchronizedMap(new HashMap<Long, String>());
    private String httpBase;
    private String cxfServletAlias;
    private HttpService httpService;
    private BundleContext context;

    @Activate
    public void activate(ComponentContext compContext) {
        Dictionary<String, Object> config = compContext.getProperties();
        initFromConfig(config);
        this.context = compContext.getBundleContext();
    }

    void initFromConfig(Dictionary<String, Object> config) {
        if (config == null) {
            config = new Hashtable<String, Object>();
        }
        this.httpBase = getWithDefault(config.get(KEY_HTTP_BASE), "http://" + LocalHostUtil.getLocalIp() + ":8181");
        this.cxfServletAlias = getWithDefault(config.get(KEY_CXF_SERVLET_ALIAS), "/cxf");
    }

    private String getWithDefault(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    public Bus registerServlet(Bus bus, String contextRoot, BundleContext callingContext, Long sid) {
        bus.setExtension(new DestinationRegistryImpl(), DestinationRegistry.class);
        CXFNonSpringServlet cxf = new CXFNonSpringServlet();
        cxf.setBus(bus);
        try {
            HttpContext httpContext1 = httpService.createDefaultHttpContext();
            HttpContext httpContext = new SecurityDelegatingHttpContext(callingContext, httpContext1);
            httpService.registerServlet(contextRoot, cxf, new Hashtable<String, String>(),
                                       httpContext);

            registerUnexportHook(sid, contextRoot);

            LOG.info("Successfully registered CXF DOSGi servlet at " + contextRoot);
        } catch (Exception e) {
            throw new ServiceException("CXF DOSGi: problem registering CXF HTTP Servlet", e);
        }
        return bus;
    }

    /**
     * This listens for service removal events and "un-exports" the service
     * from the HttpService.
     *
     * @param sref the service reference to track
     * @param alias the HTTP servlet context alias
     */
    private void registerUnexportHook(Long sid, String alias) {
        LOG.debug("Registering service listener for service with ID {}", sid);

        String previous = exportedAliases.put(sid, alias);
        if (previous != null) {
            LOG.warn("Overwriting service export for service with ID {}", sid);
        }

        try {
            Filter f = context.createFilter("(" + org.osgi.framework.Constants.SERVICE_ID + "=" + sid + ")");
            if (f != null) {
                context.addServiceListener(new UnregisterListener(), f.toString());
            } else {
                LOG.warn("Service listener could not be started. The service will not be automatically unexported.");
            }
        } catch (InvalidSyntaxException e) {
            LOG.warn("Service listener could not be started. The service will not be automatically unexported.", e);
        }
    }

    public String getDefaultAddress(Class<?> type) {
        return "/" + type.getName().replace('.', '/');
    }

    public String getAbsoluteAddress(String contextRoot, String relativeEndpointAddress) {
        if (relativeEndpointAddress.startsWith("http")) {
            return relativeEndpointAddress;
        }
        String effContextRoot = contextRoot == null ? cxfServletAlias : contextRoot;
        return this.httpBase + effContextRoot + relativeEndpointAddress;
    }

    private final class UnregisterListener implements ServiceListener {

        public void serviceChanged(ServiceEvent event) {
            if (!(event.getType() == ServiceEvent.UNREGISTERING)) {
                return;
            }
            final ServiceReference<?> sref = event.getServiceReference();
            final Long sid = (Long) sref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
            final String alias = exportedAliases.remove(sid);
            if (alias == null) {
                LOG.error("Unable to unexport HTTP servlet for service class '{}',"
                        + " service-id {}: no servlet alias found",
                        sref.getProperty(org.osgi.framework.Constants.OBJECTCLASS), sid);
                return;
            }
            LOG.debug("Unexporting HTTP servlet for alias '{}'", alias);
            try {
                httpService.unregister(alias);
            } catch (Exception e) {
                LOG.warn("An exception occurred while unregistering service for HTTP servlet alias '{}'", alias, e);
            }
        }
    }
    
    public void setContext(BundleContext context) {
        this.context = context;
    }
    
    @Reference
    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }
}
