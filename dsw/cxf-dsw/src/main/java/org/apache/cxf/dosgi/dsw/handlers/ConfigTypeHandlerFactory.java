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

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.discovery.ServiceEndpointDescription;
import org.osgi.service.distribution.DistributionConstants;

public final class ConfigTypeHandlerFactory {
    
    private static final Logger LOG = Logger.getLogger(ConfigTypeHandlerFactory.class.getName());
    private static final ConfigTypeHandlerFactory FACTORY = new ConfigTypeHandlerFactory();
    
    private ConfigTypeHandlerFactory() {}
    
    public static ConfigTypeHandlerFactory getInstance() {
        return FACTORY;
    }
    
    public ConfigurationTypeHandler getHandler(BundleContext dswBC, ServiceEndpointDescription sd, 
                                               CxfDistributionProvider dp,
                                               Map<String, Object> handlerProperties) {
        Collection<String> types = OsgiUtils.getMultiValueProperty(sd.getProperty(Constants.EXPORTED_CONFIGS));
        if (types == null) {
            types = OsgiUtils.getMultiValueProperty(sd.getProperty(Constants.EXPORTED_CONFIGS_OLD));
        }
        
        if (types == null 
        	|| types.contains(Constants.WS_CONFIG_TYPE) 
            || types.contains(Constants.WS_CONFIG_TYPE_OLD)
            || types.contains(Constants.RS_CONFIG_TYPE)) {

        	if (types == null) {
                LOG.info("Defaulting to pojo configuration type ");
            }

            boolean jaxrs = isJaxrsRequested(types, sd); 
            
            if (OsgiUtils.getProperty(sd, Constants.WS_HTTP_SERVICE_CONTEXT) != null
            	|| OsgiUtils.getProperty(sd, Constants.RS_HTTP_SERVICE_CONTEXT) != null	
            	|| OsgiUtils.getProperty(sd, Constants.WS_HTTP_SERVICE_CONTEXT_OLD) != null) {
            	return jaxrs ? new JaxRSHttpServiceConfigurationTypeHandler(dswBC, dp, handlerProperties)
                               : new HttpServiceConfigurationTypeHandler(dswBC, dp, handlerProperties) ;                

            } else {
                return jaxrs ? new JaxRSPojoConfigurationTypeHandler(dswBC, dp, handlerProperties)
                             : new PojoConfigurationTypeHandler(dswBC, dp, handlerProperties);
            }
        } else if (types.contains(Constants.WSDL_CONFIG_TYPE)) {
            return new WsdlConfigurationTypeHandler(dswBC, dp, handlerProperties);
        }
        
        LOG.info("None of the configuration types in " + types + " is supported.");
        
        return null;
    }
    
    private boolean isJaxrsRequested(Collection<String> types, ServiceEndpointDescription sd) {
    	
    	if (types == null) {
    		return false;
    	}
    	
    	if (types.contains(Constants.RS_CONFIG_TYPE)) {
            String intentsProperty = OsgiUtils.getProperty(sd,
                Constants.EXPORTED_INTENTS);
            boolean hasHttpIntent = false, hasSoapIntent = false;
            if (intentsProperty != null) {
              String[] intents = OsgiUtils.parseIntents(intentsProperty);
              for (int i = 0; i < intents.length; i++) {
                if (intents[i].indexOf("SOAP") > -1) {
                  hasSoapIntent = true;
                  break;
                }
                if("HTTP".equals(intents[i])) {
                  hasHttpIntent = true;
                }
              }
            }
            if (intentsProperty != null && hasHttpIntent && !hasSoapIntent
          	  || intentsProperty == null) {
                return true;
            }
        }
    	return false;
    }
    
}
