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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.dosgi.dsw.decorator.ServiceDecorator;
import org.apache.cxf.dosgi.dsw.decorator.ServiceDecoratorImpl;
import org.apache.cxf.dosgi.dsw.hooks.CxfFindListenerHook;
import org.apache.cxf.dosgi.dsw.hooks.CxfPublishHook;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.dosgi.dsw.service.CxfDistributionProvider;
import org.apache.cxf.dosgi.dsw.service.DistributionProviderImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.distribution.DistributionProvider;

public class Activator implements BundleActivator, ServiceListener, ManagedService {

    private static final String CONFIG_SERVICE_PID = "cxf-dsw";
    private BundleContext bc;    
    private ExecutorService execService = 
        new ThreadPoolExecutor(5, 10, 50, TimeUnit.SECONDS, 
                               new LinkedBlockingQueue<Runnable>());

    CxfDistributionProvider dpService;
    CxfFindListenerHook lHook;
    CxfPublishHook pHook;
    ServiceRegistration decoratorReg;
    
    public void start(BundleContext context) throws Exception {
        // Disable the fast infoset as it's not compatible (yet) with OSGi
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        
        bc = context; 
        // should we have a seperate PID for a find and publish hook ? 
        context.registerService(ManagedService.class.getName(), 
                                this, getDefaults());
    
        decoratorReg = context.registerService(ServiceDecorator.class.getName(), 
            new ServiceDecoratorImpl(context), null);
        
        dpService = registerDistributionProviderService();        

        pHook = new CxfPublishHook(context, dpService);        
        lHook = new CxfFindListenerHook(context, dpService);
        context.registerService(new String [] {FindHook.class.getName(), ListenerHook.class.getName()}, lHook, new Hashtable());
        
        context.addServiceListener(this);                 
        checkExistingServices();        
    }

    private CxfDistributionProvider registerDistributionProviderService() {
        DistributionProviderImpl dpService = new DistributionProviderImpl(bc);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        
        props.put(DistributionProvider.PRODUCT_NAME, getHeader("Bundle-Name"));
        props.put(DistributionProvider.PRODUCT_VERSION, getHeader("Bundle-Version"));
        props.put(DistributionProvider.VENDOR_NAME, getHeader("Bundle-Vendor"));
                
        String[] supportedIntents = getIntentMap().getIntents().keySet().toArray(new String [] {});
        String siString = OsgiUtils.formatIntents(supportedIntents);
        props.put(DistributionProvider.SUPPORTED_INTENTS, siString);
        props.put("remote.intents.supported", supportedIntents);

        // TODO make this a little smarter
        String[] supportedConfigs = {
                org.apache.cxf.dosgi.dsw.Constants.WS_CONFIG_TYPE, 
                org.apache.cxf.dosgi.dsw.Constants.WS_CONFIG_TYPE_OLD, 
                org.apache.cxf.dosgi.dsw.Constants.RS_CONFIG_TYPE};
        props.put("remote.configs.supported", supportedConfigs);
        
        bc.registerService(DistributionProvider.class.getName(), dpService, props);
        return dpService;
    }

    IntentMap getIntentMap() {
        return OsgiUtils.getIntentMap(bc);
    }

    private Object getHeader(String key) {
        Object value = bc.getBundle().getHeaders().get(key);
        if (value == null) {
            return "";
        } else {
            return value;
        }
    }

    public void stop(BundleContext context) {
        dpService.shutdown();
        execService.shutdown();
        pHook.removeEndpoints();

        decoratorReg.unregister();        
        // unregister other registered services (ManagedService + Hooks)
    }

    private Dictionary<String, String> getDefaults() {
        Dictionary<String, String> defaults = new Hashtable<String, String>();
        defaults.put(Constants.SERVICE_PID, CONFIG_SERVICE_PID);        
        return defaults;
    }
    
    public void updated(Dictionary props) throws ConfigurationException {
        if (props != null 
            && CONFIG_SERVICE_PID.equals(props.get(Constants.SERVICE_PID))) {
            pHook.updateProperties(props);
            lHook.updateProperties(props);
        }
    }

    private void checkExistingServices() throws InvalidSyntaxException {
        ServiceReference[] references = bc.getServiceReferences(null, 
            "(|(" + org.apache.cxf.dosgi.dsw.Constants.EXPORTED_INTERFACES + "=*)" +
    		"(" + org.apache.cxf.dosgi.dsw.Constants.EXPORTED_INTERFACES_OLD + "=*))");
        
        if (references != null) {
            for (ServiceReference sref : references) {
                pHook.publishEndpoint(sref);
            }
        }
    }

    public void serviceChanged(ServiceEvent event) {
        
        final ServiceReference sref = event.getServiceReference();
        
        if (event.getType() == ServiceEvent.REGISTERED) {
            
            execService.execute(new Runnable(){
                public void run() {
                    pHook.publishEndpoint(sref);
                }
            });
            
        } else if (event.getType() == ServiceEvent.UNREGISTERING) {
            // this should be timely enough
            pHook.removeEndpoint(sref);
        }  
        
    }

}
