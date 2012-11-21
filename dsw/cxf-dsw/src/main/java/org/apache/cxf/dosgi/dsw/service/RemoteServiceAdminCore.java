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

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.dosgi.dsw.handlers.ConfigTypeHandlerFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.handlers.ExportResult;
import org.apache.cxf.dosgi.dsw.qos.IntentManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteServiceAdminCore implements RemoteServiceAdmin {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteServiceAdminCore.class);

    private final LinkedHashMap<ServiceReference, Collection<ExportRegistration>> exportedServices = new LinkedHashMap<ServiceReference, Collection<ExportRegistration>>();
    private final LinkedHashMap<EndpointDescription, Collection<ImportRegistrationImpl>> importedServices = new LinkedHashMap<EndpointDescription, Collection<ImportRegistrationImpl>>();

    private BundleContext bctx;
    private EventProducer eventProducer;

    private volatile boolean useMasterMap = true;
    private volatile String defaultPort;
    private volatile String defaultHost;

    private ConfigTypeHandlerFactory configTypeHandlerFactory;
    private IntentManager intentManager;

    // protected because of tests
    protected static final List<String> supportedConfigurationTypes = new ArrayList<String>();

    static {
        supportedConfigurationTypes.add(Constants.WSDL_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.RS_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.WS_CONFIG_TYPE);
        supportedConfigurationTypes.add(Constants.WS_CONFIG_TYPE_OLD);
    }

    protected final static String DEFAULT_CONFIGURATION = Constants.WS_CONFIG_TYPE;

    public RemoteServiceAdminCore(BundleContext bc, IntentManager intentManager) {
        bctx = bc;
        this.intentManager = intentManager;
        eventProducer = new EventProducer(bctx);
        this.configTypeHandlerFactory = new ConfigTypeHandlerFactory(intentManager);
    }

    @SuppressWarnings("rawtypes")
	public List<ExportRegistration> exportService(ServiceReference serviceReference, Map additionalProperties)
        throws IllegalArgumentException, UnsupportedOperationException {

        String ifaceName = serviceReference.getClass().getName();
        LOG.debug("RemoteServiceAdmin: exportService: {}", ifaceName);

        synchronized (exportedServices) {
            // check if it is already exported ....
            if (exportedServices.containsKey(serviceReference)) {
                return copyExportRegistration(serviceReference, ifaceName);
            }

            if (isCreatedByThisRSA(serviceReference)) {
                LOG.debug("proxy provided by this bundle ...  {} ", ifaceName);
                // TODO: publish error event ? Not sure
                return Collections.emptyList();
            }

            Properties serviceProperties = getProperties(serviceReference);
            if (additionalProperties != null) {
                OsgiUtils.overlayProperties(serviceProperties, additionalProperties);
            }

            List<String> unsupportedIntents = intentManager.getUnsupportedIntents(serviceProperties);
            if (unsupportedIntents.size() > 0) {
                LOG.error("service cannot be exported because the following intents are not supported by this RSA: "
                            + unsupportedIntents);
                // TODO: publish error event
                return Collections.emptyList();
            }

            List<String> interfaces = getInterfaces(serviceProperties);
            if (interfaces.size() == 0) {
                LOG.error("export failed: no provided service interfaces found or service_exported_interfaces is null !!");
                // TODO: publish error event ? not sure
                return Collections.emptyList();
            }
            LOG.info("interfaces selected for export: " + interfaces);

            List<String> configurationTypes = determineConfigurationTypes(serviceProperties);
            LOG.info("configuration types selected for export: " + configurationTypes);
            if (configurationTypes.size() == 0) {
                LOG.info("the requested configuration types are not supported");
                // TODO: publish error event ? not sure
                return Collections.emptyList();
            }

            LinkedHashMap<String, ExportRegistrationImpl> exportRegs = new LinkedHashMap<String, ExportRegistrationImpl>(1);
            Object serviceObject = bctx.getService(serviceReference);
            BundleContext callingContext = serviceReference.getBundle().getBundleContext();
            ConfigurationTypeHandler handler = getHandler(configurationTypes, serviceProperties, getHandlerProperties());
            if (handler == null) {
                // TODO: publish error event ? not sure
                return Collections.emptyList();
            }

            // FIXME: move out of synchronized ... -> blocks until publication is finished
            for (String iface : interfaces) {
                LOG.info("creating server for interface " + iface + " with configurationTypes " + configurationTypes);
                // this is an extra sanity check, but do we really need it now ?
                Class<?> interfaceClass = ClassUtils.getInterfaceClass(serviceObject, iface);
                if (interfaceClass != null) {
                    ExportResult exportResult = handler.createServer(serviceReference, bctx, callingContext,
                            serviceProperties, interfaceClass, serviceObject);
                    LOG.info("created server for interface " + iface);
                    EndpointDescription epd = new EndpointDescription(exportResult.getEndpointProps());
                    ExportRegistrationImpl exportRegistration = new ExportRegistrationImpl(serviceReference, epd, this);
                    if (exportRegistration.getException() == null) {
                        exportRegistration.startServiceTracker(bctx);
                    }
                    exportRegs.put(iface, exportRegistration);
                }
            }

            // enlist initial export Registrations in global list of exportRegistrations
            exportedServices.put(serviceReference, new ArrayList<ExportRegistration>(exportRegs.values()));

            List<ExportRegistration> lExpReg = new ArrayList<ExportRegistration>(exportRegs.values());
            eventProducer.publishNotifcation(lExpReg);

            return lExpReg;
        }
    }

    /**
     * Determine which interfaces should be exported
     * 
     * @param serviceProperties 
     * @return interfaces to be exported
     */
    private List<String> getInterfaces(Properties serviceProperties) {
        List<String> interfaces = new ArrayList<String>(1);
        
        String[] providedInterfaces = (String[]) serviceProperties.get(org.osgi.framework.Constants.OBJECTCLASS);
        String[] allowedInterfaces = Utils.normalizeStringPlus(serviceProperties.get(RemoteConstants.SERVICE_EXPORTED_INTERFACES));
        if (providedInterfaces == null || allowedInterfaces == null) {
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
        return interfaces;
    }

    private Properties getProperties(ServiceReference serviceReference) {
        Properties serviceProperties = new Properties();
        for (String key : serviceReference.getPropertyKeys()) {
            serviceProperties.put(key, serviceReference.getProperty(key));
        }
        return serviceProperties;
    }

    private List<ExportRegistration> copyExportRegistration(ServiceReference serviceReference, String ifaceName) {
        LOG.debug("already exported ...  {} ", ifaceName);
        Collection<ExportRegistration> regs = exportedServices.get(serviceReference);

        List<EndpointDescription> copiedEndpoints = new ArrayList<EndpointDescription>();

        // / create a new list with copies of the exportRegistrations
        List<ExportRegistration> copy = new ArrayList<ExportRegistration>(regs.size());
        for (ExportRegistration exportRegistration : regs) {
        	if (exportRegistration instanceof ExportRegistrationImpl) {
        		ExportRegistrationImpl exportRegistrationImpl = (ExportRegistrationImpl) exportRegistration;
        		EndpointDescription epd = exportRegistration.getExportReference().getExportedEndpoint();
        		// create one copy for each distinct endpoint description
        		if (!copiedEndpoints.contains(epd)) {
        			copiedEndpoints.add(epd);
        			copy.add(new ExportRegistrationImpl(exportRegistrationImpl));
        		}
        	}
        }

        regs.addAll(copy);

        eventProducer.publishNotifcation(copy);
        return copy;
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
                    ers.add(new ExportReferenceImpl(er.getExportReference()));
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
        return configTypeHandlerFactory.getHandler(bctx, configurationTypes, serviceProperties,
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

        LOG.debug("importService() Endpoint: {}", endpoint.getProperties());

        synchronized (importedServices) {
            if (importedServices.containsKey(endpoint) && importedServices.get(endpoint).size() > 0) {
                LOG.debug("creating copy of existing import registrations");
                Collection<ImportRegistrationImpl> imRegs = importedServices.get(endpoint);
                ImportRegistrationImpl irParent = imRegs.iterator().next();
                ImportRegistrationImpl ir = new ImportRegistrationImpl(irParent);
                imRegs.add(ir);
                eventProducer.publishNotifcation(ir);
                return ir;
            }

            List<String> remoteConfigurationTypes = endpoint.getConfigurationTypes();

            if (remoteConfigurationTypes == null) {
                LOG.error("the supplied endpoint has no configuration type");
                return null;
            }

            List<String> usableConfigurationTypes = new ArrayList<String>();
            for (String ct : supportedConfigurationTypes) {
                if (remoteConfigurationTypes.contains(ct)) {
                    usableConfigurationTypes.add(ct);
                }
            }

            if (usableConfigurationTypes.size() == 0) {
                LOG.error("the supplied endpoint has no compatible configuration type. Supported types are: "
                            + supportedConfigurationTypes
                            + "    Types needed by the endpoint: "
                            + remoteConfigurationTypes);
                return null;
            }

            Map<String, Object> emptyProps = Collections.emptyMap();
            ConfigurationTypeHandler handler = getHandler(usableConfigurationTypes, endpoint.getProperties(),
                                                          emptyProps);

            if (handler == null) {
                LOG.error("no handler found");
                return null;
            }

            LOG.debug("Handler: {}", handler);

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
            if (LOG.isDebugEnabled()) {
                // Only logging at debug level as this might be written to the log at the TopologyManager
                LOG.debug("No class can be found for " + interfaceName, ex);
            }
            imReg.setException(ex);
        }
    }

    /**
     * Removes the provided Export Registration from the internal management structures -> intended to be used
     * when the export Registration is closed
     */
    protected void removeExportRegistration(ExportRegistrationImpl eri) {
        synchronized (exportedServices) {
            ServiceReference sref = eri.getExportReference().getExportedService();
            Collection<ExportRegistration> exRegs = exportedServices.get(sref);
            if (exRegs != null && exRegs.contains(eri)) {
                eventProducer.notifyRemoval(eri);
                exRegs.remove(eri);
            } else {
                LOG.error("An exportRegistartion was intended to be removed form internal management structure but couldn't be found in it !! ");
            }
            if (exRegs == null || exRegs.size() == 0) {
                exportedServices.remove(sref);
            }
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
            LOG.debug("Removing importRegistration {}", iri);

            Collection<ImportRegistrationImpl> imRegs = importedServices.get(iri.getImportedEndpointAlways());
            if (imRegs!=null && imRegs.contains(iri)) {
                imRegs.remove(iri);
            } else {
                LOG.error("An importRegistartion was intended to be removed form internal management structure but couldn't be found in it !! ");
            }
            if (imRegs == null || imRegs.size() == 0) {
                importedServices.remove(iri.getImportedEndpointAlways());
            }

            eventProducer.notifyRemoval(iri);
        }
    }
}
