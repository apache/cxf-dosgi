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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.dosgi.dsw.service.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public abstract class AbstractConfigurationHandler implements ConfigurationTypeHandler {

    private static final Logger LOG = Logger.getLogger(AbstractConfigurationHandler.class.getName());

    final Map<String, Object> handlerProps;
    protected BundleContext bundleContext;

    protected AbstractConfigurationHandler(BundleContext dswBC, Map<String, Object> handlerProps) {
        this.bundleContext = dswBC;
        this.handlerProps = handlerProps;
    }

    protected String getDefaultAddress(Class<?> type) {
        return getDefaultAddress(type, null);
    }
    
    protected String getDefaultAddress(Class<?> type, String port) {
        Object h = handlerProps.get(Constants.DEFAULT_HOST_CONFIG);
        if (h == null) {
            h = "localhost";
        }
        String host = h.toString();

        if (port == null) {
            Object p = handlerProps.get(Constants.DEFAULT_PORT_CONFIG);
            if (p == null) {
                p = "9000";
            }
            port = p.toString();
        } 

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

    protected Object getProxy(Object serviceProxy, Class<?> iType) {
        return Proxy.newProxyInstance(iType.getClassLoader(), new Class[] {
            iType
        }, new ServiceInvocationHandler(serviceProxy, iType));
    }

    protected BundleContext getBundleContext() {
        return bundleContext;
    }

    protected Map<String, Object> createEndpointProps(Map sd, Class<?> iClass, String[] importedConfigs,
                                                      String address, String[] intents) {
        Map<String, Object> props = new HashMap<String, Object>();

        copyEndpointProperties(sd, props);

        String[] sa = new String[] {iClass.getName()};
        String pkg = iClass.getPackage().getName();
        
        props.remove(org.osgi.framework.Constants.SERVICE_ID);
        props.put(org.osgi.framework.Constants.OBJECTCLASS, sa);
        props.put(RemoteConstants.ENDPOINT_SERVICE_ID, sd.get(org.osgi.framework.Constants.SERVICE_ID));        
        props.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID, OsgiUtils.getUUID(getBundleContext()));
        props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, importedConfigs);
        props.put(RemoteConstants.ENDPOINT_PACKAGE_VERSION_ + pkg, 
                OsgiUtils.getVersion(iClass, getBundleContext()));

        for (String configurationType : importedConfigs) {
            if(Constants.WS_CONFIG_TYPE.equals(configurationType))
                props.put(Constants.WS_ADDRESS_PROPERTY, address);
            else if(Constants.RS_CONFIG_TYPE.equals(configurationType))
                props.put(Constants.RS_ADDRESS_PROPERTY, address);
            else if(Constants.WS_CONFIG_TYPE_OLD.equals(configurationType)){
                props.put(Constants.WS_ADDRESS_PROPERTY_OLD, address);
                props.put(Constants.WS_ADDRESS_PROPERTY, address);
            }
        }
        
        {
            String[] allIntents = Utils.mergeArrays(intents, Utils.getInetntsImplementedByTheService(sd));
            props.put(RemoteConstants.SERVICE_INTENTS, allIntents);
        }

        //        for (String cfg : importedConfigs) {
        //            props.put(cfg + ".stuff", "unused");
        //        }

        // make sure that the Endpoint contains the address that was actualy used
        addAddressProperty(props, address);

        return props;

    }

    private void copyEndpointProperties(Map sd, Map<String, Object> endpointProps) {
        Set<Map.Entry> keys = sd.entrySet();
        for (Map.Entry entry : keys) {
            try {
                String skey = (String)entry.getKey();
                if (!skey.startsWith("."))
                    endpointProps.put(skey, entry.getValue());
            } catch (ClassCastException e) {
                LOG.warning("ServiceProperties Map contained non String key. Skipped  " + entry + "   "
                            + e.getLocalizedMessage());
            }
        }
    }

    protected void addAddressProperty(Map props, String address) {
        if (props != null) {
            props.put(RemoteConstants.ENDPOINT_ID, address);
        }
    }
}
