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

import org.apache.cxf.dosgi.dsw.handlers.ConfigTypeHandlerFactory;
import org.apache.cxf.dosgi.dsw.handlers.ConfigurationTypeHandler;
import org.apache.cxf.dosgi.dsw.handlers.ExportResult;
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

    private final Map<Map<String, Object>, Collection<ExportRegistration>> exportedServices
        = new LinkedHashMap<Map<String, Object>, Collection<ExportRegistration>>();
    private final Map<EndpointDescription, Collection<ImportRegistrationImpl>> importedServices
        = new LinkedHashMap<EndpointDescription, Collection<ImportRegistrationImpl>>();

    private BundleContext bctx;
    private EventProducer eventProducer;

    private ConfigTypeHandlerFactory configTypeHandlerFactory;

    public RemoteServiceAdminCore(BundleContext bc, ConfigTypeHandlerFactory configTypeHandlerFactory) {
        bctx = bc;
        eventProducer = new EventProducer(bctx);
        this.configTypeHandlerFactory = configTypeHandlerFactory;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<ExportRegistration> exportService(ServiceReference serviceReference, Map additionalProperties)
        throws IllegalArgumentException, UnsupportedOperationException {

        Map<String, Object> serviceProperties = getProperties(serviceReference);
        if (additionalProperties != null) {
            OsgiUtils.overlayProperties(serviceProperties, additionalProperties);
        }

        List<String> interfaces = getInterfaces(serviceProperties);
        if (interfaces.size() == 0) {
            LOG.error("export failed: no provided service interfaces found or service_exported_interfaces is null !!");
            // TODO: publish error event ? not sure
            return Collections.emptyList();
        }

        if (isCreatedByThisRSA(serviceReference)) {
            LOG.debug("Skipping export of this service as we created it ourself as a proxy {}", interfaces);
            // TODO: publish error event ? Not sure
            return Collections.emptyList();
        }

        synchronized (exportedServices) {
            // check if it is already exported ....
            if (exportedServices.containsKey(serviceProperties)) {
                LOG.debug("already exported this service. Returning existing exportRegs {} ", interfaces);
                return copyExportRegistration(serviceProperties);
            }
            LOG.info("interfaces selected for export: " + interfaces);
            Map<String, ExportRegistrationImpl> exportRegs = new LinkedHashMap<String, ExportRegistrationImpl>(1);
            Object serviceObject = bctx.getService(serviceReference);
            BundleContext callingContext = serviceReference.getBundle().getBundleContext();
            ConfigurationTypeHandler handler = null;
            try {
                handler = configTypeHandlerFactory.getHandler(bctx, serviceProperties);
            } catch (RuntimeException e) {
                LOG.error(e.getMessage(), e);
                return Collections.emptyList();
            }

            // FIXME: move out of synchronized ... -> blocks until publication is finished
            for (String iface : interfaces) {
                LOG.info("creating server for interface " + iface);
                // this is an extra sanity check, but do we really need it now ?
                Class<?> interfaceClass = ClassUtils.getInterfaceClass(serviceObject, iface);
                if (interfaceClass != null) {
                    ExportResult exportResult = handler.createServer(serviceReference, bctx, callingContext,
                            serviceProperties, interfaceClass, serviceObject);
                    LOG.info("created server for interface " + iface);
                    EndpointDescription epd = new EndpointDescription(exportResult.getEndpointProps());
                    ExportRegistrationImpl exportRegistration = new ExportRegistrationImpl(serviceReference, epd, this);
                    if (exportResult.getException() == null) {
                        exportRegistration.startServiceTracker(bctx);
                    } else {
                        LOG.error(exportResult.getException().getMessage(), exportResult.getException());
                        exportRegistration.setException(exportResult.getException());
                    }
                    exportRegs.put(iface, exportRegistration);
                }
            }

            // enlist initial export Registrations in global list of exportRegistrations
            exportedServices.put(serviceProperties, new ArrayList<ExportRegistration>(exportRegs.values()));

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
    private List<String> getInterfaces(Map<String, Object> serviceProperties) {
        List<String> interfaces = new ArrayList<String>(1);

        // Earlier in this class we have converted objectClass from String[] to List<String>...
        @SuppressWarnings("unchecked")
        List<String> providedInterfaces = (List<String>) serviceProperties.get(org.osgi.framework.Constants.OBJECTCLASS);

        String[] allowedInterfaces
            = Utils.normalizeStringPlus(serviceProperties.get(RemoteConstants.SERVICE_EXPORTED_INTERFACES));
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

    private Map<String, Object> getProperties(ServiceReference serviceReference) {
        Map<String, Object> serviceProperties = new HashMap<String, Object>();
        for (String key : serviceReference.getPropertyKeys()) {
            Object val = serviceReference.getProperty(key);
            if (val instanceof Object[]) {
                // If we are dealing with an array, turn it into a list because otherwise the equals method on the map key won't work
                Object[] arr = (Object[]) val;
                List<Object> l = new ArrayList<Object>();
                for (Object o : arr)
                    l.add(o);

                serviceProperties.put(key, l);
            } else {
                serviceProperties.put(key, val);
            }
        }
        return serviceProperties;
    }

    private List<ExportRegistration> copyExportRegistration(Map<String, Object> serviceProperties) {
        Collection<ExportRegistration> regs = exportedServices.get(serviceProperties);

        List<EndpointDescription> copiedEndpoints = new ArrayList<EndpointDescription>();

        // / create a new list with copies of the exportRegistrations
        List<ExportRegistration> copy = new ArrayList<ExportRegistration>(regs.size());
        for (ExportRegistration exportRegistration : regs) {
            if (exportRegistration instanceof ExportRegistrationImpl) {
                ExportRegistrationImpl exportRegistrationImpl = (ExportRegistrationImpl) exportRegistration;
                EndpointDescription epd = exportRegistration.getExportReference().getExportedEndpoint();
                //create one copy for each distinct endpoint description
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


    private boolean isCreatedByThisRSA(ServiceReference sref) {
        return (sref.getBundle() != null) && sref.getBundle().equals(bctx.getBundle());
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

            ConfigurationTypeHandler handler = null;
            try {
                handler = configTypeHandlerFactory.getHandler(bctx, endpoint);
            } catch (RuntimeException e) {
                LOG.error("no handler found: " + e.getMessage(), e);
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
            if (iClass == null) {
                throw new ClassNotFoundException("Cannot load interface class");
            }

            BundleContext actualContext = bctx;
            Class<?> actualClass = requestingContext.getBundle().loadClass(interfaceName);
            if (actualClass != iClass) {
                LOG.info("Class " + interfaceName + " loaded by DSW's bundle context is not "
                             + "equal to the one loaded by the requesting bundle context, "
                             + "DSW will use the requesting bundle context to register " + "a proxy service");
                iClass = actualClass;
                actualContext = requestingContext;
            }

            EndpointDescription ed = imReg.getImportedEndpointDescription();
            /* TODO: add additional local params ... */
            Dictionary<String, Object> serviceProps = new Hashtable<String, Object>(ed.getProperties());
            serviceProps.put(RemoteConstants.SERVICE_IMPORTED, true);
            serviceProps.remove(RemoteConstants.SERVICE_EXPORTED_INTERFACES);

            // synchronized (discoveredServices) {
            ClientServiceFactory csf = new ClientServiceFactory(actualContext, iClass, ed, handler, imReg);
            imReg.setClientServiceFactory(csf);
            ServiceRegistration proxyReg = actualContext.registerService(interfaceName, csf, serviceProps);
            imReg.setImportedServiceRegistration(proxyReg);
        } catch (Exception ex) {
            if (LOG.isDebugEnabled()) {
                // Only logging at debug level as this might be written to the log at the TopologyManager
                LOG.debug("Can not proxy service with interface " + interfaceName + ": " + ex.getMessage(), ex);
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
            for (Iterator<Collection<ExportRegistration>> it = exportedServices.values().iterator(); it.hasNext(); ) {
                Collection<ExportRegistration> value = it.next();
                for (Iterator<ExportRegistration> it2 = value.iterator(); it2.hasNext(); ) {
                    ExportRegistration er = it2.next();
                    if (er.equals(eri)) {
                        eventProducer.notifyRemoval(eri);
                        it2.remove();
                        if (value.size() == 0)
                            it.remove();

                        return;
                    }
                }
            }
        }
    }

    // Remove all export registrations associated with the given bundle
    protected void removeExportRegistrations(BundleContext exportingBundleCtx) {
        Bundle exportingBundle = exportingBundleCtx.getBundle();

        // Work on a copy as the map gets modified as part of the behaviour by underlying methods
        Map<Map<String, Object>, Collection<ExportRegistration>> exportCopy
            = new HashMap<Map<String, Object>, Collection<ExportRegistration>>(exportedServices);

        for (Iterator<Map.Entry<Map<String, Object>, Collection<ExportRegistration>>> it
                = exportCopy.entrySet().iterator(); it.hasNext();) {

            Entry<Map<String, Object>, Collection<ExportRegistration>> entry = it.next();
            Bundle regBundle = null;
            Iterator<ExportRegistration> it2 = entry.getValue().iterator();
            if (it2.hasNext())
                regBundle = it2.next().getExportReference().getExportedService().getBundle();

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
            if (imRegs != null && imRegs.contains(iri)) {
                imRegs.remove(iri);
            } else {
                LOG.error("An importRegistartion was intended to be removed form internal management "
                    + "structure but couldn't be found in it !! ");
            }
            if (imRegs == null || imRegs.size() == 0) {
                importedServices.remove(iri.getImportedEndpointAlways());
            }

            eventProducer.notifyRemoval(iri);
        }
    }
}
