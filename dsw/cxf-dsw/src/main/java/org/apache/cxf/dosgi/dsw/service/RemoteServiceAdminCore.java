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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.handlers.ClientServiceFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigTypeHandlerFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.qos.IntentMap;
import org.apache.cxf.dosgi.dsw.qos.IntentUtils;
import org.apache.cxf.dosgi.dsw.util.ClassUtils;
import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.dosgi.dsw.util.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class RemoteServiceAdminCore implements RemoteServiceAdmin {

    private static final Logger LOG = LogUtils.getL7dLogger(RemoteServiceAdminCore.class);

    private final LinkedHashMap<ServiceReference, Collection<ExportRegistration>> exportedServices = new LinkedHashMap<ServiceReference, Collection<ExportRegistration>>();
    private final LinkedHashMap<EndpointDescription, Collection<ImportRegistrationImpl>> importedServices = new LinkedHashMap<EndpointDescription, Collection<ImportRegistrationImpl>>();

    private BundleContext bctx;
    private EventProducer eventProducer;
    private IntentMap intentMap;

    private volatile boolean useMasterMap = true;
    private volatile String defaultPort;
    private volatile String defaultHost;

    // protected because of tests
    protected static final List<String> supportedConfigurationTypes = new ArrayList<String>();

    static {
        supportedConfigurationTypes.add(Constants.WSDL_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.RS_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.WS_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.WS_CONFIG_TYPE_OLD);
    }

    protected final static String DEFAULT_CONFIGURATION = Constants.WS_CONFIG_TYPE;

    public RemoteServiceAdminCore(BundleContext bc, IntentMap intentMap) {
        bctx = bc;
        eventProducer = new EventProducer(bctx);
        this.intentMap = intentMap;
    }

    @SuppressWarnings("rawtypes")
	public List<ExportRegistration> exportService(ServiceReference serviceReference, Map additionalProperties)
        throws IllegalArgumentException, UnsupportedOperationException {

        LOG.fine("RemoteServiceAdmin: exportService: " + serviceReference.getClass().getName());

        synchronized (exportedServices) {
            // check if it is already exported ....
            if (exportedServices.containsKey(serviceReference)) {
                LOG.fine("already exported ...  " + serviceReference.getClass().getName());
                Collection<ExportRegistration> regs = exportedServices.get(serviceReference);

                List<EndpointDescription> copiedEndpoints = new ArrayList<EndpointDescription>();

                // / create a new list with copies of the exportRegistrations
                List<ExportRegistration> copy = new ArrayList<ExportRegistration>(regs.size());
                for (ExportRegistration exportRegistration : regs) {
                	if (exportRegistration instanceof ExportRegistrationImpl) {
                		ExportRegistrationImpl exportRegistrationImpl = (ExportRegistrationImpl) exportRegistration;
                		// create one copy for each distinct endpoint description
                		if (!copiedEndpoints.contains(exportRegistrationImpl.getEndpointDescription())) {
                			copiedEndpoints.add(exportRegistrationImpl.getEndpointDescription());
                			copy.add(new ExportRegistrationImpl(exportRegistrationImpl));
                		}
                	}
                }

                regs.addAll(copy);

                eventProducer.publishNotifcation(copy);
                return copy;
            }

            if (isCreatedByThisRSA(serviceReference)) {
                LOG.fine("proxy provided by this bundle ...  " + serviceReference.getClass().getName());
                // TODO: publish error event ? Not sure
                return Collections.emptyList();
            }



            Properties serviceProperties = new Properties();

            {// gather properties from sRef
                String[] keys = serviceReference.getPropertyKeys();
                for (String k : keys) {
                    serviceProperties.put(k, serviceReference.getProperty(k));
                }
            }


            if (additionalProperties != null) {// overlay properties with the additionalProperies
                OsgiUtils.overlayProperties(serviceProperties,additionalProperties);
            }

            // Get the intents that need to be supported by the RSA
            String[] requiredIntents = IntentUtils.getAllRequiredIntents(serviceProperties);

            {
                List<String> unsupportedIntents = new ArrayList<String>();

                for (String ri : requiredIntents) {
                    if (!intentMap.getIntents().containsKey(ri)) {
                        unsupportedIntents.add(ri);
                    }
                }

                if (unsupportedIntents.size() > 0) {
                    LOG
                        .severe("service cannot be exported because the following intents are not supported by this RSA: "
                                + unsupportedIntents);
                    // TODO: publish error event
                    return Collections.emptyList();
                }

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
                    // TODO: publish error event ? not sure
                    return Collections.emptyList();
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
                LOG.warning("no interfaces to be exported");
                // TODO: publish error event ? not sure
                return Collections.emptyList();
            }

            List<String> configurationTypes = determineConfigurationTypes(serviceProperties);

            LOG.info("configuration types selected for export: " + configurationTypes);

            // if no configuration type is supported ? return null
            if (configurationTypes.size() == 0) {
                LOG.info("the requested configuration types are not supported");
                // TODO: publish error event ? not sure
                return Collections.emptyList();
            }

            LinkedHashMap<String, ExportRegistrationImpl> exportRegs = new LinkedHashMap<String, ExportRegistrationImpl>(
                                                                                                                         1);

            for (String iface : interfaces) {
                LOG.info("creating initial ExportDescription for interface " + iface
                         + "  with configuration types " + configurationTypes);

                // create initial ExportRegistartion
                ExportRegistrationImpl expReg = new ExportRegistrationImpl(serviceReference, null, this);

                exportRegs.put(iface, expReg);

            }

            // FIXME: move out of synchronized ... -> blocks until publication is finished
            for (String iface : interfaces) {
                LOG.info("creating server for interface " + iface);

                ExportRegistrationImpl exportRegistration = exportRegs.get(iface);
                ConfigurationTypeHandler handler = getHandler(configurationTypes, serviceProperties,
                                                              getHandlerProperties());
                Object serviceObject = bctx.getService(serviceReference);
                BundleContext callingContext = serviceReference.getBundle().getBundleContext();

                if (handler == null) {
                    // TODO: publish error event ? not sure
                    return Collections.emptyList();
                }

                LOG.info("found handler for " + iface + "  -> " + handler);

                String interfaceName = iface;
                // this is an extra sanity check, but do we really need it now ?
                Class<?> interfaceClass = ClassUtils.getInterfaceClass(serviceObject, interfaceName);

                if (interfaceClass != null) {

                    handler.createServer(exportRegistration, bctx, callingContext, serviceProperties,
                                         interfaceClass, serviceObject);

                    if(exportRegistration.getException()==null){
                        LOG.info("created server for interface " + iface);

                        exportRegistration.startServiceTracker(bctx);
                    }else{
                        LOG.warning("server creation for interface " + iface + "  failed!");
                        // Fire event happens at the end
                    }


                }
            }

            // enlist initial export Registrations in global list of exportRegistrations
            exportedServices.put(serviceReference, new ArrayList<ExportRegistration>(exportRegs.values()));

            List<ExportRegistration> lExpReg = new ArrayList<ExportRegistration>(exportRegs.values());
            eventProducer.publishNotifcation(lExpReg);

            return lExpReg;
        }
    }


    protected List<String> determineConfigurationTypes(Properties serviceProperties) {

        List<String> configurationTypes = new ArrayList<String>();

        {// determine which configuration types should be used / if the requested are supported
            String[] requestedConfigurationTypes = Utils.normalizeStringPlus(serviceProperties
                .get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
            if (requestedConfigurationTypes == null || requestedConfigurationTypes.length == 0) {
                // add default configuration
                configurationTypes.add(DEFAULT_CONFIGURATION);
            } else {
                for (String rct : requestedConfigurationTypes) {
                    if (supportedConfigurationTypes.contains(rct)) {
                        // this RSA supports this requested type ...
                        configurationTypes.add(rct);
                    }
                }
            }

        }

        return configurationTypes;
    }

    private boolean isCreatedByThisRSA(ServiceReference sref) {
        return (sref.getBundle() != null ) && sref.getBundle().equals(bctx.getBundle());
    }

    public Collection<ExportReference> getExportedServices() {
        synchronized (exportedServices) {
            List<ExportReference> ers = new ArrayList<ExportReference>();
            for (Collection<ExportRegistration> exportRegistrations : exportedServices.values()) {
                for (ExportRegistration er : exportRegistrations) {
                    ers.add(new ExportReferenceImpl(er));
                }
            }
            return Collections.unmodifiableCollection(ers);
        }
    }

    public Collection<ImportReference> getImportedEndpoints() {
        synchronized (importedServices) {
            List<ImportReference> irs = new ArrayList<ImportReference>();
            for (Collection<ImportRegistrationImpl> irl : importedServices.values()) {
            	for (ImportRegistrationImpl impl : irl) {
            		irs.add(impl.getImportReference());
            	}
            }
            return Collections.unmodifiableCollection(irs);
        }
    }

    private ConfigurationTypeHandler getHandler(List<String> configurationTypes, Map<?, ?> serviceProperties,
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
    public ImportRegistration importService(EndpointDescription endpoint) {

        LOG.fine("importService() Endpoint: " + endpoint.getProperties());

        synchronized (importedServices) {
            if (importedServices.containsKey(endpoint) && importedServices.get(endpoint).size() > 0) {
                LOG.fine("creating copy of existing import registrations");
                Collection<ImportRegistrationImpl> imRegs = importedServices.get(endpoint);
                ImportRegistrationImpl irParent = imRegs.iterator().next();
                ImportRegistrationImpl ir = new ImportRegistrationImpl(irParent);
                imRegs.add(ir);
                eventProducer.publishNotifcation(ir);
                return ir;
            }

            List<String> remoteConfigurationTypes = endpoint.getConfigurationTypes();

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
                            + supportedConfigurationTypes
                            + "    Types needed by the endpoint: "
                            + remoteConfigurationTypes);
                return null;
            }

            Map<String, Object> emptyProps = Collections.emptyMap();
            ConfigurationTypeHandler handler = getHandler(usableConfigurationTypes, endpoint.getProperties(),
                                                          emptyProps);

            if (handler == null) {
                LOG.severe("no handler found");
                return null;
            }

            LOG.fine("Handler: " + handler);

            // // TODO: somehow select the interfaces that should be imported ----> job of the TopologyManager
            // ?
            List<String> matchingInterfaces = endpoint.getInterfaces();

            LOG.info("Matching Interfaces for import: " + matchingInterfaces);

            if (matchingInterfaces.size() == 1) {
                LOG.info("Proxifying interface : " + matchingInterfaces.get(0));

                ImportRegistrationImpl imReg = new ImportRegistrationImpl(endpoint, this);

                proxifyMatchingInterface(matchingInterfaces.get(0), imReg, handler, bctx);
                Collection<ImportRegistrationImpl> imRegs = importedServices.get(endpoint);
                if (imRegs == null) {
                    imRegs = new ArrayList<ImportRegistrationImpl>();
                    importedServices.put(endpoint, imRegs);
                }
                imRegs.add(imReg);
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
                Dictionary<String, Object> serviceProps = new Hashtable<String, Object>(imReg.getImportedEndpointDescription()
                    .getProperties());
                serviceProps.put(RemoteConstants.SERVICE_IMPORTED, true);
                serviceProps.remove(RemoteConstants.SERVICE_EXPORTED_INTERFACES);

                // synchronized (discoveredServices) {
                ClientServiceFactory csf = new ClientServiceFactory(actualContext, iClass, imReg
                    .getImportedEndpointDescription(), handler, imReg);

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
            Collection<ExportRegistration> exRegs = exportedServices.get(eri.getServiceReference());
            if (exRegs != null && exRegs.contains(eri)) {
                exRegs.remove(eri);
            } else {
                LOG
                    .severe("An exportRegistartion was intended to be removed form internal management structure but couldn't be found in it !! ");
            }
            if (exRegs == null || exRegs.size() == 0) {
                exportedServices.remove(eri.getServiceReference());
            }

            eventProducer.notifyRemoval(eri);
        }

    }

    // Remove all export registrations associated with the given bundle
    protected void removeExportRegistrations(BundleContext exportingBundleCtx) {
        Bundle exportingBundle = exportingBundleCtx.getBundle();

        // Work on a copy as the map gets modified as part of the behaviour by underlying methods
        HashMap<ServiceReference, Collection<ExportRegistration>> exportCopy = new HashMap<ServiceReference, Collection<ExportRegistration>>(exportedServices);

        for (Iterator<Map.Entry<ServiceReference, Collection<ExportRegistration>>> it = exportCopy.entrySet().iterator(); it.hasNext(); ) {
            Entry<ServiceReference, Collection<ExportRegistration>> entry = it.next();
            Bundle regBundle = entry.getKey().getBundle();
            if (exportingBundle.equals(regBundle)) {
                // Again work on a copy, as the value gets modified by the behaviour inside export.close()
                for (ExportRegistration export : new ArrayList<ExportRegistration>(entry.getValue())) {
                    // This will remove the registration from the real map of exports
                    export.close();
                }
            }
        }
    }

    protected void removeImportRegistration(ImportRegistrationImpl iri) {
        synchronized (importedServices) {
            LOG.finest("Removing importRegistration " + iri);

            Collection<ImportRegistrationImpl> imRegs = importedServices.get(iri.getImportedEndpointAlways());
            if (imRegs!=null && imRegs.contains(iri)) {
                imRegs.remove(iri);
            } else {
                LOG
                    .severe("An importRegistartion was intended to be removed form internal management structure but couldn't be found in it !! ");
            }
            if (imRegs == null || imRegs.size() == 0) {
                importedServices.remove(iri.getImportedEndpointAlways());
            }

            eventProducer.notifyRemoval(iri);
        }
    }
}
