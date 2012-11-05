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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.ProxyClassLoader;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.UserResource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class JaxRSPojoConfigurationTypeHandler extends PojoConfigurationTypeHandler {
    private static final Logger LOG = LogUtils.getL7dLogger(JaxRSPojoConfigurationTypeHandler.class);

    Set<ServiceReference> httpServiceReferences = new CopyOnWriteArraySet<ServiceReference>();

    protected JaxRSPojoConfigurationTypeHandler(BundleContext dswBC,

    Map<String, Object> handlerProps) {
        super(dswBC, handlerProps);
    }

    @Override
    public Object createProxy(ServiceReference serviceReference, BundleContext dswContext,
                              BundleContext callingContext, Class<?> iClass, EndpointDescription sd)
        throws IntentUnsatifiedException {

        String address = getPojoAddress(sd, iClass);
        if (address == null) {
            LOG.warning("Remote address is unavailable");
            return null;
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            return createJaxrsProxy(address, callingContext, dswContext, iClass, null, sd);
        } catch (Throwable e) {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        
        try {
        	ProxyClassLoader cl = new ProxyClassLoader();
        	cl.addLoader(iClass.getClassLoader());
        	cl.addLoader(Client.class.getClassLoader());
            return createJaxrsProxy(address, callingContext, dswContext, iClass, cl, sd);
        } catch (Throwable e) {
            LOG.log(Level.WARNING, "proxy creation failed", e);
        }
        
        return null;

    }

    protected Object createJaxrsProxy(String address, 
    		                          BundleContext dswContext,
                                      BundleContext callingContext,
                                      Class<?> iClass,
                                      ClassLoader loader,
                                      EndpointDescription sd) {
    	JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        if (loader != null) {
        	bean.setClassLoader(loader);
        }
        
        addRsInterceptorsFeaturesProps(bean, callingContext, sd.getProperties());
        
        List<UserResource> resources = JaxRSUtils.getModel(callingContext, iClass);
        if (resources != null) {
            bean.setModelBeansWithServiceClass(resources, iClass);
        } else {
            bean.setServiceClass(iClass);
        }
        List<Object> providers = JaxRSUtils.getProviders(callingContext, dswContext, sd.getProperties());
        if (providers != null && providers.size() > 0) {
            bean.setProviders(providers);
        }
        Thread.currentThread().setContextClassLoader(JAXRSClientFactoryBean.class.getClassLoader());
        Object proxy = getProxy(bean.create(), iClass);
        return proxy;
    }
    
    @Override
    public ExportResult createServer(ServiceReference sref, BundleContext dswContext,
                             BundleContext callingContext, Map sd, Class<?> iClass, Object serviceBean)
        throws IntentUnsatifiedException {

        String address = getPojoAddress(sd, iClass);
        if (address == null) {
            throw new RuntimeException("Remote address is unavailable");
        }

        LOG.info("Creating a " + iClass.getName()
                 + " endpoint via JaxRSPojoConfigurationTypeHandler, address is " + address);

        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();

        List<UserResource> resources = JaxRSUtils.getModel(callingContext, iClass);
        if (resources != null) {
            factory.setModelBeansWithServiceClass(resources, iClass);
            factory.setServiceBeanObjects(serviceBean);
        } else {
            factory.setServiceClass(iClass);
            factory.setResourceProvider(iClass, new SingletonResourceProvider(serviceBean));
        }

        factory.setAddress(address);
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
        
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            String[] intents = new String[] {
                "HTTP"
            };

            // The properties for the EndpointDescription
            Map<String, Object> endpointProps = createEndpointProps(sd, iClass, new String[] {
                Constants.RS_CONFIG_TYPE
            }, address,intents);
            EndpointDescription endpdDesc = null;

            Thread.currentThread().setContextClassLoader(JAXRSServerFactoryBean.class.getClassLoader());
            Server server = factory.create();

            // add the information on the new Endpoint to the export registration
            return new ExportResult(endpointProps, server);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }


    }

    protected String getPojoAddress(EndpointDescription sd, Class<?> iClass) {
        String address = OsgiUtils.getProperty(sd, Constants.RS_ADDRESS_PROPERTY);

        if (address == null) {
            address = getDefaultAddress(iClass);
            if (address != null) {
                LOG.info("Using a default address : " + address);
            }
        }
        return address;
    }

}
