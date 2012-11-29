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
package org.apache.cxf.dosgi.dsw;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.dosgi.dsw.decorator.ServiceDecorator;
import org.apache.cxf.dosgi.dsw.decorator.ServiceDecoratorImpl;
import org.apache.cxf.dosgi.dsw.handlers.ConfigTypeHandlerFactory;
import org.apache.cxf.dosgi.dsw.handlers.HttpServiceManager;
import org.apache.cxf.dosgi.dsw.qos.DefaultIntentMapFactory;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
import org.apache.cxf.dosgi.dsw.qos.IntentManagerImpl;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.dosgi.dsw.qos.IntentTracker;
import org.apache.cxf.dosgi.dsw.qos.IntentUtils;
import org.apache.cxf.dosgi.dsw.service.RemoteServiceAdminCore;
import org.apache.cxf.dosgi.dsw.service.RemoteServiceadminFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// registered as spring bean -> start / stop called accordingly 
public class Activator implements ManagedService, BundleActivator {
    private static final int DEFAULT_INTENT_TIMEOUT = 30000;
    private final static Logger LOG = LoggerFactory.getLogger(Activator.class);
    private static final String CONFIG_SERVICE_PID = "cxf-dsw";
    private ServiceRegistration rsaFactoryReg;
    private ServiceRegistration decoratorReg;
    private IntentTracker intentTracker;
    private BundleContext bc;

    public void start(BundleContext bc) throws Exception {
        this.bc = bc;
        start(bc, new Hashtable<String, Object>());
    }

    private void start(BundleContext bc, Map<String, Object> config) {
        this.bc = bc;
        String servletBase = (String) config.get(org.apache.cxf.dosgi.dsw.Constants.SERVLET_BASE);
        // Disable the fast infoset as it's not compatible (yet) with OSGi
        System.setProperty("org.apache.cxf.nofastinfoset", "true");

        registerManagedService(bc);
        IntentMap intentMap = new IntentMap(new DefaultIntentMapFactory().create());
        intentTracker = new IntentTracker(bc, intentMap);
        intentTracker.open();
        IntentManager intentManager = new IntentManagerImpl(intentMap, DEFAULT_INTENT_TIMEOUT);
        HttpServiceManager httpServiceManager = new HttpServiceManager(bc, servletBase);
        ConfigTypeHandlerFactory configTypeHandlerFactory = new ConfigTypeHandlerFactory(bc, intentManager, httpServiceManager);
        RemoteServiceAdminCore rsaCore = new RemoteServiceAdminCore(bc, configTypeHandlerFactory);
        RemoteServiceadminFactory rsaf = new RemoteServiceadminFactory(rsaCore);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        String[] supportedIntents = intentMap.keySet().toArray(new String[] {});
        String siString = IntentUtils.formatIntents(supportedIntents);
        props.put("remote.intents.supported", siString);
        props.put("remote.configs.supported", configTypeHandlerFactory.getSupportedConfigurationTypes());
        LOG.info("Registering RemoteServiceAdminFactory...");
        rsaFactoryReg = bc.registerService(RemoteServiceAdmin.class.getName(), rsaf, props);
        decoratorReg = bc.registerService(ServiceDecorator.class.getName(), new ServiceDecoratorImpl(bc), null);
    }

    private void registerManagedService(BundleContext bc) {
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, CONFIG_SERVICE_PID);
        bc.registerService(ManagedService.class.getName(), this, props);
    }

    public void stop(BundleContext context) throws Exception {
        LOG.debug("RemoteServiceAdmin Implementation is shutting down now");
        if (intentTracker != null) {
            intentTracker.close();
            // This also triggers the unimport and unexport of the remote services
            rsaFactoryReg.unregister();
            decoratorReg.unregister();
            // shutdown the CXF Bus -> Causes also the shutdown of the embedded HTTP server
            Bus b = BusFactory.getDefaultBus();
            if (b != null) {
                LOG.debug("Shutting down the CXF Bus");
                b.shutdown(true);
            }
            intentTracker = null;
            rsaFactoryReg = null;
            decoratorReg = null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized void updated(Dictionary config) throws ConfigurationException {
        if (rsaFactoryReg != null) {
            try {
                stop(bc);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        if (config != null) {
            HashMap<String, Object> configMap = getMapFromDictionary(config);
            start(bc, configMap);
        }
    }

    private HashMap<String, Object> getMapFromDictionary(Dictionary<String, Object> config) {
        HashMap<String, Object> configMap = new HashMap<String, Object>();
        if (config == null) {
            return configMap;
        }
        Enumeration<String> keys = config.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            configMap.put(key, config.get(key));
        }
        return configMap;
    }


}
