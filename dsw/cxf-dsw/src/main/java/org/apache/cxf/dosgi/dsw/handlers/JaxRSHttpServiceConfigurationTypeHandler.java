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

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.UserResource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaxRSHttpServiceConfigurationTypeHandler extends HttpServiceConfigurationTypeHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JaxRSHttpServiceConfigurationTypeHandler.class);

    protected JaxRSHttpServiceConfigurationTypeHandler(BundleContext dswBC,
                                                       Map<String, Object> handlerProps) {
        super(dswBC, handlerProps);
    }

    @Override
    public ExportResult createServer(ServiceReference sref, BundleContext dswContext,
                             BundleContext callingContext, Map sd, Class<?> iClass, Object serviceBean) {

        String contextRoot = getServletContextRoot(sd, iClass);
        if (contextRoot == null) {
            throw new RuntimeException("Remote address is unavailable");
        }

        Bus bus = registerServletAndGetBus(contextRoot, dswContext, sref);

        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setBus(bus);

        List<UserResource> resources = JaxRSUtils.getModel(callingContext, iClass);
        if (resources != null) {
            factory.setModelBeansWithServiceClass(resources, iClass);
            factory.setServiceBeanObjects(serviceBean);
        } else {
            factory.setServiceClass(iClass);
            factory.setResourceProvider(iClass, new SingletonResourceProvider(serviceBean));
        }
        
        String relativeEndpointAddress = getRelativeEndpointAddress(sd);
        factory.setAddress(relativeEndpointAddress);
        
        List<Object> providers = JaxRSUtils.getProviders(callingContext, dswContext, sd);
        if (providers != null && providers.size() > 0) {
            factory.setProviders(providers);
        }
        addRsInterceptorsFeaturesProps(factory, callingContext, sd);
        String location = OsgiUtils.getProperty(sd, Constants.RS_WADL_LOCATION);
    	if (location != null) {
    		URL wadlURL = callingContext.getBundle().getResource(location);
                if (wadlURL != null) {
	            factory.setDocLocation(wadlURL.toString());
                }
    	}        

    	String completeEndpointAddress = 
        		constructAddress(dswContext, contextRoot, relativeEndpointAddress);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            String[] intents = new String[] {
                "HTTP"
            };
            
            // The properties for the EndpointDescription
            Map<String, Object> endpointProps = createEndpointProps(sd, iClass, new String[] {
                Constants.RS_CONFIG_TYPE
            }, completeEndpointAddress, intents);
            Thread.currentThread().setContextClassLoader(JAXRSServerFactoryBean.class.getClassLoader());
            Server server = factory.create();
            return new ExportResult(endpointProps, server);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    protected String getServletContextRoot(Map sd, Class<?> iClass) {
        String context = OsgiUtils.getProperty(sd, Constants.RS_HTTP_SERVICE_CONTEXT);

        if (context == null) {
            context = "/" + iClass.getName().replace('.', '/');
            LOG.info("Using a default address : " + context);
        }
        return context;
    }
}
