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
import java.util.Hashtable;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.dosgi.dsw.decorator.ServiceDecorator;
import org.apache.cxf.dosgi.dsw.decorator.ServiceDecoratorImpl;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.dosgi.dsw.qos.IntentUtils;
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

    private final static Logger LOG = LoggerFactory.getLogger(Activator.class);

    private static final String CONFIG_SERVICE_PID = "cxf-dsw";

    private ServiceRegistration rsaFactoryReg;

    private ServiceRegistration decoratorReg;

    public void start(BundleContext bc) throws Exception {
        // Disable the fast infoset as it's not compatible (yet) with OSGi
        System.setProperty("org.apache.cxf.nofastinfoset", "true");

        // should we have a seperate PID for a find and publish hook ?
        // context.registerService(ManagedService.class.getName(), this, getDefaults());

        registerRemoteServiceAdminService(bc);

        decoratorReg = bc.registerService(ServiceDecorator.class.getName(), new ServiceDecoratorImpl(bc),
                                          null);

    }

    private RemoteServiceadminFactory registerRemoteServiceAdminService(BundleContext bc) {
    	IntentMap intentMap = IntentUtils.getIntentMap(bc);
        RemoteServiceadminFactory rsaf = new RemoteServiceadminFactory(bc, intentMap);
        Hashtable<String, Object> props = new Hashtable<String, Object>();

        // TODO .... RemoteAdminService.XXX
        // props.put(DistributionProvider.PRODUCT_NAME, getHeader("Bundle-Name"));
        // props.put(DistributionProvider.PRODUCT_VERSION, getHeader("Bundle-Version"));
        // props.put(DistributionProvider.VENDOR_NAME, getHeader("Bundle-Vendor"));

        String[] supportedIntents = intentMap.getIntents().keySet().toArray(new String[] {});
        String siString = IntentUtils.formatIntents(supportedIntents);
        props.put("remote.intents.supported", siString);

        // // TODO make this a little smarter
        String[] supportedConfigs = {
                                     org.apache.cxf.dosgi.dsw.Constants.WS_CONFIG_TYPE,
                                     org.apache.cxf.dosgi.dsw.Constants.WS_CONFIG_TYPE_OLD,
                                     org.apache.cxf.dosgi.dsw.Constants.RS_CONFIG_TYPE
        };
        props.put("remote.configs.supported", supportedConfigs);
        
        LOG.info("Registering RemoteServiceAdminFactory...");

        rsaFactoryReg = bc.registerService(RemoteServiceAdmin.class.getName(), rsaf, props);
        return rsaf;
    }

    public void stop(BundleContext context) throws Exception {
        LOG.debug("RemoteServiceAdmin Implementation is shutting down now");
        
        // This also triggers the unimport and unexport of the remote services
        rsaFactoryReg.unregister();
        decoratorReg.unregister();

        // shutdown the CXF Bus -> Causes also the shutdown of the embedded HTTP server
        Bus b = BusFactory.getDefaultBus();
        if (b != null) {
            LOG.debug("Shutting down the CXF Bus");
            b.shutdown(true);
        }

        // unregister other registered services (ManagedService + Hooks)
    }

    @SuppressWarnings("rawtypes")
	public synchronized void updated(Dictionary props) throws ConfigurationException {
        if (props != null && CONFIG_SERVICE_PID.equals(props.get(Constants.SERVICE_PID))) {
            // topManager.updated(props);
        }
    }

}
