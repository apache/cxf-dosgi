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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.service.ExportRegistrationImpl;
import org.apache.cxf.dosgi.dsw.service.Utils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class PojoConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {
    private static final Logger LOG = Logger.getLogger(PojoConfigurationTypeHandler.class.getName());

    public PojoConfigurationTypeHandler(BundleContext dswBC, Map<String, Object> handlerProps) {
        super(dswBC, handlerProps);
    }

    public Object createProxy(ServiceReference serviceReference, BundleContext dswContext,
                              BundleContext callingContext, Class<?> iClass, EndpointDescription sd)
        throws IntentUnsatifiedException {
        //
        String address = getPojoAddress(sd.getProperties(), iClass);
        if (address == null) {
            LOG.warning("Remote address is unavailable");
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

    public void createServer(ExportRegistrationImpl exportRegistration, BundleContext dswContext,
                             BundleContext callingContext, Map sd, Class<?> iClass, Object serviceBean)
        throws IntentUnsatifiedException {
        String address = getPojoAddress(sd, iClass);
        if (address == null) {
            LOG.warning("Remote address is unavailable");
            exportRegistration.setException(new Throwable("Remote address is unavailable"));
            return;
        }

        LOG.info("Creating a " + iClass.getName() + " endpoint from CXF PublishHook, address is " + address);

        // The properties for the EndpointDescription
        Map<String, Object> endpointProps = new HashMap<String, Object>();

        copyEndpointProperties(sd, endpointProps);

        String[] sa = new String[1];
        sa[0] = iClass.getName();
        endpointProps.put(org.osgi.framework.Constants.OBJECTCLASS, sa);
        // endpointProps.put(RemoteConstants.SERVICE_IMPORTED, "someValue;-)"); // should be done when the
        // service is imported
        // endpointProps.put(RemoteConstants.SERVICE_REMOTE_ID, "TODO");

        // FIXME: This key is not defined in the spec but is required by the EndpointDescription !!!!!
        endpointProps.put(RemoteConstants.ENDPOINT_ID, 123L);

        endpointProps.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, OsgiUtils.getUUID(getBundleContext()));
        endpointProps.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, Constants.WS_CONFIG_TYPE);
        endpointProps.put(RemoteConstants.ENDPOINT_PACKAGE_VERSION_ + sa[0], OsgiUtils.getVersion(iClass, dswContext));
        endpointProps.put(RemoteConstants.SERVICE_INTENTS, Utils.getAllIntentsCombined(sd));

        DataBinding databinding;
        String dataBindingImpl = (String)exportRegistration.getExportedService()
            .getProperty(Constants.WS_DATABINDING_PROP_KEY);
        if ("jaxb".equals(dataBindingImpl)) {
            databinding = new JAXBDataBinding();
        } else {
            databinding = new AegisDatabinding();
        }
        String frontEndImpl = (String)exportRegistration.getExportedService()
            .getProperty(Constants.WS_FRONTEND_PROP_KEY);
        ServerFactoryBean factory = createServerFactoryBean(frontEndImpl);

        factory.setServiceClass(iClass);        
        factory.setAddress(address);
        factory.getServiceFactory().setDataBinding(databinding);
        factory.setServiceBean(serviceBean);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            String[] intents = applyIntents(dswContext, callingContext, factory.getFeatures(), factory, sd);

            Thread.currentThread().setContextClassLoader(ServerFactoryBean.class.getClassLoader());
            Server server = factory.create();
            endpointProps.put(RemoteConstants.ENDPOINT_URI, address);

            exportRegistration.setServer(server);

        } catch (IntentUnsatifiedException iue) {
            exportRegistration.setException(iue);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }

        // add the information on the new Endpoint to the export registration
        EndpointDescription ed = new EndpointDescription(endpointProps);
        exportRegistration.setEndpointdescription(ed);
    }

    

    private void copyEndpointProperties(Map sd, Map<String, Object> endpointProps) {
        Set<Map.Entry> keys = sd.entrySet();
        for (Map.Entry entry : keys) {
            try{
                String skey = (String)entry.getKey();
                if (!skey.startsWith("."))
                    endpointProps.put(skey, entry.getValue());
            }catch (ClassCastException e) {
                LOG.warning("ServiceProperties Map contained non String key. Skipped  "+entry + "   "+e.getLocalizedMessage());
            }
        }
    }



//    @Override
//    Map<String, String> registerPublication(Server server, String[] intents) {
//        Map<String, String> publicationProperties = super.registerPublication(server, intents);
//        publicationProperties.put(Constants.WS_ADDRESS_PROPERTY, server.getDestination().getAddress()
//            .getAddress().getValue());
//
//        return publicationProperties;
//    }

    protected String getPojoAddress(Map sd, Class<?> iClass) {
        String address = OsgiUtils.getProperty(sd, RemoteConstants.ENDPOINT_URI);
        if (address == null) {
            address = OsgiUtils.getProperty(sd, Constants.WS_ADDRESS_PROPERTY);
        }
        if (address == null) {
            address = OsgiUtils.getProperty(sd, Constants.WS_ADDRESS_PROPERTY_OLD);
        }
        if (address == null) {
            address = getDefaultAddress(iClass);
            if (address != null) {
                LOG.info("Using a default address : " + address);
            }
        }
        return address;
    }

}
