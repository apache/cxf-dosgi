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
package org.apache.cxf.dosgi.dsw.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.cxf.dosgi.dsw.ClassUtils;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.handlers.ClientServiceFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigTypeHandlerFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class RemoteServiceAdminCore implements RemoteServiceAdmin {

    private Logger LOG = Logger.getLogger(RemoteServiceAdminCore.class.getName());

    private LinkedHashMap<ServiceReference, Collection<ExportRegistrationImpl>> exportedServices = new LinkedHashMap<ServiceReference, Collection<ExportRegistrationImpl>>();
    private LinkedHashMap<EndpointDescription, ImportRegistrationImpl> importedServices = new LinkedHashMap<EndpointDescription, ImportRegistrationImpl>();

    private BundleContext bctx;

    private EventProducer eventProducer;

    private volatile boolean useMasterMap = true;
    private volatile String defaultPort;
    private volatile String defaultHost;
    
    
    private static List<String> supportedConfigurationTypes = new ArrayList<String>();
    static {
        supportedConfigurationTypes.add(Constants.WSDL_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.RS_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.WS_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.WS_CONFIG_TYPE_OLD);
    }

    public RemoteServiceAdminCore(BundleContext bc) {
        bctx = bc;
        eventProducer = new EventProducer(bctx);
    }

    public List exportService(ServiceReference sref, Map additionalProperties)
        throws IllegalArgumentException, UnsupportedOperationException {

        LOG.fine("RemoteServiceAdmin: exportService: " + sref.getClass().getName());

        synchronized (exportedServices) {
            // check if it is already exported ....
            if (exportedServices.containsKey(sref)) {
                LOG.fine("already exported ...  " + sref.getClass().getName());
                Collection<ExportRegistrationImpl> regs = exportedServices.get(sref);

                // / create a new list with copies of the exportRegistrations
                List<ExportRegistrationImpl> copy = new ArrayList<ExportRegistrationImpl>(regs.size());
                for (ExportRegistrationImpl exportRegistration : regs) {
                    copy.add(new ExportRegistrationImpl(exportRegistration));
                }
                eventProducer.publishNotifcation(copy);
                return copy;
            }

            if (isCreatedByThisRSA(sref)) {
                LOG.fine("proxy provided by this bundle ...  " + sref.getClass().getName());
                return null;
            }

            Properties serviceProperties = new Properties();

            {// gather EventProducerproperties from sRef
                String[] keys = sref.getPropertyKeys();
                for (String k : keys) {
                    serviceProperties.put(k, sref.getProperty(k));
                }
            }

            if (additionalProperties != null) {// overlay properties with the additionalProperies
                Set<Map.Entry> adProps = additionalProperties.entrySet();
                for (Map.Entry e : adProps) {
                    // objectClass and service.id must not be overwritten
                    Object keyO = e.getKey();
                    if (keyO instanceof String && keyO != null) {
                        String key = ((String)keyO).toLowerCase();
                        if (org.osgi.framework.Constants.SERVICE_ID.toLowerCase().equals(key)
                            || org.osgi.framework.Constants.OBJECTCLASS.toLowerCase().equals(key)) {
                            LOG
                                .info("exportService called with additional properties map that contained illegal key: "
                                      + key + "   The key is ignored");
                            continue;
                        }
                    }
                    serviceProperties.put(e.getKey(), e.getValue());
                    LOG.fine("Overwriting property ["+e.getKey()+"]  with value ["+e.getValue()+"]");
                }
            }
            
            
          
            
            
            // Get the intents that need to be supported by the RSA
            String[] requiredIntents = Utils.getAllRequiredIntents(serviceProperties);
            
            { // TODO: Determine if the required intents can be provided by the RSA ....
                
                // if not return null
            }
            
            
            
            List<String> interfaces = new ArrayList<String>(1);

            {// determine which interfaces should be exported ? based on props and sRef
                String[] providedInterfaces = (String[])serviceProperties
                    .get(org.osgi.framework.Constants.OBJECTCLASS);
                String[] allowedInterfaces = Utils.normalizeStringPlus(serviceProperties
                    .get(RemoteConstants.SERVICE_EXPORTED_INTERFACES));
                if (providedInterfaces == null || allowedInterfaces == null) {
                    LOG
                        .severe("export failed: no provided service interfaces found or service_exported_interfaces is null !!");
                    return null;
                }

                if (allowedInterfaces.length == 1 && "*".equals(allowedInterfaces[0])) {
                    for (String i : providedInterfaces) {
                        interfaces.add(i);
                    }
                } else {
                    for (String x : allowedInterfaces) {
                        for (String i : providedInterfaces) {
                            if (x.equals(i)) {
                                interfaces.add(i);
                            }
                        }
                    }
                }
            }

            LOG.info("interfaces selected for export: " + interfaces);

            // if no interface is to be exported return null
            if (interfaces.size() == 0) {
                LOG.info("no interfaces to be exported");
                return null;
            }

            List<String> configurationTypes = new ArrayList<String>();
            {// determine which configuration types should be used / if the requested are supported
                String[] requestedConfigurationTypes = Utils.normalizeStringPlus(serviceProperties
                    .get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
                if (requestedConfigurationTypes == null || requestedConfigurationTypes.length == 0) {
                    // add all supported
                    configurationTypes.addAll(supportedConfigurationTypes);
                } else {
                    for (String rct : requestedConfigurationTypes) {
                        if (supportedConfigurationTypes.contains(rct)) {
                            // this RSA supports this requested type ...
                            configurationTypes.add(rct);
                        }
                    }
                }

            }

            LOG.info("configuration types selected for export: " + configurationTypes);

            // if no configuration type is supported ? return null
            if (configurationTypes.size() == 0) {
                LOG.info("the requested configuration types are not supported");
                return null;
            }

            LinkedHashMap<String, ExportRegistrationImpl> exportRegs = new LinkedHashMap<String, ExportRegistrationImpl>(
                                                                                                                         1);
            
            for (String iface : interfaces) {
                LOG.info("creating initial ExportDescription for interface " + iface
                         + "  with configuration types " + configurationTypes);

                // create initial ExportRegistartion
                ExportRegistrationImpl expReg = new ExportRegistrationImpl(sref, null, this);

                exportRegs.put(iface, expReg);

            }

            // enlist initial export Registrations in global list of exprtRegistrations
            exportedServices.put(sref, exportRegs.values());

            // FIXME: move out of synchronized ... -> blocks until publication is finished
            for (String iface : interfaces) {
                LOG.info("creating server for interface " + iface);

                ExportRegistrationImpl exportRegistration = exportRegs.get(iface);
                ConfigurationTypeHandler handler = getHandler(configurationTypes, serviceProperties,
                                                              getHandlerProperties());
                Object serviceObject = bctx.getService(sref);
                BundleContext callingContext = sref.getBundle().getBundleContext();

                if (handler == null) {
                    return null;
                }

                LOG.info("found handler for " + iface + "  -> " + handler);

                String interfaceName = iface;
                // this is an extra sanity check, but do we really need it now ?
                Class<?> interfaceClass = ClassUtils.getInterfaceClass(serviceObject, interfaceName);

                if (interfaceClass != null) {

                    handler.createServer(exportRegistration, bctx, callingContext, serviceProperties,
                                         interfaceClass, serviceObject);
                    LOG.info("created server for interface " + iface);

                }
            }

            List<ExportRegistrationImpl> lExpReg = new ArrayList<ExportRegistrationImpl>(exportRegs.values());

            eventProducer.publishNotifcation(lExpReg);

            return lExpReg;
        }
    }

    private boolean isCreatedByThisRSA(ServiceReference sref) {
        return sref.getBundle().equals(bctx.getBundle());
    }

    public Collection getExportedServices() {
        synchronized (exportedServices) {
            List<ExportRegistrationImpl> ers = new ArrayList<ExportRegistrationImpl>();
            for (Collection<ExportRegistrationImpl> exportRegistrations : exportedServices.values()) {
                ers.addAll(exportRegistrations);
            }
            return Collections.unmodifiableCollection(ers);
        }
    }

    public Collection getImportedEndpoints() {
        synchronized (importedServices) {
            return Collections.unmodifiableCollection(importedServices.values());
        }
    }



    private ConfigurationTypeHandler getHandler(List<String> configurationTypes, Map serviceProperties,
                                                Map<String, Object> props) {
        return ConfigTypeHandlerFactory.getInstance().getHandler(bctx, configurationTypes, serviceProperties,
                                                                 props);
    }

    protected Map<String, Object> getHandlerProperties() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.DEFAULT_PORT_CONFIG, defaultPort == null
            ? Constants.DEFAULT_PORT_VALUE : defaultPort);
        props.put(Constants.DEFAULT_HOST_CONFIG, defaultHost == null
            ? Constants.DEFAULT_HOST_VALUE : defaultHost);
        props.put(Constants.USE_MASTER_MAP, useMasterMap);
        return props;
    }

    /**
     * Importing form here ....
     */

    /**
     * For me it looks like the RSA is only able to import one interface by creating one proxy service which
     * can than be placed in the ImportRegistration object. However the decision which service should be
     * imported lies in the hands f the TopologyManager as the RSA has no idea about service interests ...
     * Therefore the TM needs to modify the EndpointDescription in such a way that only one interface is
     * listed in it anymore...
     */
    public ImportRegistration importService(EndpointDescription endpoint) {

        LOG.info("importService() Endpoint: " + endpoint.getProperties());

        synchronized (importedServices) {
            if (importedServices.containsKey(endpoint)) {
                ImportRegistrationImpl ir = new ImportRegistrationImpl(importedServices.get(endpoint));
                eventProducer.publishNotifcation(ir);
                return ir;
            }

            List remoteConfigurationTypes = endpoint.getConfigurationTypes(); 

            if (remoteConfigurationTypes == null) {
                LOG.severe("the supplied endpoint has no configuration type");
                return null;
            }

            List<String> usableConfigurationTypes = new ArrayList<String>();
            for (String ct : supportedConfigurationTypes) {
                if (remoteConfigurationTypes.contains(ct)) {
                    usableConfigurationTypes.add(ct);
                }
            }

            if (usableConfigurationTypes.size() == 0) {
                LOG
                    .severe("the supplied endpoint has no compatible configuration type. Supported types are: "
                            + supportedConfigurationTypes  +  "    Types needed by the endpoint: " +remoteConfigurationTypes);
                return null;
            }

            Map<String, Object> emptyProps = Collections.EMPTY_MAP;
            ConfigurationTypeHandler handler = getHandler(usableConfigurationTypes, endpoint.getProperties(),
                                                          emptyProps);

            if (handler == null) {
                LOG.severe("no handler found");
                return null;
            }

            LOG.fine("Handler: "+handler);

            // // TODO: somehow select the interfaces that should be imported ----> job of the TopologyManager
            // ?
            List<String> matchingInterfaces = endpoint.getInterfaces();

            LOG.info("Interfaces: " + matchingInterfaces);

            if (matchingInterfaces.size() == 1) {
                LOG.info("Proxifying interface : " + matchingInterfaces.get(0));

                ImportRegistrationImpl imReg = new ImportRegistrationImpl(endpoint,this);

                proxifyMatchingInterface(matchingInterfaces.get(0), imReg, handler, bctx);
                importedServices.put(endpoint, imReg);
                eventProducer.publishNotifcation(imReg);
                return imReg;
            } else {
                return null;
            }
        }

    }

    protected void proxifyMatchingInterface(String interfaceName, ImportRegistrationImpl imReg,
                                          ConfigurationTypeHandler handler, BundleContext requestingContext) {

        try {
            // MARC: relies on dynamic imports ?
            Class<?> iClass = bctx.getBundle().loadClass(interfaceName);
            if (iClass != null) {
                BundleContext actualContext = bctx;
                Class<?> actualClass = requestingContext.getBundle().loadClass(interfaceName);
                if (actualClass != iClass) {
                    LOG.info("Class " + interfaceName + " loaded by DSW's bundle context is not "
                             + "equal to the one loaded by the requesting bundle context, "
                             + "DSW will use the requesting bundle context to register " + "a proxy service");
                    iClass = actualClass;
                    actualContext = requestingContext;
                }

                /* TODO: add additional local params ... */
                Dictionary serviceProps = new Hashtable(imReg.getImportedEndpointDescription()
                    .getProperties());
                serviceProps.put(RemoteConstants.SERVICE_IMPORTED, true);
                serviceProps.remove(RemoteConstants.SERVICE_EXPORTED_INTERFACES);

                // synchronized (discoveredServices) {
                ClientServiceFactory csf = new ClientServiceFactory(actualContext, iClass, imReg
                    .getImportedEndpointDescription(), handler,imReg);

                imReg.setClientServiceFactory(csf);
                ServiceRegistration proxyRegistration = actualContext.registerService(interfaceName, csf,
                                                                                      serviceProps);
                imReg.setImportedServiceRegistration(proxyRegistration);
                // cacheEndpointId(sd, proxyRegistration);
                // }
            } else {
                LOG.info("not proxifying service, cannot load interface class: " + interfaceName);
                imReg.setException(new ClassNotFoundException(
                                                              "not proxifying service, cannot load interface class: "
                                                                  + interfaceName));
            }
        } catch (ClassNotFoundException ex) {
            LOG.warning("No class can be found for " + interfaceName);
            imReg.setException(ex);
        }
    }

    /**
     * Removes the provided Export Registration from the internal management structures -> intended to be used
     * when the export Registration is closed
     */
    protected void removeExportRegistration(ExportRegistrationImpl eri) {

        synchronized (exportedServices) {
            Collection<ExportRegistrationImpl> exRegs = exportedServices.get(eri.getServiceReference());
            if (exRegs.contains(eri)) {
                exRegs.remove(eri);
            } else {
                LOG.severe("An exportRegistartion was intended to be removed form internal management structure but couldn't be found in it !! ");
            }
            if (exRegs.size() == 0) {
                exportedServices.remove(eri.getServiceReference());
            }

            eventProducer.notifyRemoval(eri);
        }

    }
    
    protected void removeImportRegistration(ImportRegistrationImpl iri){
        synchronized (importedServices) {
            LOG.finest("Removing importRegistration "+iri);
            if(importedServices.remove(iri.getImportedEndpointDescription())==null){
                LOG.severe("An importRegistartion couldn't be removed from the internal management structure -> structure is inconsistent !!");
            }
            eventProducer.notifyRemoval(iri);
        }
    }

}
