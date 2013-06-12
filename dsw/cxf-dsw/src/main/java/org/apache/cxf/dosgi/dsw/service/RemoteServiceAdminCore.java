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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // Is stored in exportedServices while the export is in progress as a marker
    private final List<ExportRegistration> exportInProgress = Collections.emptyList();

    private final BundleContext bctx;
    private final EventProducer eventProducer;
    private final ConfigTypeHandlerFactory configTypeHandlerFactory;

    public RemoteServiceAdminCore(BundleContext bc, ConfigTypeHandlerFactory configTypeHandlerFactory) {
        this.bctx = bc;
        this.eventProducer = new EventProducer(bctx);
        this.configTypeHandlerFactory = configTypeHandlerFactory;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<ExportRegistration> exportService(ServiceReference serviceReference, Map additionalProperties)
        throws IllegalArgumentException, UnsupportedOperationException {
        Map<String, Object> serviceProperties = OsgiUtils.getProperties(serviceReference);
        if (additionalProperties != null) {
            OsgiUtils.overlayProperties(serviceProperties, additionalProperties);
        }
        Map<String, Object> key = makeKey(serviceProperties);

        List<String> interfaces = getInterfaces(serviceProperties);

        if (isCreatedByThisRSA(serviceReference)) {
            LOG.debug("Skipping export of this service as we created it ourselves as a proxy {}", interfaces);
            // TODO: publish error event? Not sure
            return Collections.emptyList();
        }

        synchronized (exportedServices) {
            // check if it is already exported...
            Collection<ExportRegistration> existingRegs = exportedServices.get(key);

            // if the export is already in progress, wait for it to be complete
            while (existingRegs == exportInProgress) {
                try {
                    exportedServices.wait();
                    existingRegs = exportedServices.get(key);
                } catch (InterruptedException ie) {
                    LOG.debug("interrupted while waiting for export in progress");
                    return Collections.emptyList();
                }
            }

            // if the export is complete, return a copy of existing export
            if (existingRegs != null) {
                LOG.debug("already exported this service. Returning existing exportRegs {} ", interfaces);
                return copyExportRegistration(existingRegs);
            }

            // mark export as being in progress
            exportedServices.put(key, exportInProgress);
        }

        try {
            // do the export
            List<ExportRegistration> exportRegs = exportInterfaces(interfaces, serviceReference, serviceProperties);
            if (!exportRegs.isEmpty()) {
                // enlist initial export registrations in global list of exportRegistrations
                synchronized (exportedServices) {
                    exportedServices.put(key, new ArrayList<ExportRegistration>(exportRegs));
                }
                eventProducer.publishNotification(exportRegs);
            }
            return exportRegs;
        } finally {
            synchronized (exportedServices) {
                if (exportedServices.get(key) == exportInProgress) {
                    exportedServices.remove(key);
                }
                exportedServices.notifyAll(); // in any case, always notify waiting threads
            }
        }
    }

    private List<ExportRegistration> exportInterfaces(List<String> interfaces,
            ServiceReference serviceReference, Map<String, Object> serviceProperties) {
        LOG.info("interfaces selected for export: " + interfaces);
        ConfigurationTypeHandler handler;
        try {
            handler = configTypeHandlerFactory.getHandler(bctx, serviceProperties);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            return Collections.emptyList();
        }
        List<ExportRegistration> exportRegs = new ArrayList<ExportRegistration>(1);
        Object service = bctx.getService(serviceReference);
        Bundle bundle = serviceReference.getBundle();

        // if service has been unregistered in the meantime
        if (service == null || bundle == null) {
            LOG.info("service has been unregistered, aborting export");
            return exportRegs;
        }

        for (String iface : interfaces) {
            LOG.info("creating server for interface " + iface);
            // this is an extra sanity check, but do we really need it now?
            Class<?> interfaceClass = ClassUtils.getInterfaceClass(service, iface);
            if (interfaceClass != null) {
                ExportResult exportResult = handler.createServer(serviceReference, bctx, bundle.getBundleContext(),
                    serviceProperties, interfaceClass, service);
                LOG.info("created server for interface " + iface);
                EndpointDescription epd = new EndpointDescription(exportResult.getEndpointProps());
                ExportRegistrationImpl exportRegistration = new ExportRegistrationImpl(serviceReference, epd, this);
                if (exportResult.getException() == null) {
                    exportRegistration.setServer(exportResult.getServer());
                    exportRegistration.startServiceTracker(bctx);
                } else {
                    LOG.error(exportResult.getException().getMessage(), exportResult.getException());
                    exportRegistration.setException(exportResult.getException());
                }
                exportRegs.add(exportRegistration);
            }
        }
        return exportRegs;
    }

    /**
     * Determines which interfaces should be exported.
     *
     * @param serviceProperties the exported service properties
     * @return the interfaces to be exported
     * @throws IllegalArgumentException if the service parameters are invalid
     * @see RemoteServiceAdmin#exportService
     * @see org.osgi.framework.Constants#OBJECTCLASS
     * @see RemoteConstants#SERVICE_EXPORTED_INTERFACES
     */
    private List<String> getInterfaces(Map<String, Object> serviceProperties) {
        String[] providedInterfaces = (String[])serviceProperties.get(org.osgi.framework.Constants.OBJECTCLASS);
        if (providedInterfaces == null || providedInterfaces.length == 0) {
            throw new IllegalArgumentException("service is missing the objectClass property");
        }

        String[] allowedInterfaces
            = Utils.normalizeStringPlus(serviceProperties.get(RemoteConstants.SERVICE_EXPORTED_INTERFACES));
        if (allowedInterfaces == null || allowedInterfaces.length == 0) {
            throw new IllegalArgumentException("service is missing the service.exported.interfaces property");
        }

        List<String> interfaces = new ArrayList<String>(1);
        if (allowedInterfaces.length == 1 && "*".equals(allowedInterfaces[0])) {
            // FIXME: according to the spec, this should only return the interfaces, and not
            // non-interface classes (which are valid OBJECTCLASS values, even if discouraged)
            Collections.addAll(interfaces, providedInterfaces);
        } else {
            List<String> providedList = Arrays.asList(providedInterfaces);
            List<String> allowedList = Arrays.asList(allowedInterfaces);
            if (!providedList.containsAll(allowedList)) {
                throw new IllegalArgumentException(String.format(
                    "exported interfaces %s must be a subset of the service's registered types %s",
                    allowedList, providedList));
            }

            Collections.addAll(interfaces, allowedInterfaces);
        }
        return interfaces;
    }

    /**
     * Converts the given properties map into one that can be used as a map key itself.
     * For example, if a value is an array, it is converted into a list so that the
     * equals method will compare it properly.
     *
     * @param properties a properties map
     * @return a map that represents the given map, but can be safely used as a map key itself
     */
    private Map<String, Object> makeKey(Map<String, Object> properties) {
        // FIXME: we should also make logically equal values actually compare as equal
        // (e.g. String+ values should be normalized)
        Map<String, Object> converted = new HashMap<String, Object>(properties.size());
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object val = entry.getValue();
            // convert arrays into lists so that they can be compared via equals()
            if (val instanceof Object[]) {
                val = Arrays.asList((Object[])val);
            }
            converted.put(entry.getKey(), val);
        }
        return converted;
    }

    private List<ExportRegistration> copyExportRegistration(Collection<ExportRegistration> regs) {
        Set<EndpointDescription> copiedEndpoints = new HashSet<EndpointDescription>();

        // create a new list with copies of the exportRegistrations
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

        eventProducer.publishNotification(copy);
        return copy;
    }

    private boolean isCreatedByThisRSA(ServiceReference sref) {
        return bctx.getBundle().equals(sref.getBundle()); // sref bundle can be null
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
     * Importing form here...
     */
    public ImportRegistration importService(EndpointDescription endpoint) {
        LOG.debug("importService() Endpoint: {}", endpoint.getProperties());

        synchronized (importedServices) {
            Collection<ImportRegistrationImpl> imRegs = importedServices.get(endpoint);
            if (imRegs != null && !imRegs.isEmpty()) {
                LOG.debug("creating copy of existing import registrations");
                ImportRegistrationImpl irParent = imRegs.iterator().next();
                ImportRegistrationImpl ir = new ImportRegistrationImpl(irParent);
                imRegs.add(ir);
                eventProducer.publishNotification(ir);
                return ir;
            }

            ConfigurationTypeHandler handler;
            try {
                handler = configTypeHandlerFactory.getHandler(bctx, endpoint);
            } catch (RuntimeException e) {
                LOG.error("no handler found: " + e.getMessage(), e);
                return null;
            }

            LOG.debug("Handler: {}", handler);

            // TODO: somehow select the interfaces that should be imported ---> job of the TopologyManager?
            List<String> matchingInterfaces = endpoint.getInterfaces();

            LOG.info("Matching Interfaces for import: " + matchingInterfaces);

            if (matchingInterfaces.size() == 1) {
                LOG.info("Proxifying interface: " + matchingInterfaces.get(0));

                ImportRegistrationImpl imReg = new ImportRegistrationImpl(endpoint, this);

                proxifyMatchingInterface(matchingInterfaces.get(0), imReg, handler, bctx);
                if (imRegs == null) {
                    imRegs = new ArrayList<ImportRegistrationImpl>();
                    importedServices.put(endpoint, imRegs);
                }
                imRegs.add(imReg);
                eventProducer.publishNotification(imReg);
                return imReg;
            } else {
                return null;
            }
        }
    }

    protected void proxifyMatchingInterface(String interfaceName, ImportRegistrationImpl imReg,
                                            ConfigurationTypeHandler handler, BundleContext requestingContext) {
        try {
            // MARC: relies on dynamic imports?
            Class<?> iClass = bctx.getBundle().loadClass(interfaceName);
            if (iClass == null) {
                throw new ClassNotFoundException("Cannot load interface class");
            }

            BundleContext actualContext = bctx;
            Class<?> actualClass = requestingContext.getBundle().loadClass(interfaceName);
            if (actualClass != iClass) {
                LOG.info("Class " + interfaceName + " loaded by DSW's bundle context is not "
                             + "equal to the one loaded by the requesting bundle context, "
                             + "DSW will use the requesting bundle context to register a proxy service");
                iClass = actualClass;
                actualContext = requestingContext;
            }

            EndpointDescription ed = imReg.getImportedEndpointDescription();
            /* TODO: add additional local params... */
            Dictionary<String, Object> serviceProps = new Hashtable<String, Object>(ed.getProperties());
            serviceProps.put(RemoteConstants.SERVICE_IMPORTED, true);
            serviceProps.remove(RemoteConstants.SERVICE_EXPORTED_INTERFACES);

            // synchronized (discoveredServices) {
            ClientServiceFactory csf = new ClientServiceFactory(actualContext, iClass, ed, handler, imReg);
            imReg.setClientServiceFactory(csf);
            ServiceRegistration proxyReg = actualContext.registerService(interfaceName, csf, serviceProps);
            imReg.setImportedServiceRegistration(proxyReg);
        } catch (Exception ex) {
            // Only logging at debug level as this might be written to the log at the TopologyManager
            LOG.debug("Can not proxy service with interface " + interfaceName + ": " + ex.getMessage(), ex);
            imReg.setException(ex);
        }
    }

    /**
     * Removes the provided Export Registration from the internal management structures -> intended to be used
     * when the export Registration is closed
     */
    protected void removeExportRegistration(ExportRegistrationImpl eri) {
        synchronized (exportedServices) {
            for (Iterator<Collection<ExportRegistration>> it = exportedServices.values().iterator(); it.hasNext();) {
                Collection<ExportRegistration> value = it.next();
                for (Iterator<ExportRegistration> it2 = value.iterator(); it2.hasNext();) {
                    ExportRegistration er = it2.next();
                    if (er.equals(eri)) {
                        eventProducer.notifyRemoval(eri);
                        it2.remove();
                        if (value.isEmpty()) {
                            it.remove();
                        }
                        return;
                    }
                }
            }
        }
    }

    // remove all export registrations associated with the given bundle
    protected void removeExportRegistrations(Bundle exportingBundle) {
        List<ExportRegistration> bundleExports = getExportsForBundle(exportingBundle);
        for (ExportRegistration export : bundleExports) {
            export.close();
        }
    }

    // remove all import registrations associated with the given bundle
    protected void removeImportRegistrations() {
        Collection<ImportRegistrationImpl> copy = new ArrayList<ImportRegistrationImpl>();
        synchronized (importedServices) {
            for (Collection<ImportRegistrationImpl> irs : importedServices.values()) {
                copy.addAll(irs);
            }
        }
        for (ImportRegistrationImpl ir : copy) {
            removeImportRegistration(ir);
        }
    }

    private List<ExportRegistration> getExportsForBundle(Bundle exportingBundle) {
        synchronized (exportedServices) {
            List<ExportRegistration> bundleRegs = new ArrayList<ExportRegistration>();
            for (Collection<ExportRegistration> regs : exportedServices.values()) {
                if (!regs.isEmpty()) {
                    Bundle regBundle = regs.iterator().next().getExportReference().getExportedService().getBundle();
                    if (exportingBundle.equals(regBundle)) {
                        bundleRegs.addAll(regs);
                    }
                }
            }
            return bundleRegs;
        }
    }

    protected void removeImportRegistration(ImportRegistrationImpl iri) {
        synchronized (importedServices) {
            LOG.debug("Removing importRegistration {}", iri);

            Collection<ImportRegistrationImpl> imRegs = importedServices.get(iri.getImportedEndpointAlways());
            if (imRegs != null && imRegs.contains(iri)) {
                imRegs.remove(iri);
            } else {
                LOG.error("An importRegistration was intended to be removed from internal management "
                    + "structure but couldn't be found in it!");
            }
            if (imRegs == null || imRegs.isEmpty()) {
                importedServices.remove(iri.getImportedEndpointAlways());
            }

            eventProducer.notifyRemoval(iri);
        }
    }
}
