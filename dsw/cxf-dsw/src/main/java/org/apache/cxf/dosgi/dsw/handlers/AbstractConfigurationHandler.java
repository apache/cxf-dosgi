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

import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.osgi.framework.BundleContext;

public abstract class AbstractConfigurationHandler implements ConfigurationTypeHandler {
    private final CxfDistributionProvider distributionProvider;
    final Map<String, Object> handlerProps;
    protected BundleContext bundleContext;
    
    protected AbstractConfigurationHandler(BundleContext dswBC,
                                           CxfDistributionProvider dp, 
                                           Map<String, Object> handlerProps) {
        this.bundleContext = dswBC;
        this.distributionProvider = dp;
        this.handlerProps = handlerProps;
    }
    
    protected String getDefaultAddress(Class<?> type) {        
        String host = handlerProps.get(Constants.DEFAULT_HOST_CONFIG).toString();
        String port = handlerProps.get(Constants.DEFAULT_PORT_CONFIG).toString();
        return getAddress("http", host, port, "/" + type.getName().replace('.', '/'));        
    }

    protected String getAddress(String scheme, String host, String port, String context) {
        StringBuilder buf = new StringBuilder();
        buf.append(scheme).append("://").append(host).append(':').append(port).append(context);
        return buf.toString();
    }
    
    protected boolean useMasterMap() {
        
        Object value = handlerProps.get(Constants.USE_MASTER_MAP);
        if (value == null) {
            return true;
        }
        
        return OsgiUtils.toBoolean(value);        
    }
    
    protected CxfDistributionProvider getDistributionProvider() {
        return distributionProvider;
    }
    
    protected Object getProxy(Object serviceProxy, Class<?> iType) {
        return Proxy.newProxyInstance(iType.getClassLoader(),
                      new Class[] {iType},
                      new ServiceInvocationHandler(serviceProxy, iType));
    }    
}
