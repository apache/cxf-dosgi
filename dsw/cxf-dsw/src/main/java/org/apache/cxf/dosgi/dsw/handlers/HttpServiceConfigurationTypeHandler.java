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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class HttpServiceConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {
    private static final Logger LOG = Logger.getLogger(HttpServiceConfigurationTypeHandler.class.getName());

    Set<ServiceReference> httpServiceReferences = new CopyOnWriteArraySet<ServiceReference>(); 
    
    protected HttpServiceConfigurationTypeHandler(BundleContext dswBC,
                                                  CxfDistributionProvider dp,
                                                  Map<String, Object> handlerProps) {
        super(dswBC, dp, handlerProps);

        ServiceTracker st = new ServiceTracker(dswBC, HttpService.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                httpServiceReferences.add(reference);
                return super.addingService(reference);
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                httpServiceReferences.remove(reference);
                super.removedService(reference, service);
            }                        
        };
        st.open();
    }

    public Object createProxy(ServiceReference serviceReference,
            BundleContext dswContext, BundleContext callingContext,
            Class<?> iClass, ServiceEndpointDescription sd) {
        // This handler doesn't make sense on the client side
        return null;
    }

    public Server createServer(ServiceReference serviceReference,
                               BundleContext dswContext, 
                               BundleContext callingContext,
                               ServiceEndpointDescription sd, 
                               Class<?> iClass, 
                               Object serviceBean) {
        final String contextRoot = getServletContextRoot(sd, iClass);
        if (contextRoot == null) {
            LOG.warning("Remote address is unavailable");
            return null;
        }

        CXFNonSpringServlet cxf = new CXFNonSpringServlet();
        HttpService httpService = getHttpService();
        try {
            httpService.registerServlet(contextRoot, cxf, new Hashtable<String, String>(), null);
            LOG.info("Successfully registered CXF DOSGi servlet at " + contextRoot);
        } catch (Exception e) {
            throw new ServiceException("CXF DOSGi: problem registering CXF HTTP Servlet", e);
        }
        Bus bus = cxf.getBus();
        DataBinding databinding;
        String dataBindingImpl = (String) serviceReference.getProperty(Constants.WS_DATABINDING_PROP_KEY);
        if("jaxb".equals(dataBindingImpl)) {
          databinding = new JAXBDataBinding();
        } else {
          databinding = new AegisDatabinding();
        }
        String frontEndImpl = (String) serviceReference.getProperty(Constants.WS_FRONTEND_PROP_KEY);
        ServerFactoryBean factory = createServerFactoryBean(frontEndImpl);
        String address = constructAddress(dswContext, contextRoot);
        factory.setBus(bus);
        factory.setServiceClass(iClass);
        factory.setAddress("/");
        factory.getServiceFactory().setDataBinding(databinding);
        factory.setServiceBean(serviceBean);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            String [] intents = 
                applyIntents(dswContext, callingContext, factory.getFeatures(), factory, sd);

            Thread.currentThread().setContextClassLoader(ServerFactoryBean.class.getClassLoader());
            Server server = factory.create();
            registerStopHook(bus, httpService, server, contextRoot, Constants.WS_HTTP_SERVICE_CONTEXT);
            getDistributionProvider().addExposedService(serviceReference, registerPublication(server, intents, address));
            addAddressProperty(sd.getProperties(), address);
            return server;
        } catch (IntentUnsatifiedException iue) {
            getDistributionProvider().intentsUnsatisfied(serviceReference);
            throw iue;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }       
    }
    
    protected Map<String, String> registerPublication(Server server, String[] intents, String address) {
        Map<String, String> publicationProperties = super.registerPublication(server, intents);
        publicationProperties.put(Constants.WS_ADDRESS_PROPERTY, address);
        return publicationProperties;
    }    

    protected String constructAddress(BundleContext ctx, String contextRoot) {
        String port = null;
        boolean https = false;
        if ("true".equalsIgnoreCase(ctx.getProperty("org.osgi.service.http.secure.enabled"))) {
            https = true;
            port = ctx.getProperty("org.osgi.service.http.port.secure");            
        } else {
            port = ctx.getProperty("org.osgi.service.http.port");            
        }
        if (port == null) {
            port = "8080";
        }
        
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "localhost";
        }

        return getAddress(https ? "https" : "http", hostName, port, contextRoot);
    }

    protected HttpService getHttpService() {
        for (ServiceReference sr : httpServiceReferences) {
            Object svc = bundleContext.getService(sr);
            if (svc instanceof HttpService) {
                return (HttpService) svc;
            }
        }
        throw new ServiceException("CXF DOSGi: No HTTP Service could be found to publish CXF endpoint in.");
    }

    protected String getServletContextRoot(ServiceEndpointDescription sd, Class<?> iClass) {
        String context = OsgiUtils.getProperty(sd, Constants.WS_HTTP_SERVICE_CONTEXT);
        if (context == null) {
            context = OsgiUtils.getProperty(sd, Constants.WS_HTTP_SERVICE_CONTEXT_OLD);                      
        }
        
        if (context == null) {
            context = "/" + iClass.getName().replace('.', '/');
            LOG.info("Using a default address : " + context);
        }
        return context;
    }
    
    protected void registerStopHook(Bus bus, final HttpService httpService, 
                                    Server theServer, final String contextRoot,
                                    final String propertyName) {
        if(bus != null) {
            theServer.getEndpoint().put(propertyName, contextRoot);
            ServerLifeCycleListener stopHook = new ServerLifeCycleListener() {
                public void stopServer(Server s) {
                    Object contextProperty = s.getEndpoint().get(propertyName);
                    if (contextProperty != null && contextProperty.equals(contextRoot)) {
                        httpService.unregister(contextRoot);
                    }
                }
                public void startServer(Server s) {
                }
            };
            ServerLifeCycleManager mgr = bus.getExtension(ServerLifeCycleManager.class);
            if (mgr != null) {
                mgr.registerListener(stopHook);
            }
        }
    }
}
