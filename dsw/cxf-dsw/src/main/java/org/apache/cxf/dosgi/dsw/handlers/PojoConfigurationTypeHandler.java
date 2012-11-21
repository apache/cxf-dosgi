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

import java.util.Map;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentUtils;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PojoConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PojoConfigurationTypeHandler.class);

    public PojoConfigurationTypeHandler(BundleContext dswBC, IntentManager intentManager, Map<String, Object> handlerProps) {
        super(dswBC, intentManager, handlerProps);
    }

    public Object createProxy(ServiceReference serviceReference, BundleContext dswContext,
                              BundleContext callingContext, Class<?> iClass, EndpointDescription sd)
        throws IntentUnsatifiedException {
        //
        String address = getPojoAddress(sd.getProperties(), iClass);
        if (address == null) {
            LOG.warn("Remote address is unavailable");
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
            factory.setServiceClass(iClass);
            factory.setAddress(address);
            factory.getServiceFactory().setDataBinding(databinding);

            addWsInterceptorsFeaturesProps(factory.getClientFactoryBean(), callingContext, sd.getProperties());
            setClientWsdlProperties(factory.getClientFactoryBean(), dswContext, sd.getProperties(), false);
            
            intentManager.applyIntents(factory.getFeatures(), factory.getClientFactoryBean(), sd.getProperties());

            Thread.currentThread().setContextClassLoader(ClientProxyFactoryBean.class.getClassLoader());
            Object proxy = getProxy(factory.create(), iClass);
            return proxy;
        } catch (Exception e) {
            LOG.warn("proxy creation failed", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        return null;
    }

    public ExportResult createServer(ServiceReference sref, BundleContext dswContext,
                             BundleContext callingContext, Map sd, Class<?> iClass, Object serviceBean)
        throws IntentUnsatifiedException {
        String address = getPojoAddress(sd, iClass);
        if (address == null) {
            throw new RuntimeException("Remote address is unavailable");
        }

        LOG.info("Creating a " + iClass.getName() + " endpoint from CXF PublishHook, address is " + address);

        DataBinding databinding;
        String dataBindingImpl = (String)sref.getProperty(Constants.WS_DATABINDING_PROP_KEY);
        if ("jaxb".equals(dataBindingImpl)) {
            databinding = new JAXBDataBinding();
        } else {
            databinding = new AegisDatabinding();
        }
        String frontEndImpl = (String)sref.getProperty(Constants.WS_FRONTEND_PROP_KEY);
        ServerFactoryBean factory = createServerFactoryBean(frontEndImpl);

        factory.setServiceClass(iClass);
        factory.setAddress(address);
        factory.getServiceFactory().setDataBinding(databinding);
        factory.setServiceBean(serviceBean);

        addWsInterceptorsFeaturesProps(factory, callingContext, sd);
        setWsdlProperties(factory, callingContext, sd, false);
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        
        String[] intents = intentManager.applyIntents(factory.getFeatures(), factory, sd);

        // The properties for the EndpointDescription
        Map<String, Object> endpointProps = createEndpointProps(sd, iClass, new String[]{Constants.WS_CONFIG_TYPE}, address,intents);

        try {
            Thread.currentThread().setContextClassLoader(ServerFactoryBean.class.getClassLoader());
            Server server = factory.create();
            return new ExportResult(endpointProps, server);                
        } catch (Exception e) {
            return new ExportResult(endpointProps, e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    protected String getPojoAddress(Map sd, Class<?> iClass) {
        String address = OsgiUtils.getProperty(sd, RemoteConstants.ENDPOINT_ID);
        if(address == null && sd.get(RemoteConstants.ENDPOINT_ID)!=null ){
            LOG.error("Could not use address property " + RemoteConstants.ENDPOINT_ID );
            return null;
        }
        
        if (address == null) {
            address = OsgiUtils.getProperty(sd, Constants.WS_ADDRESS_PROPERTY);
        }
        if(address == null && sd.get(Constants.WS_ADDRESS_PROPERTY)!=null ){
            LOG.error("Could not use address property " + Constants.WS_ADDRESS_PROPERTY );
            return null;
        }
        
        if (address == null) {
            address = OsgiUtils.getProperty(sd, Constants.WS_ADDRESS_PROPERTY_OLD);
        }
        if(address == null && sd.get(Constants.WS_ADDRESS_PROPERTY_OLD)!=null ){
            LOG.error("Could not use address property " + Constants.WS_ADDRESS_PROPERTY_OLD);
            return null;
        }
        
        if (address == null) {
            address = OsgiUtils.getProperty(sd, Constants.RS_ADDRESS_PROPERTY);
        }
        if(address == null && sd.get(Constants.RS_ADDRESS_PROPERTY)!=null ){
            LOG.error("Could not use address property " + Constants.RS_ADDRESS_PROPERTY);
            return null;
        }
        
        
        if (address == null) {
            String port = null;
            Object p = sd.get(Constants.WS_PORT_PROPERTY);
            if (p instanceof String) {
                port = (String) p;
            }
            
            address = getDefaultAddress(iClass, port);
            if (address != null) {
                LOG.info("Using a default address : " + address);
            }
        }
        return address;
    }


}
