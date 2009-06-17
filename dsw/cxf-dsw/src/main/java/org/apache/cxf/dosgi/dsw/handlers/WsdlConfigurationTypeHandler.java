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
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.apache.cxf.endpoint.Server;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.ServiceEndpointDescription;

public class WsdlConfigurationTypeHandler extends AbstractConfigurationHandler {
    private static final String CONFIGURATION_TYPE = "wsdl";
    private static final Logger LOG = Logger.getLogger(WsdlConfigurationTypeHandler.class.getName());
    
    public WsdlConfigurationTypeHandler(BundleContext dswBC,
                                        CxfDistributionProvider dp,
                                        Map<String, Object> handlerProps) {
        super(dswBC, dp, handlerProps);
    }
    
    public String getType() {
        return CONFIGURATION_TYPE;
    }

    public Object createProxy(ServiceReference serviceReference,
                              BundleContext dswContext,
                              BundleContext callingContext,
                              Class<?> iClass, 
                              ServiceEndpointDescription sd) {
        
        String wsdlAddressProp = getWsdlAddress(sd, iClass);
        if (wsdlAddressProp == null) {
            LOG.warning("WSDL address is unavailable");
            return null;
        }
        
        URL wsdlAddress = null;
        try {
            wsdlAddress = new URL(wsdlAddressProp);
        } catch (MalformedURLException ex) {
            LOG.warning("WSDL address is malformed");
            return null;
        }
        
        LOG.info("Creating a " + sd.getProvidedInterfaces().toArray()[0] + " client, wsdl address is "
                 + OsgiUtils.getProperty(sd, Constants.WSDL_CONFIG_PREFIX));
        
        String serviceNs = OsgiUtils.getProperty(sd, Constants.SERVICE_NAMESPACE);
        if (serviceNs == null) {
            serviceNs = PackageUtils.getNamespace(
                            PackageUtils.getPackageName(iClass));
        }
        QName serviceQname = new QName(serviceNs, iClass.getSimpleName());
        Service service = createWebService(wsdlAddress, serviceQname);
        Object proxy = getProxy(service.getPort(iClass), iClass);
        getDistributionProvider().addRemoteService(serviceReference);
        return proxy;
        
    }

    // Isolated so that it can be overridden for test purposes.
    Service createWebService(URL wsdlAddress, QName serviceQname) {
        return Service.create(wsdlAddress, serviceQname);
    }

    public Server createServer(ServiceReference sr,
                               BundleContext dswContext,
                               BundleContext callingContext,
                               ServiceEndpointDescription sd, 
                               Class<?> iClass, 
                               Object serviceBean) {
        
        throw new UnsupportedOperationException("No WSDL configuration is currently supported for"
                  + " creating service endpoints");
    }
    
    private String getWsdlAddress(ServiceEndpointDescription sd, Class<?> iClass) {
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
