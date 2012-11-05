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

import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
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
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.util.tracker.ServiceTracker;

public class HttpServiceConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {
    private static final Logger LOG = LogUtils.getL7dLogger(HttpServiceConfigurationTypeHandler.class);

    Set<ServiceReference> httpServiceReferences = new CopyOnWriteArraySet<ServiceReference>();
    Map<Long, String> exportedAliases = Collections.synchronizedMap(new HashMap<Long, String>());
    
    protected HttpServiceConfigurationTypeHandler(BundleContext dswBC,

    Map<String, Object> handlerProps) {
        super(dswBC, handlerProps);

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

    public Object createProxy(ServiceReference serviceReference, BundleContext dswContext,
                              BundleContext callingContext, Class<?> iClass, EndpointDescription sd) {
        String address = getHttpServiceAddress(sd.getProperties(), iClass);
        if (address == null) {
            LOG.warning("Remote address is unavailable");
            // TODO: fire Event
            return null;
        }

        LOG.info("Creating a " + iClass.getName() + " client, endpoint address is " + address);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            DataBinding databinding;
            String dataBindingImpl = (String)serviceReference.getProperty(Constants.WS_DATABINDING_PROP_KEY);
            if ("jaxb".equals(dataBindingImpl)) {
                databinding = new JAXBDataBinding();
            } else {
                databinding = new AegisDatabinding();
            }
            String frontEndImpl = (String)serviceReference.getProperty(Constants.WS_FRONTEND_PROP_KEY);
            ClientProxyFactoryBean factory = createClientProxyFactoryBean(frontEndImpl);
            addWsInterceptorsFeaturesProps(factory.getClientFactoryBean(), callingContext, sd.getProperties());
            setClientWsdlProperties(factory.getClientFactoryBean(), dswContext, sd.getProperties(), false);
            
            factory.setServiceClass(iClass);
            factory.setAddress(address);
            factory.getServiceFactory().setDataBinding(databinding);

            applyIntents(dswContext, callingContext, factory.getFeatures(), factory.getClientFactoryBean(),
                         sd.getProperties());

            Thread.currentThread().setContextClassLoader(ClientProxyFactoryBean.class.getClassLoader());
            Object proxy = getProxy(factory.create(), iClass);
            return proxy;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "proxy creation failed", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        return null;
    }

    public ExportResult createServer(ServiceReference sref, BundleContext dswContext,
                             BundleContext callingContext, Map sd, Class<?> iClass, Object serviceBean) {
        final String contextRoot = getServletContextRoot(sd, iClass);
        if (contextRoot == null) {
            throw new RuntimeException("Remote address is unavailable");
        }

        Bus bus = registerServletAndGetBus(contextRoot, dswContext, sref);
        DataBinding databinding;
        String dataBindingImpl = (String)sref.getProperty(Constants.WS_DATABINDING_PROP_KEY);
        String dataBindingImpl2 = (String) sref.getProperty(Constants.WS_DATABINDING_PROP_KEY);
        if ("jaxb".equals(dataBindingImpl) || "jaxb".equals(dataBindingImpl2)) {
            databinding = new JAXBDataBinding();
        } else {
            databinding = new AegisDatabinding();
        }
        String frontEndImpl = (String)sref.getProperty(Constants.WS_FRONTEND_PROP_KEY);
        String frontEndImpl2 = (String) sref.getProperty(Constants.WS_FRONTEND_PROP_KEY);
        
        ServerFactoryBean factory = 
        	createServerFactoryBean(frontEndImpl != null ? frontEndImpl : frontEndImpl2);
        
        factory.setBus(bus);
        factory.setServiceClass(iClass);
        
        String relativeEndpointAddress = getRelativeEndpointAddress(sd);
        factory.setAddress(relativeEndpointAddress);
        factory.getServiceFactory().setDataBinding(databinding);
        factory.setServiceBean(serviceBean);
        
        addWsInterceptorsFeaturesProps(factory, callingContext, sd);
        
        setWsdlProperties(factory, callingContext, sd, false);
        
        String completeEndpointAddress = 
        		constructAddress(dswContext, contextRoot, relativeEndpointAddress);
        
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            String[] intents = applyIntents(dswContext, callingContext, factory.getFeatures(), factory, sd);

            // The properties for the EndpointDescription
            Map<String, Object> endpointProps = createEndpointProps(sd, iClass, new String[] {
                Constants.WS_CONFIG_TYPE
            }, completeEndpointAddress, intents);

            Thread.currentThread().setContextClassLoader(ServerFactoryBean.class.getClassLoader());
            Server server = factory.create();

            return new ExportResult(endpointProps, server);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
    
    protected Bus registerServletAndGetBus(String contextRoot, BundleContext dswContext,
    		ServiceReference sref) {
    	CXFNonSpringServlet cxf = new CXFNonSpringServlet();
        HttpService httpService = getHttpService();
        try {
            httpService.registerServlet(contextRoot, cxf, new Hashtable<String, String>(), 
                                       getHttpContext(dswContext, httpService));
            registerUnexportHook(sref, contextRoot);
            
            LOG.info("Successfully registered CXF DOSGi servlet at " + contextRoot);
        } catch (Exception e) {
            throw new ServiceException("CXF DOSGi: problem registering CXF HTTP Servlet", e);
        }
        return cxf.getBus();
    }

    protected String constructAddress(BundleContext ctx, String contextRoot, 
    		                          String relativeEndpointAddress) {
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
            hostName = AbstractConfigurationHandler.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            hostName = "localhost";
        }
        
        String address = getAddress(https ? "https" : "http", hostName, port, contextRoot);
        if (!StringUtils.isEmpty(relativeEndpointAddress) 
        	&& !relativeEndpointAddress.equals("/")) {
        	address += relativeEndpointAddress;
        }
        return address;
    }

    protected HttpService getHttpService() {
        for (ServiceReference sr : httpServiceReferences) {
            Object svc = bundleContext.getService(sr);
            if (svc instanceof HttpService) {
                return (HttpService)svc;
            }
        }
        throw new ServiceException("CXF DOSGi: No HTTP Service could be found to publish CXF endpoint in.");
    }

    protected String getServletContextRoot(Map sd, Class<?> iClass) {
    	String context = OsgiUtils.getProperty(sd, Constants.WS_HTTP_SERVICE_CONTEXT);
        if (context == null) {
            context = OsgiUtils.getProperty(sd, Constants.WS_HTTP_SERVICE_CONTEXT_OLD);
        }
        if (context == null) {
            context = OsgiUtils.getProperty(sd, Constants.WSDL_HTTP_SERVICE_CONTEXT);
        }

        if (context == null) {
            context = "/" + iClass.getName().replace('.', '/');
            LOG.info("Using a default address : " + context);
        }
        return context;
    }

    protected HttpContext getHttpContext(BundleContext bundleContext, HttpService httpService) {

        HttpContext httpContext = httpService.createDefaultHttpContext();
        return new SecurityDelegatingHttpContext(bundleContext, httpContext);
    }
    
    protected String getHttpServiceAddress(Map sd, Class<?> iClass) {
        String address = OsgiUtils.getProperty(sd, RemoteConstants.ENDPOINT_ID);
        if(address == null && sd.get(RemoteConstants.ENDPOINT_ID)!=null ){
            LOG.severe("Could not use address property " + RemoteConstants.ENDPOINT_ID );
            return null;
        }
        
        
        if (address == null) {
            address = OsgiUtils.getProperty(sd, Constants.WS_ADDRESS_PROPERTY);
        }
        if(address == null && sd.get(Constants.WS_ADDRESS_PROPERTY)!=null ){
            LOG.severe("Could not use address property " + Constants.WS_ADDRESS_PROPERTY );
            return null;
        }
        
        if (address == null) {
            address = OsgiUtils.getProperty(sd, Constants.WS_ADDRESS_PROPERTY_OLD);
        }
        if(address == null && sd.get(Constants.WS_ADDRESS_PROPERTY_OLD)!=null ){
            LOG.severe("Could not use address property " + Constants.WS_ADDRESS_PROPERTY_OLD);
            return null;
        }
        
        if (address == null) {
            address = OsgiUtils.getProperty(sd, Constants.RS_ADDRESS_PROPERTY);
        }
        if(address == null && sd.get(Constants.RS_ADDRESS_PROPERTY)!=null ){
            LOG.severe("Could not use address property " + Constants.RS_ADDRESS_PROPERTY);
            return null;
        }

        return address;
    }
    
    /**
     * This listens for service removal events and "un-exports" the service
     * from the HttpService.
     * 
     * @param reference The service reference to track
     * @param alias The HTTP servlet context alias
     */
    protected void registerUnexportHook(ServiceReference sref, String alias) {
        final Long sid = (Long) sref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
        LOG.log(Level.FINE, "Registering service listener for service with ID {0}", sid);
     
        String previous = exportedAliases.put(sid, alias);
        if(previous != null) {
            LOG.log(Level.WARNING, "Overwriting service export for service with ID {0}", sid);
        }
        
        try {
            Filter f = bundleContext.createFilter("(" + org.osgi.framework.Constants.SERVICE_ID + "=" + sid + ")");
            
            if(f != null) {
                bundleContext.addServiceListener(new ServiceListener() {
     
                    public void serviceChanged(ServiceEvent event) {

                        if (event.getType() == ServiceEvent.UNREGISTERING) {
                            final ServiceReference sref = event.getServiceReference();
                            final Long sid = (Long) sref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
                            final String alias = exportedAliases.remove(sid);

                            if(alias != null) {
                                LOG.log(Level.FINE, "Unexporting HTTP servlet for alias ''{0}''...", alias);
                                HttpService http = getHttpService();

                                if(http != null) {
                                    try {
                                        http.unregister(alias);
                                    } catch(Exception e) {
                                        LOG.log(Level.WARNING,
                                                "An exception occurred while unregistering service for HTTP servlet alias '"
                                                + alias + "'", e);
                                    }
                                } else {
                                    LOG.log(Level.WARNING,
                                            "Unable to unexport HTTP servlet for alias ''{0}'': no HTTP service available",
                                            alias);
                                }
                            } else {
                                LOG.log(Level.WARNING,
                                        "Unable to unexport HTTP servlet for service class ''{0}'', service-id {1}: no servlet alias found",
                                        new Object[] {sref.getProperty(org.osgi.framework.Constants.OBJECTCLASS), sid});
                            }
                        }
                    }
                }, f.toString());
            } else {
                LOG.warning("Service listener could not be started. The service will not be automatically unexported.");
            }
        } catch (InvalidSyntaxException e) {
            LOG.log(Level.WARNING, "Service listener could not be started. The service will not be automatically unexported.", e);
        }
    }

    protected String getRelativeEndpointAddress(Map sd) {
        String address = OsgiUtils.getProperty(sd, Constants.RS_ADDRESS_PROPERTY);
        if (address != null) {
        	if (URI.create(address).isAbsolute()) {
        		LOG.info("Ignoring an absolute endpoint address, the value of " 
        				 + Constants.RS_ADDRESS_PROPERTY + " property can only be a relative URI"
        				 + " when " + Constants.RS_HTTP_SERVICE_CONTEXT 
        				 + " property is set");
        	} else {
        		return address;
        	}
        }
        return "/";
    }
}
