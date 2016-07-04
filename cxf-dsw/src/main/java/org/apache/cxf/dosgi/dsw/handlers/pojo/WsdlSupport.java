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
package org.apache.cxf.dosgi.dsw.handlers.pojo;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.dosgi.common.util.OsgiUtils;
import org.apache.cxf.dosgi.dsw.osgi.Constants;
import org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory;
import org.osgi.framework.BundleContext;

public final class WsdlSupport {

    private WsdlSupport() {
    }
    
    public static void setWsdlProperties(AbstractWSDLBasedEndpointFactory factory, BundleContext context, 
                                         Map<String, Object> sd,
                                     boolean wsdlType) {
        String location = OsgiUtils.getProperty(sd, wsdlType ? Constants.WSDL_LOCATION : Constants.WS_WSDL_LOCATION);
        if (location != null) {
            URL wsdlURL = context.getBundle().getResource(location);
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

    protected static QName getServiceQName(Class<?> iClass, Map<String, Object> sd, String nsPropName,
                                           String namePropName) {
        String serviceNs = OsgiUtils.getProperty(sd, nsPropName);
        String serviceName = OsgiUtils.getProperty(sd, namePropName);
        if (iClass == null && (serviceNs == null || serviceName == null)) {
            return null;
        }
        if (serviceNs == null) {
            serviceNs = PackageUtils.getNamespace(PackageUtils.getPackageName(iClass));
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
}
