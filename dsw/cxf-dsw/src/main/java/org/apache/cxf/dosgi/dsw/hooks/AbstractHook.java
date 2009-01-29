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
package org.apache.cxf.dosgi.dsw.hooks;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.OsgiService;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public abstract class AbstractHook {
    private final CxfDistributionProvider distributionProvider;
    
    private volatile BundleContext bc;    
    private volatile boolean checkBundleForMetadata = true;
    private volatile boolean useMasterMap = true;
    private volatile String defaultPort;
    private volatile String defaultHost;
    
    public AbstractHook(BundleContext bc, CxfDistributionProvider dp) {
        this.bc = bc;
        this.distributionProvider = dp;
    }
    
    protected BundleContext getContext() {
        return bc;
    }
    
    public void updateProperties(Dictionary d) {
        
        Object value = d.get(Constants.CHECK_BUNDLE);
        if (value != null) {
            checkBundleForMetadata = OsgiUtils.toBoolean(value.toString());
        }
        
        value = d.get(Constants.USE_MASTER_MAP);
        if (value != null) {
            useMasterMap = OsgiUtils.toBoolean(value.toString());
        }
        
        value = d.get(Constants.DEFAULT_HOST_CONFIG);
        defaultHost = value == null ? Constants.DEFAULT_HOST_VALUE
                                    : value.toString();
        
        value = d.get(Constants.DEFAULT_PORT_CONFIG);
        defaultPort = value == null ? Constants.DEFAULT_PORT_VALUE
                                    : value.toString();
    }
    
    protected boolean checkBundle() {
        return checkBundleForMetadata;
    }
    
    protected Map<String, Object> getHandlerProperties() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.DEFAULT_PORT_CONFIG, 
                  defaultPort == null ? Constants.DEFAULT_PORT_VALUE : defaultPort);
        props.put(Constants.DEFAULT_HOST_CONFIG, 
                  defaultHost == null ? Constants.DEFAULT_HOST_VALUE : defaultHost);
        props.put(Constants.USE_MASTER_MAP, useMasterMap);
        return props;
    }
    
    
    protected String getIdentificationProperty() {
        Bundle b = bc.getBundle();
        Object name = 
            b.getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME);
        if (name == null) {
            name = b.getHeaders().get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME);
        }
        
        Object version = b.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
        
        StringBuilder sb = new StringBuilder();
        sb.append(name.toString()).append(", version : " + version.toString());
        return sb.toString();
    }
    
    protected CxfDistributionProvider getDistributionProvider() {
        return distributionProvider;
    }        
}
