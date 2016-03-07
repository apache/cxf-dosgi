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

import org.apache.cxf.dosgi.dsw.api.DistributionProvider;
import org.apache.cxf.dosgi.dsw.api.Endpoint;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
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
    private final ServiceListener exportedServiceListener;
    private DistributionProvider provider;
    private BundleContext apictx;

    public RemoteServiceAdminCore(BundleContext context, BundleContext apiContext, DistributionProvider provider) {
        this.bctx = context;
        this.apictx = apiContext;
        this.eventProducer = new EventProducer(bctx);
        this.provider = provider;
        // listen for exported services being unregistered so we can close the export
        this.exportedServiceListener = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.UNREGISTERING) {
                    removeServiceExports(event.getServiceReference());
                }
            }
        };
        try {
            String filter = "(" + RemoteConstants.SERVICE_EXPORTED_INTERFACES + "=*)";
            context.addServiceListener(exportedServiceListener, filter);
        } catch (InvalidSyntaxException ise) {
            throw new RuntimeException(ise); // can never happen
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<ExportRegistration> exportService(ServiceReference serviceReference, Map additionalProperties)
        throws IllegalArgumentException, UnsupportedOperationException {
        Map<String, Object> serviceProperties = getProperties(serviceReference);
        if (additionalProperties != null) {
            overlayProperties(serviceProperties, additionalProperties);
        }
        Map<String, Object> key = makeKey(serviceProperties);

        List<String> interfaceNames = getInterfaceNames(serviceProperties);

        if (isImportedService(serviceReference)) {
            return Collections.emptyList();
        }

        List<ExportRegistration> exportRegs = getExistingAndLock(key, interfaceNames);
        if (exportRegs != null) {
            return exportRegs;
        }

        try {
            ExportRegistration exportReg = exportService(interfaceNames, serviceReference, serviceProperties);
            exportRegs = new ArrayList<>();
            exportRegs.add(exportReg);
            store(key, exportRegs);
            return exportRegs;
        } finally {
            unlock(key);
        }
    }

    private void store(Map<String, Object> key, List<ExportRegistration> exportRegs) {
        if (!exportRegs.isEmpty()) {
            // enlist initial export registrations in global list of exportRegistrations
            synchronized (exportedServices) {
                exportedServices.put(key, new ArrayList<ExportRegistration>(exportRegs));
            }
            eventProducer.publishNotification(exportRegs);
        }
    }

    private void unlock(Map<String, Object> key) {
        synchronized (exportedServices) {
            if (exportedServices.get(key) == exportInProgress) {
                exportedServices.remove(key);
            }
            exportedServices.notifyAll(); // in any case, always notify waiting threads
        }
    }

    private List<ExportRegistration> getExistingAndLock(Map<String, Object> key, List<String> interfaces) {
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
        return null;
    }

    private ExportRegistration exportService(List<String> interfaceNames,
            ServiceReference<?> serviceReference, Map<String, Object> serviceProperties) {
        LOG.info("interfaces selected for export: " + interfaceNames);
        try {
            Class<?>[] interfaces = getInterfaces(interfaceNames, serviceReference.getBundle());
            Endpoint endpoint = provider.exportService(serviceReference, serviceProperties, interfaces);
            return new ExportRegistrationImpl(serviceReference, endpoint, this);
        } catch (Exception e) {
            return new ExportRegistrationImpl(this, e);
        }
    }
    
    private Class<?>[] getInterfaces(List<String> interfaceNames, 
                                         Bundle serviceBundle) throws ClassNotFoundException {
        List<Class<?>> interfaces = new ArrayList<>();
        for (String interfaceName : interfaceNames) {
            interfaces.add(serviceBundle.loadClass(interfaceName));
        }
        return interfaces.toArray(new Class[]{});
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
    private List<String> getInterfaceNames(Map<String, Object> serviceProperties) {
        String[] providedInterfaces = (String[])serviceProperties.get(org.osgi.framework.Constants.OBJECTCLASS);
        if (providedInterfaces == null || providedInterfaces.length == 0) {
            throw new IllegalArgumentException("service is missing the objectClass property");
        }

        String[] exportedInterfaces
            = StringPlus.normalize(serviceProperties.get(RemoteConstants.SERVICE_EXPORTED_INTERFACES));
        if (exportedInterfaces == null || exportedInterfaces.length == 0) {
            throw new IllegalArgumentException("service is missing the service.exported.interfaces property");
        }

        List<String> interfaces = new ArrayList<String>(1);
        if (exportedInterfaces.length == 1 && "*".equals(exportedInterfaces[0])) {
            // FIXME: according to the spec, this should only return the interfaces, and not
            // non-interface classes (which are valid OBJECTCLASS values, even if discouraged)
            Collections.addAll(interfaces, providedInterfaces);
        } else {
            List<String> providedList = Arrays.asList(providedInterfaces);
            List<String> allowedList = Arrays.asList(exportedInterfaces);
            if (!providedList.containsAll(allowedList)) {
                throw new IllegalArgumentException(String.format(
                    "exported interfaces %s must be a subset of the service's registered types %s",
                    allowedList, providedList));
            }

            Collections.addAll(interfaces, exportedInterfaces);
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

    private boolean isImportedService(ServiceReference<?> sref) {
        return sref.getProperty(RemoteConstants.SERVICE_IMPORTED) != null;
    }

    @Override
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

    @Override
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
    @Override
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

            if (determineConfigTypesForImport(endpoint).size() == 0) {
                LOG.info("No matching handler can be found for remote endpoint {}.", endpoint.getId());
                return null;
            }
            
            // TODO: somehow select the interfaces that should be imported ---> job of the TopologyManager?
            List<String> matchingInterfaces = endpoint.getInterfaces();
            
            if (matchingInterfaces.size() == 0) {
                LOG.info("No matching interfaces found for remote endpoint {}.", endpoint.getId());
                return null;
            }
            
            LOG.info("Importing service {} with interfaces {} using handler {}.", 
                     endpoint.getId(), endpoint.getInterfaces(), provider.getClass());

            ImportRegistrationImpl imReg = exposeServiceFactory(matchingInterfaces.get(0), endpoint, provider);
            if (imRegs == null) {
                imRegs = new ArrayList<ImportRegistrationImpl>();
                importedServices.put(endpoint, imRegs);
            }
            imRegs.add(imReg);
            eventProducer.publishNotification(imReg);
            return imReg;
        }
    }
    
    private List<String> determineConfigTypesForImport(EndpointDescription endpoint) {
        List<String> remoteConfigurationTypes = endpoint.getConfigurationTypes();

        List<String> usableConfigurationTypes = new ArrayList<String>();
        for (String ct : provider.getSupportedTypes()) {
            if (remoteConfigurationTypes.contains(ct)) {
                usableConfigurationTypes.add(ct);
            }
        }

        LOG.info("Ignoring endpoint {} as it has no compatible configuration types: {}.", 
                 endpoint.getId(), remoteConfigurationTypes);
        return usableConfigurationTypes;
    }

    protected ImportRegistrationImpl exposeServiceFactory(String interfaceName,
                                            EndpointDescription epd,
                                            DistributionProvider handler) {
        ImportRegistrationImpl imReg = new ImportRegistrationImpl(epd, this);
        try {
            EndpointDescription endpoint = imReg.getImportedEndpointDescription();
            Dictionary<String, Object> serviceProps = new Hashtable<String, Object>(endpoint.getProperties());
            serviceProps.put(RemoteConstants.SERVICE_IMPORTED, true);
            serviceProps.remove(RemoteConstants.SERVICE_EXPORTED_INTERFACES);

            ClientServiceFactory csf = new ClientServiceFactory(endpoint, handler, imReg);
            imReg.setClientServiceFactory(csf);
            
            /**
             *  Export the factory using the api context as it has very few imports.
             *  If the bundle publishing the factory does not import the service interface
             *  package then the factory is visible for all consumers which we want.
             */
            ServiceRegistration<?> csfReg = apictx.registerService(interfaceName, csf, serviceProps);
            imReg.setImportedServiceRegistration(csfReg);
        } catch (Exception ex) {
            // Only logging at debug level as this might be written to the log at the TopologyManager
            LOG.debug("Can not proxy service with interface " + interfaceName + ": " + ex.getMessage(), ex);
            imReg.setException(ex);
        }
        return imReg;
    }

    /**
     * Removes and closes all exports for the given service.
     * This is called when the service is unregistered.
     *
     * @param sref the service whose exports should be removed and closed
     */
    protected void removeServiceExports(ServiceReference<?> sref) {
        List<ExportRegistration> regs = new ArrayList<ExportRegistration>(1);
        synchronized (exportedServices) {
            for (Iterator<Collection<ExportRegistration>> it = exportedServices.values().iterator(); it.hasNext();) {
                Collection<ExportRegistration> value = it.next();
                for (Iterator<ExportRegistration> it2 = value.iterator(); it2.hasNext();) {
                    ExportRegistration er = it2.next();
                    if (er.getExportReference().getExportedService().equals(sref)) {
                        regs.add(er);
                    }
                }
            }
            // do this outside of iteration to avoid concurrent modification
            for (ExportRegistration er : regs) {
                LOG.debug("closing export for service {}", sref);
                er.close();
            }
        }

    }

    /**
     * Removes the provided Export Registration from the internal management structures.
     * This is called from the ExportRegistration itself when it is closed (so should
     * not attempt to close it again here).
     *
     * @param eri the export registration to remove
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

    // remove all import registrations
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
                    ExportRegistration exportRegistration = regs.iterator().next();
                    if (exportRegistration.getException() == null) {
                        Bundle regBundle = exportRegistration.getExportReference().getExportedService().getBundle();
                        if (exportingBundle.equals(regBundle)) {
                            bundleRegs.addAll(regs);
                        }
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
                eventProducer.notifyRemoval(iri);
            }
            if (imRegs == null || imRegs.isEmpty()) {
                importedServices.remove(iri.getImportedEndpointAlways());
            }
        }
    }

    public void close() {
        removeImportRegistrations();
        bctx.removeServiceListener(exportedServiceListener);
    }

    static void overlayProperties(Map<String, Object> serviceProperties,
                                         Map<String, Object> additionalProperties) {
        Map<String, String> keysLowerCase = new HashMap<String, String>();
        for (String key : serviceProperties.keySet()) {
            keysLowerCase.put(key.toLowerCase(), key);
        }
    
        for (Map.Entry<String, Object> e : additionalProperties.entrySet()) {
            String key = e.getKey();
            String lowerKey = key.toLowerCase();
            if (org.osgi.framework.Constants.SERVICE_ID.toLowerCase().equals(lowerKey)
                || org.osgi.framework.Constants.OBJECTCLASS.toLowerCase().equals(lowerKey)) {
                // objectClass and service.id must not be overwritten
                LOG.info("exportService called with additional properties map that contained illegal key: "
                          + key + ", the key is ignored");
            } else {
                String origKey = keysLowerCase.get(lowerKey);
                if (origKey != null) {
                    LOG.debug("Overwriting property [{}] with value [{}]", origKey, e.getValue());
                } else {
                    origKey = key;
                    keysLowerCase.put(lowerKey, origKey);
                }
                serviceProperties.put(origKey, e.getValue());
            }
        }
    }

    /**
     * Returns a service's properties as a map.
     *
     * @param serviceReference a service reference
     * @return the service's properties as a map
     */
    private Map<String, Object> getProperties(ServiceReference<?> serviceReference) {
        String[] keys = serviceReference.getPropertyKeys();
        Map<String, Object> props = new HashMap<String, Object>(keys.length);
        for (String key : keys) {
            Object val = serviceReference.getProperty(key);
            props.put(key, val);
        }
        return props;
    }
}
