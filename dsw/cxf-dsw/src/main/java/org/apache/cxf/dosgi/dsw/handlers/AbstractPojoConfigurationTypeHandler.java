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

import javax.xml.namespace.QName;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.util.ClassUtils;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPojoConfigurationTypeHandler extends AbstractConfigurationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPojoConfigurationTypeHandler.class);
    private static final String CONFIGURATION_TYPE = "org.apache.cxf.ws";
    
    public AbstractPojoConfigurationTypeHandler(BundleContext dswBC,
                                                IntentManager intentManager,
                                                Map<String, Object> handlerProps) {
        super(dswBC, intentManager, handlerProps);
    }

    // Isolated so that it can be substituted for testing
    ClientProxyFactoryBean createClientProxyFactoryBean(String frontEndImpl) {
      if("jaxws".equals(frontEndImpl)) {
        return new JaxWsProxyFactoryBean();
      } else {
        return new ClientProxyFactoryBean();
      }
    }

    // Isolated so that it can be substituted for testing
    ServerFactoryBean createServerFactoryBean(String frontEndImpl) {
      if("jaxws".equals(frontEndImpl)) {
        return new JaxWsServerFactoryBean();
      } else {
        return new ServerFactoryBean();
      }
    }

    protected void setWsdlProperties(ServerFactoryBean factory,
                                     BundleContext callingContext,  
                                     Map sd, boolean wsdlType) {
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
    
    protected void addWsInterceptorsFeaturesProps(
    		AbstractEndpointFactory factory, BundleContext callingContext, Map sd) {
    	addInterceptors(factory, callingContext, sd, Constants.WS_IN_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.WS_OUT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.WS_OUT_FAULT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.WS_IN_FAULT_INTERCEPTORS_PROP_KEY);
        addFeatures(factory, callingContext, sd, Constants.WS_FEATURES_PROP_KEY);
        addContextProperties(factory, callingContext, sd, Constants.WS_CONTEXT_PROPS_PROP_KEY);
    }
    
    protected void addRsInterceptorsFeaturesProps(
    		AbstractEndpointFactory factory, BundleContext callingContext, Map sd) {
    	addInterceptors(factory, callingContext, sd, Constants.RS_IN_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.RS_OUT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.RS_OUT_FAULT_INTERCEPTORS_PROP_KEY);
        addInterceptors(factory, callingContext, sd, Constants.RS_IN_FAULT_INTERCEPTORS_PROP_KEY);
        addFeatures(factory, callingContext, sd, Constants.RS_FEATURES_PROP_KEY);
        addContextProperties(factory, callingContext, sd, Constants.RS_CONTEXT_PROPS_PROP_KEY);
    }
        
    
    protected void setClientWsdlProperties(ClientFactoryBean factory, BundleContext dswContext, 
    		Map sd, boolean wsdlType) {
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
    
    protected QName getServiceQName(Class<?> iClass, Map sd, String nsPropName, String namePropName) {
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
    
    protected QName getPortQName(String ns, Map sd, String propName) {
    	String portName = OsgiUtils.getProperty(sd, propName);
        if (portName == null) {
        	return null;	
        }
        return new QName(ns, portName);
    }
    
    protected void addInterceptors(AbstractEndpointFactory factory, BundleContext callingContext, 
    		Map sd, String propName) {

        List<Object> providers = ClassUtils.loadProviderClasses(callingContext, sd, propName); 
        boolean in = propName.contains("in.interceptors");
        boolean out = propName.contains("out.interceptors");
        boolean in_fault = propName.contains("in.fault.interceptors");
        boolean out_fault = propName.contains("out.fault.interceptors");
        for (int i = 0; i < providers.size(); i++) {
        	Interceptor<?> interceptor = (Interceptor<?>)providers.get(i);  
	        if (in) {
	        	factory.getInInterceptors().add(interceptor);
	        } else if (out) {
	        	factory.getOutInterceptors().add(interceptor);
	        } else if (in_fault) {
	        	factory.getInFaultInterceptors().add(interceptor);
	        } else if (out_fault) {
	        	factory.getOutFaultInterceptors().add(interceptor);
	        }
        }
    }
    
       
    protected void addFeatures(AbstractEndpointFactory factory, BundleContext callingContext, 
    		Map sd, String propName) {

        List<Object> providers = ClassUtils.loadProviderClasses(callingContext, sd, propName); 
        if (providers.size() > 0) {
        	factory.getFeatures().addAll(CastUtils.cast(providers, AbstractFeature.class));
        }
    }
    
    protected void addContextProperties(AbstractEndpointFactory factory, BundleContext callingContext, 
    		Map sd, String propName) {
    	Map<String, Object> props = (Map<String, Object>)sd.get(propName);
        if (props != null) {
        	factory.getProperties(true).putAll(props);
        }
    }

    public String getType() {
        return CONFIGURATION_TYPE;
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
