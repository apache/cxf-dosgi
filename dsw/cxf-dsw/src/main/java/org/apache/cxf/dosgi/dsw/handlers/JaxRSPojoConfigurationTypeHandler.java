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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.AegisElementProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.ServiceEndpointDescription;

public class JaxRSPojoConfigurationTypeHandler extends PojoConfigurationTypeHandler {
    private static final Logger LOG = Logger.getLogger(JaxRSPojoConfigurationTypeHandler.class.getName());

    Set<ServiceReference> httpServiceReferences = new CopyOnWriteArraySet<ServiceReference>(); 

    protected JaxRSPojoConfigurationTypeHandler(BundleContext dswBC,
                                                  CxfDistributionProvider dp,
                                                  Map<String, Object> handlerProps) {
        super(dswBC, dp, handlerProps);
    }

    public Object createProxy(ServiceReference serviceReference,
            BundleContext dswContext, BundleContext callingContext,
            Class<?> iClass, ServiceEndpointDescription sd) {
      String address = getPojoAddress(sd, iClass);
      
      if (address == null) {
          LOG.warning("Remote address is unavailable");
          return null;
      }

      LOG.info("Creating a " + sd.getProvidedInterfaces().toArray()[0]
              + " client, endpoint address is " + address);

      ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
      try {
    	  JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    	  bean.setAddress(address);
    	  bean.setServiceClass(iClass);
    	  if (!"jaxb".equals(sd.getProperty(Constants.RS_DATABINDING_PROP_KEY))) {
    	      bean.setProvider(new AegisElementProvider());
    	  }
    	  Thread.currentThread().setContextClassLoader(JAXRSClientFactoryBean.class.getClassLoader());
    	  Object proxy = getProxy(bean.create(), iClass);
    	  getDistributionProvider().addRemoteService(serviceReference);
          return proxy;
      } catch (Exception e) {
          LOG.log(Level.WARNING, "proxy creation failed", e);
      } finally {
          Thread.currentThread().setContextClassLoader(oldClassLoader);
      }
      return null;
    }
    
    
    public Server createServer(ServiceReference serviceReference,
                               BundleContext dswContext, 
                               BundleContext callingContext,
                               ServiceEndpointDescription sd, 
                               Class<?> iClass, 
                               Object serviceBean) {
    	String address = getPojoAddress(sd, iClass);
        if (address == null) {
            LOG.warning("Remote address is unavailable");
            return null;
        }
        
        LOG.info("Creating a " + sd.getProvidedInterfaces().toArray()[0]
            + " endpoint from CXF PublishHook, address is " + address);
        
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setServiceClass(iClass);
        factory.setAddress(address);
        factory.setResourceProvider(iClass, new SingletonResourceProvider(serviceBean));
        if (!"jaxb".equals(sd.getProperty(Constants.RS_DATABINDING_PROP_KEY))) {
	        List<Object> providers = new ArrayList<Object>(); 
	        providers.add(new AegisElementProvider());
	        factory.setProviders(providers);
        }
        
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            String [] intents = new String[]{"HTTP"};

            Thread.currentThread().setContextClassLoader(JAXRSServerFactoryBean.class.getClassLoader());
            Server server = factory.create();
            getDistributionProvider().addExposedService(serviceReference, registerPublication(server, intents));
            addAddressProperty(sd.getProperties(), address);
            return server;
        } catch (IntentUnsatifiedException iue) {
            getDistributionProvider().intentsUnsatisfied(serviceReference);
            throw iue;
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }       
    }

    protected String getPojoAddress(ServiceEndpointDescription sd, Class<?> iClass) {
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
