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
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPojoConfigurationTypeHandler extends AbstractConfigurationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPojoConfigurationTypeHandler.class);
    private static final String CONFIGURATION_TYPE = "org.apache.cxf.ws";
    protected HttpServiceManager httpServiceManager;
    
    public AbstractPojoConfigurationTypeHandler(BundleContext dswBC,
                                                IntentManager intentManager,
                                                HttpServiceManager httpServiceManager,
                                                Map<String, Object> handlerProps) {
        super(dswBC, intentManager, handlerProps);
        this.httpServiceManager = httpServiceManager;
    }

    // Isolated so that it can be substituted for testing
    ClientProxyFactoryBean createClientProxyFactoryBean(ServiceReference sref, Class<?> iClass) {
        String frontEnd = (String)sref.getProperty(Constants.WS_FRONTEND_PROP_KEY);
        ClientProxyFactoryBean factory = "jaxws".equals(frontEnd) ? new JaxWsProxyFactoryBean() : new ClientProxyFactoryBean();
        String dataBindingName = (String)sref.getProperty(Constants.WS_DATABINDING_PROP_KEY);
        DataBinding databinding = "jaxb".equals(dataBindingName) ? new JAXBDataBinding() : new AegisDatabinding();
        factory.getServiceFactory().setDataBinding(databinding);
        return factory;
    }

    // Isolated so that it can be substituted for testing
    ServerFactoryBean createServerFactoryBean(ServiceReference sref, Class<?> iClass) {
        String frontEnd = (String)sref.getProperty(Constants.WS_FRONTEND_PROP_KEY);
        ServerFactoryBean factory = "jaxws".equals(frontEnd) ? new JaxWsServerFactoryBean() : new ServerFactoryBean();
        String dataBindingName = (String)sref.getProperty(Constants.WS_DATABINDING_PROP_KEY);
        DataBinding databinding = "jaxb".equals(dataBindingName) ? new JAXBDataBinding() : new AegisDatabinding();
        factory.getServiceFactory().setDataBinding(databinding);
        return factory;
    }

    protected void setWsdlProperties(ServerFactoryBean factory,
                                     BundleContext callingContext,  
                                     Map<String, Object> sd, boolean wsdlType) {
    	String location = OsgiUtils.getProperty(sd, wsdlType ? Constants.WSDL_LOCATION : Constants.WS_WSDL_LOCATION);
    	if (location != null) {
    		URL wsdlURL = callingContext.getBundle().getResource(location);
                if (wsdlURL != null) {
	            factory.setWsdlURL(wsdlURL.toString());
	        }
	        QName serviceName = getServiceQName(null, sd, 
	        		wsdlType ? Constants.WSDL_SERVICE_NAMESPACE : Constants.WS_WSDL_SERVICE_NAMESPACE, 
	        		wsdlType ? Constants.WSDL_SERVICE_NAME : Constants.WS_WSDL_SERVICE_NAME);
	        if (serviceName != null) {
	        	factory.setServiceName(serviceName);
	            QName portName = getPortQName(serviceName.getNamespaceURI(), sd, 
	            		wsdlType ? Constants.WSDL_PORT_NAME : Constants.WS_WSDL_PORT_NAME);
	            if (portName != null) {
	            	factory.setEndpointName(portName);
	            }
	        }
    	}
    }
    
    protected void setClientWsdlProperties(ClientFactoryBean factory, BundleContext dswContext, 
    		Map<String, Object> sd, boolean wsdlType) {
    	String location = OsgiUtils.getProperty(sd, wsdlType ? Constants.WSDL_LOCATION : Constants.WS_WSDL_LOCATION);
    	if (location != null) {
    		URL wsdlURL = dswContext.getBundle().getResource(location);
	        if (wsdlURL != null) {
	            factory.setWsdlURL(wsdlURL.toString());
	        }
	        QName serviceName = getServiceQName(null, sd, 
	        		wsdlType ? Constants.WSDL_SERVICE_NAMESPACE : Constants.WS_WSDL_SERVICE_NAMESPACE, 
	        		wsdlType ? Constants.WSDL_SERVICE_NAME : Constants.WS_WSDL_SERVICE_NAME);
	        if (serviceName != null) {
	        	factory.setServiceName(serviceName);
	            QName portName = getPortQName(serviceName.getNamespaceURI(), sd, 
	            		wsdlType ? Constants.WSDL_PORT_NAME : Constants.WS_WSDL_PORT_NAME);
	            if (portName != null) {
	            	factory.setEndpointName(portName);
	            }
	        }
    	}
    }
    
    protected static QName getServiceQName(Class<?> iClass, Map<String, Object> sd, String nsPropName, String namePropName) {
    	String serviceNs = OsgiUtils.getProperty(sd, nsPropName);
    	String serviceName = OsgiUtils.getProperty(sd, namePropName);
    	if (iClass == null && (serviceNs == null || serviceName == null)) {
    		return null;
    	}
    	if (serviceNs == null) {
            serviceNs = PackageUtils.getNamespace(
                            PackageUtils.getPackageName(iClass));
        }
        if (serviceName == null) {
        	serviceName = iClass.getSimpleName();	
        }
        return new QName(serviceNs, serviceName);
    }
    
    protected static QName getPortQName(String ns, Map<String, Object> sd, String propName) {
    	String portName = OsgiUtils.getProperty(sd, propName);
        if (portName == null) {
        	return null;	
        }
        return new QName(ns, portName);
    }
    
    public String getType() {
        return CONFIGURATION_TYPE;
    }
    
    protected String getClientAddress(Map<String, Object> sd, Class<?> iClass) {
        return OsgiUtils.getFirstNonEmptyStringProperty(sd, 
                RemoteConstants.ENDPOINT_ID,
                Constants.WS_ADDRESS_PROPERTY,
                Constants.WS_ADDRESS_PROPERTY,
                Constants.WS_ADDRESS_PROPERTY_OLD,
                Constants.RS_ADDRESS_PROPERTY);
    }

    protected String getServerAddress(Map<String, Object> sd, Class<?> iClass) {
        String address = null;
        try {
            address = getClientAddress(sd, iClass);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
        return (address == null) ? getDefaultAddress(iClass) : address;
    }
    
    protected final ExportResult createServerFromFactory(ServerFactoryBean factory, Map<String, Object> endpointProps) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
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

}
