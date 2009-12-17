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
package org.apache.cxf.dosgi.topologymanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class TopologyManagerImport {

    private final static Logger LOG = Logger.getLogger(TopologyManagerImport.class.getName());
    private ExecutorService execService = new ThreadPoolExecutor(5, 10, 50, TimeUnit.SECONDS,
                                                                 new LinkedBlockingQueue<Runnable>());

    private EndpointListenerImpl endpointListener;
    private BundleContext bctx;
    private RemoteServiceAdminList remoteServiceAdminList;
    private ListenerHookImpl listenerHook;

    /**
     * If set to false only one service is imported for each import interest even it multiple services are
     * available. If set to true, all available services are imported.
     * 
     * TODO: Make this available as a configuration option
     */
    private boolean importAllAvailable = true;

    /**
     * Contains an instance of the Class Import Interest for each distinct import request. If the same filter
     * is requested multiple times the existing instance of the Object increments an internal reference
     * counter. If an interest is removed, the related ServiceInterest object is used to reduce the reference
     * counter until it reaches zero. in this case the interest is removed.
     */
    private Map<String/* filter */, ImportInterest> importInterests = new HashMap<String, ImportInterest>();

    private static class ImportInterest {
        String filter;
        int refs;

        public ImportInterest(String filter) {
            this.filter = filter;
            refs = 1;
        }

        public int addReference() {
            return ++refs;
        }

        public int removeReference() {
            return --refs;
        }

    }

    /**
     * FIXME: Documnet me .... !
     */
    private Map<String /* filter */, List<EndpointDescription>> importPossibilities = new HashMap<String, List<EndpointDescription>>();
    private Map<String /* filter */, List<ImportRegistration>> importedServices = new HashMap<String, List<ImportRegistration>>();

    public TopologyManagerImport(BundleContext bc, RemoteServiceAdminList rsaList) {
        bctx = bc;
        remoteServiceAdminList = rsaList;
        endpointListener = new EndpointListenerImpl(bctx, this);
        listenerHook = new ListenerHookImpl(bctx, this);
    }

    public void start() {
        // / register the EndpointListener for discovery
        endpointListener.start();
        listenerHook.start();
    }

    public void stop() {
        execService.shutdown();
        endpointListener.stop();
        listenerHook.stop();
    }

    protected void addServiceInterest(String filter) {

        String exFilter = Utils.extendFilter(filter, bctx);

        synchronized (importInterests) {
            ImportInterest i = importInterests.get(exFilter);
            if (i != null) {
                i.addReference();
            } else {
                importInterests.put(exFilter, new ImportInterest(exFilter));
                endpointListener.extendScope(exFilter);
            }
        }

    }

    public void removeServiceInterest(String filter) {

        String exFilter = Utils.extendFilter(filter, bctx);

        synchronized (importInterests) {
            ImportInterest i = importInterests.get(exFilter);
            if (i != null) {
                // remove reference
                if (i.removeReference() <= 0) {
                    // last reference, remove from scope
                    LOG.fine("last reference to import interest is gone -> removing interest  filter:"
                             + exFilter);
                    endpointListener.reduceScope(exFilter);
                    importInterests.remove(exFilter);
                    List<ImportRegistration> irs = importedServices.remove(exFilter);
                    for (ImportRegistration ir : irs) {
                        if (ir != null) {
                            ir.close();
                        }
                    }
                }
            } else {
                // unhandled service ... do nothing
            }
        }

    }

    public void removeImportableService(String filter, EndpointDescription epd) {

        synchronized (importPossibilities) {
            List<EndpointDescription> ips = importPossibilities.get(filter);
            if (ips != null) {
                ips.remove(epd);
            } else {
                // should not happen
            }
        }

        triggerImport(filter);

    }

    public void addImportableService(String filter, EndpointDescription epd) {

        LOG.fine("importable service added for filter " + filter + " -> " + epd);
        synchronized (importPossibilities) {
            List<EndpointDescription> ips = importPossibilities.get(filter);
            if (ips == null) {
                ips = new ArrayList<EndpointDescription>();
                importPossibilities.put(filter, ips);
            }

            ips.add(epd);
        }

        triggerImport(filter);
    }

    private void triggerImport(final String filter) {

        LOG.fine("import of a service for filter " + filter + " was queued");

        execService.execute(new Runnable() {
            public void run() {
                synchronized (importedServices) { // deadlock possibility ?
                    synchronized (importPossibilities) {
                        if (importAllAvailable) {
                            importAllServicesStrategy(filter);
                        } else {
                            importSingleServiceStrategy(filter);
                        }
                    }
                }
                // Notify EndpointListeners ? NO!
            }

        });

    }

    private void importAllServicesStrategy(String filter) {

        List<ImportRegistration> irs = importedServices.get(filter);
        if (irs == null) {
            irs = new ArrayList<ImportRegistration>();
            importedServices.put(filter, irs);
        }

        if (irs.size() > 0) { // remove old services that are not available anymore
            List<EndpointDescription> ips = importPossibilities.get(filter);
            Iterator<ImportRegistration> it = irs.iterator();
            while (it.hasNext()) {
                ImportRegistration ir = it.next();
                EndpointDescription ep = ir.getImportReference().getImportedEndpoint();

                // if service is already imported, check if endpoint is still in the list of
                // possible imports
                if ((ips != null && !ips.contains(ep)) || ips == null) {
                    // unexport service
                    ir.close();
                    it.remove();
                }

            }
        }

        for (EndpointDescription epd : importPossibilities.get(filter)) {
            if (!irs.contains(epd)) {
                // service not imported yet -> import it now
                ImportRegistration ir = importService(epd);
                if (ir != null) {
                    // import was successful
                    irs.add(ir);
                }
            }
        }

    }

    private void importSingleServiceStrategy(final String filter) {

        if (importedServices.containsKey(filter) && importedServices.get(filter) != null
            && importedServices.get(filter).size() > 0) {
            // a service was already imported ....
            List<ImportRegistration> irs = importedServices.get(filter);
            List<EndpointDescription> ips = importPossibilities.get(filter);

            Iterator<ImportRegistration> it = irs.iterator();
            while (it.hasNext()) {
                ImportRegistration ir = it.next();
                EndpointDescription ep = ir.getImportReference().getImportedEndpoint();

                // if service is already imported, check if endpoint is still in the list of
                // possible imports
                if ((ips != null && !ips.contains(ep)) || ips == null) {
                    // unexport service
                    ir.close();
                    it.remove();
                }
            }

            if (irs.size() == 0) {
                // if there are other import possibilities available, try to import them...
                if (ips != null && ips.size() > 0) {
                    triggerImport(filter);
                }
            }

            // TODO but optional: if the service is already imported and the endpoint is still
            // in the list
            // of possible imports check if a "better" endpoint is now in the list ?

        } else {
            // if the service is not yet imported, try ...
            if (importPossibilities.get(filter).size() > 0) {
                for (EndpointDescription ep : importPossibilities.get(filter)) {
                    ImportRegistration ir = importService(ep);
                    if (ir != null) {
                        // import was successful
                        List<ImportRegistration> irs = importedServices.get(filter);
                        if (irs == null) {
                            irs = new ArrayList<ImportRegistration>(1);
                            importedServices.put(filter, irs);
                        }
                        irs.add(ir);
                        break;
                    } else {
                        // import of endpoint failed -> try next one
                    }
                }
            }

        }
    }

    private ImportRegistration importService(EndpointDescription ep) {
        synchronized (remoteServiceAdminList) {
            for (RemoteServiceAdmin rsa : remoteServiceAdminList) {
                ImportRegistration ir = rsa.importService(ep);
                if (ir != null && ir.getException() == null) {
                    // successful
                    LOG.fine("service impoort was successful: " + ir);
                    return ir;
                } else {
                    // failed -> next RSA
                }
            }
        }
        return null;
    }

    public void removeImportRegistration(ImportRegistration importRegistration) {
        synchronized (importedServices) {
            if (importedServices.remove(importRegistration) != null) {
                LOG.fine("removed imported service reference: " + importRegistration);
            }
        }
    }

    public void triggerExportImportForRemoteSericeAdmin(RemoteServiceAdmin rsa) {
        LOG.severe("NOT IMPLEMENTED !!!");
    }

    public void removeImportReference(ImportReference anyObject) {
        LOG.severe("NOT IMPLEMENTED !!!");
    }

}
