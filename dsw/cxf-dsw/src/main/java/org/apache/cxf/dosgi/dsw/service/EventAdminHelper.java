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

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cxf.dosgi.dsw.util.Utils.setIfNotNull;

public class EventAdminHelper {

    private static final Logger LOG = LoggerFactory.getLogger(EventAdminHelper.class);

    private BundleContext bctx;

    public EventAdminHelper(BundleContext bc) {
        bctx = bc;
    }

    private Event createEvent(Map<String, Object> props, String type) {
        String topic = "org/osgi/service/remoteserviceadmin/" + type;
        props.put("bundle", bctx.getBundle());
        props.put("bundle.id", bctx.getBundle().getBundleId());
        props.put("bundle.symbolicname", bctx.getBundle().getSymbolicName());

        String version = (String)bctx.getBundle().getHeaders().get("Bundle-Version");
        Version v = version != null ? new Version(version) : Version.emptyVersion;
        setIfNotNull(props, "bundle.version", v);

        return new Event(topic, props);
    }

    public void notifyEventAdmin(RemoteServiceAdminEvent rsae) {
        String topic = remoteServiceAdminEventTypeToString(rsae.getType());

        Map<String, Object> props = new HashMap<String, Object>();
        setIfNotNull(props, "cause", rsae.getException());

        EndpointDescription endpoint = null;
        if (rsae.getImportReference() != null) {
            endpoint = ((ImportRegistrationImpl)rsae.getImportReference()).getImportedEndpointAlways();
            setIfNotNull(props, "import.registration", endpoint);
        } else if (rsae.getExportReference() != null) {
            endpoint = rsae.getExportReference().getExportedEndpoint();
            setIfNotNull(props, "export.registration", endpoint);
        }

        if (endpoint != null) {
            setIfNotNull(props, "service.remote.id", endpoint.getServiceId());
            setIfNotNull(props, "service.remote.uuid", endpoint.getFrameworkUUID());
            setIfNotNull(props, "service.remote.uri", endpoint.getId());
            setIfNotNull(props, "objectClass", endpoint.getInterfaces().toArray());
            setIfNotNull(props, "service.imported.configs", endpoint.getConfigurationTypes());
        }
        props.put("timestamp", System.currentTimeMillis());
        props.put("event", rsae);

        Event event = createEvent(props, topic);
        notifyEventAdmins(topic, event);
    }

    @SuppressWarnings({
     "rawtypes", "unchecked"
    })
    private void notifyEventAdmins(String topic, Event event) {
        ServiceReference[] refs = null;
        try {
            refs = bctx.getAllServiceReferences(EventAdmin.class.getName(), null);
        } catch (InvalidSyntaxException e) {
            LOG.error("Failed to get EventAdmin: " + e.getMessage(), e);
        }

        if (refs != null) {
            LOG.debug("Publishing event to {} EventAdmins; Topic:[{}]", refs.length, topic);
            for (ServiceReference serviceReference : refs) {
                EventAdmin eventAdmin = (EventAdmin) bctx.getService(serviceReference);
                try {
                    eventAdmin.postEvent(event);
                } finally {
                    if (eventAdmin != null) {
                        bctx.ungetService(serviceReference);
                    }
                }
            }
        }
    }

    private static String remoteServiceAdminEventTypeToString(int type) {
        String retval;
        switch (type) {
        case RemoteServiceAdminEvent.EXPORT_ERROR:
            retval = "EXPORT_ERROR";
            break;
        case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
            retval = "EXPORT_REGISTRATION";
            break;
        case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION:
            retval = "EXPORT_UNREGISTRATION";
            break;
        case RemoteServiceAdminEvent.EXPORT_WARNING:
            retval = "EXPORT_WARNING";
            break;
        case RemoteServiceAdminEvent.IMPORT_ERROR:
            retval = "IMPORT_ERROR";
            break;
        case RemoteServiceAdminEvent.IMPORT_REGISTRATION:
            retval = "IMPORT_REGISTRATION";
            break;
        case RemoteServiceAdminEvent.IMPORT_UNREGISTRATION:
            retval = "IMPORT_UNREGISTRATION";
            break;
        case RemoteServiceAdminEvent.IMPORT_WARNING:
            retval = "IMPORT_WARNING";
            break;
        default:
            retval = "UNKNOWN_EVENT";
        }
        return retval;
    }
}
