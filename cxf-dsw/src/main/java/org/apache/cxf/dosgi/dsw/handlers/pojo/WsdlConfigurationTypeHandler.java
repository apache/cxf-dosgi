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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.aries.rsa.spi.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.common.httpservice.HttpServiceManager;
import org.apache.cxf.dosgi.common.intent.IntentManager;
import org.apache.cxf.dosgi.common.proxy.ProxyFactory;
import org.apache.cxf.dosgi.common.util.OsgiUtils;
import org.apache.cxf.dosgi.dsw.osgi.Constants;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WsdlConfigurationTypeHandler extends AbstractPojoConfigurationTypeHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WsdlConfigurationTypeHandler.class);

    public WsdlConfigurationTypeHandler(BundleContext dswBC,
                                        IntentManager intentManager,
                                        HttpServiceManager httpServiceManager) {
        super(dswBC, intentManager, httpServiceManager);
    }

    public String[] getSupportedTypes() {
        return new String[] {Constants.WSDL_CONFIG_TYPE};
    }

    @SuppressWarnings("rawtypes")
    public Object importEndpoint(ClassLoader consumerLoader,
                                 BundleContext consumerContext,
                                 Class[] interfaces,
                                 EndpointDescription endpoint) {
        Class<?> iClass = interfaces[0];
        String wsdlAddressProp = getWsdlAddress(endpoint, iClass);
        if (wsdlAddressProp == null) {
            LOG.warn("WSDL address is unavailable");
            return null;
        }

        URL wsdlAddress;
        try {
            wsdlAddress = new URL(wsdlAddressProp);
        } catch (MalformedURLException ex) {
            LOG.warn("WSDL address is malformed");
            return null;
        }

        LOG.info("Creating a " + endpoint.getInterfaces().toArray()[0] + " client, wsdl address is "
                 + OsgiUtils.getProperty(endpoint, Constants.WSDL_CONFIG_PREFIX));

        String serviceNs = OsgiUtils.getProperty(endpoint, Constants.WSDL_SERVICE_NAMESPACE);
        if (serviceNs == null) {
            serviceNs = PackageUtils.getNamespace(PackageUtils.getPackageName(iClass));
        }
        String serviceName = OsgiUtils.getProperty(endpoint, Constants.WSDL_SERVICE_NAME);
        if (serviceName == null) {
            serviceName = iClass.getSimpleName();
        }
        QName serviceQname = WsdlSupport.getServiceQName(iClass, endpoint.getProperties(),
                                             Constants.WSDL_SERVICE_NAMESPACE,
                                             Constants.WSDL_SERVICE_NAME);
        QName portQname = WsdlSupport.getPortQName(serviceQname.getNamespaceURI(),
                endpoint.getProperties(), Constants.WSDL_PORT_NAME);
        Service service = createWebService(wsdlAddress, serviceQname);
        Object port = portQname == null ? service.getPort(iClass) : service.getPort(portQname, iClass);
        Object proxy = ProxyFactory.create(port, iClass);
        // MARC: FIXME!!!! getDistributionProvider().addRemoteService(serviceReference);
        return proxy;
    }

    // Isolated so that it can be overridden for test purposes.
    Service createWebService(URL wsdlAddress, QName serviceQname) {
        return Service.create(wsdlAddress, serviceQname);
    }

    @SuppressWarnings("rawtypes")
    public Endpoint exportService(Object serviceO,
                                  BundleContext serviceContext,
                                  Map<String, Object> sd,
                                  Class[] exportedInterfaces) {
        Class<?> iClass = exportedInterfaces[0];
        String location = OsgiUtils.getProperty(sd, Constants.WSDL_LOCATION);
        if (location == null) {
            throw new RuntimeException("WSDL location property is unavailable");
        }
        URL wsdlURL = serviceContext.getBundle().getResource(location);
        if (wsdlURL == null) {
            throw new RuntimeException("WSDL resource at " + location + " is unavailable");
        }

        String address = getServerAddress(sd, iClass);
        String contextRoot = getServletContextRoot(sd);
        if (address == null && contextRoot == null) {
            throw new RuntimeException("Remote address is unavailable");
        }

        LOG.info("Creating a " + iClass.getName() + " endpoint from CXF PublishHook, address is " + address);

        DataBinding databinding = new JAXBDataBinding();
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        final Long sid = (Long) sd.get(RemoteConstants.ENDPOINT_SERVICE_ID);
        Bus bus = createBus(sid, serviceContext, contextRoot);
        factory.setBus(bus);
        factory.setServiceClass(iClass);
        factory.setAddress(address != null ? address : "/");
        factory.getServiceFactory().setDataBinding(databinding);
        factory.setServiceBean(serviceO);

        addWsInterceptorsFeaturesProps(factory, serviceContext, sd);

        WsdlSupport.setWsdlProperties(factory, serviceContext, sd, true);

        String[] intents = intentManager.applyIntents(factory.getFeatures(), factory, sd);

        EndpointDescription epd = createEndpointDesc(sd,
                                                     new String[]{Constants.WS_CONFIG_TYPE},
                                                     address, intents);
        return createServerFromFactory(factory, epd);
    }

    private String getWsdlAddress(EndpointDescription endpoint, Class<?> iClass) {
        String address = OsgiUtils.getProperty(endpoint, Constants.WSDL_CONFIG_PREFIX);
        if (address == null) {
            address = httpServiceManager.getDefaultAddress(iClass);
            if (address != null) {
                address += "?wsdl";
            }
        }
        return address;
    }
}