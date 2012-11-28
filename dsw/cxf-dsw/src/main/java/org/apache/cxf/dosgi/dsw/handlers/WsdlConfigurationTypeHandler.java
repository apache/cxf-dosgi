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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class WsdlConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {
    private static final String CONFIGURATION_TYPE = "wsdl";
    private static final Logger LOG = LoggerFactory.getLogger(WsdlConfigurationTypeHandler.class);
    
    public WsdlConfigurationTypeHandler(BundleContext dswBC,
                                        IntentManager intentManager,
                                        HttpServiceManager httpServiceManager,
                                        Map<String, Object> handlerProps) {
        super(dswBC, intentManager, httpServiceManager, handlerProps);
    }
    
    public String getType() {
        return CONFIGURATION_TYPE;
    }

    public Object createProxy(ServiceReference serviceReference,
                              BundleContext dswContext,
                              BundleContext callingContext,
                              Class<?> iClass, 
                              EndpointDescription sd) {
        
        String wsdlAddressProp = getWsdlAddress(sd, iClass);
        if (wsdlAddressProp == null) {
            LOG.warn("WSDL address is unavailable");
            return null;
        }
        
        URL wsdlAddress = null;
        try {
            wsdlAddress = new URL(wsdlAddressProp);
        } catch (MalformedURLException ex) {
            LOG.warn("WSDL address is malformed");
            return null;
        }
        
        LOG.info("Creating a " + sd.getInterfaces().toArray()[0] + " client, wsdl address is "
                 + OsgiUtils.getProperty(sd, Constants.WSDL_CONFIG_PREFIX));
        
        String serviceNs = OsgiUtils.getProperty(sd, Constants.WSDL_SERVICE_NAMESPACE);
        if (serviceNs == null) {
            serviceNs = PackageUtils.getNamespace(
                            PackageUtils.getPackageName(iClass));
        }
        String serviceName = OsgiUtils.getProperty(sd, Constants.WSDL_SERVICE_NAME);
        if (serviceName == null) {
        	serviceName = iClass.getSimpleName();	
        }
        QName serviceQname = getServiceQName(iClass, sd.getProperties(),
        		Constants.WSDL_SERVICE_NAMESPACE, Constants.WSDL_SERVICE_NAME);
        QName portQname = getPortQName(serviceQname.getNamespaceURI(), sd.getProperties(), Constants.WSDL_PORT_NAME);
        Service service = createWebService(wsdlAddress, serviceQname);
        Object proxy = getProxy(
            portQname == null ? service.getPort(iClass) : service.getPort(portQname, iClass), 
        	iClass);
        //MARC: FIXME !!!! getDistributionProvider().addRemoteService(serviceReference);
        return proxy;
        
    }

    // Isolated so that it can be overridden for test purposes.
    Service createWebService(URL wsdlAddress, QName serviceQname) {
        return Service.create(wsdlAddress, serviceQname);
    }

    public ExportResult createServer(ServiceReference sref,
                               BundleContext dswContext,
                               BundleContext callingContext,
                               Map<String, Object> sd, 
                               Class<?> iClass, 
                               Object serviceBean) {
        
    	String location = OsgiUtils.getProperty(sd, Constants.WSDL_LOCATION);
    	if (location == null) {
    	    throw new RuntimeException("WSDL location property is unavailable");
    	}
        URL wsdlURL = dswContext.getBundle().getResource(location);
        if (wsdlURL == null) {
            throw new RuntimeException("WSDL resource at " + location + " is unavailable");
    	}
        
    	String address = getServerAddress(sd, iClass);
    	String contextRoot = httpServiceManager.getServletContextRoot(sd, iClass);
        if (address == null && contextRoot == null) {
            throw new RuntimeException("Remote address is unavailable");
        }

        LOG.info("Creating a " + iClass.getName() + " endpoint from CXF PublishHook, address is " + address);

        DataBinding databinding = new JAXBDataBinding();
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        if (contextRoot != null) {
            Bus bus = httpServiceManager.registerServletAndGetBus(contextRoot, dswContext, sref);
            factory.setBus(bus);
        }
        factory.setServiceClass(iClass);
        factory.setAddress(address != null ? address : "/");
        factory.getServiceFactory().setDataBinding(databinding);
        factory.setServiceBean(serviceBean);

        InterceptorUtils.addWsInterceptorsFeaturesProps(factory, callingContext, sd);
        
        setWsdlProperties(factory, callingContext, sd, true);
        
        String[] intents = intentManager.applyIntents(factory.getFeatures(), factory, sd);

        // The properties for the EndpointDescription
        Map<String, Object> endpointProps = createEndpointProps(sd, iClass, new String[]{Constants.WS_CONFIG_TYPE}, address,intents);

        return createServerFromFactory(factory, endpointProps);
    }
    
    private String getWsdlAddress(EndpointDescription sd, Class<?> iClass) {
        String address = OsgiUtils.getProperty(sd, Constants.WSDL_CONFIG_PREFIX);
        if (address == null) {
            address = getDefaultAddress(iClass);
            if (address != null) {
                address += "?wsdl";    
            }
        }
        return address;
    }

}
