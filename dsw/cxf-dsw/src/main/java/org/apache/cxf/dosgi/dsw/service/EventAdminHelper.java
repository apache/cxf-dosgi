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

import java.util.Dictionary;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;

public class EventAdminHelper {

    private final static Logger LOG = LogUtils.getL7dLogger(EventAdminHelper.class);
    
    private BundleContext bctx;

    public EventAdminHelper(BundleContext bc) {
        bctx = bc;
    }

    @SuppressWarnings("rawtypes")
	private Event createEvent(Properties props, String type) {

        String topic = "org/osgi/service/remoteserviceadmin/" + type;
        props.put("bundle", bctx.getBundle());
        props.put("bundle.id", bctx.getBundle().getBundleId());
        props.put("bundle.symbolicname", bctx.getBundle().getSymbolicName());
        // FIXME is this correct?
        
        String version = (String)bctx.getBundle().getHeaders().get("Bundle-Version");
        
        Version v;
        if(version!=null)
            v = new Version(version);
        else
            v = Version.emptyVersion;
        
        setIfNotNull(props, "bundle.version", v);

        return new Event(topic, (Dictionary)props);
    }

    public void notifyEventAdmin(RemoteServiceAdminEvent rsae) {

        String topic = remoteServiceAdminEventTypeToString(rsae.getType());

        Properties props = new Properties();
        setIfNotNull(props, "cause", rsae.getException());

        
        EndpointDescription epd = null;
        if (rsae.getImportReference()!= null) {
            epd = ((ImportReferenceImpl)rsae.getImportReference()).getImportedEndpointAlways();
            setIfNotNull(props, "import.registration", epd);
        } else if (rsae.getExportReference() != null) {
            epd = ((ExportReferenceImpl)rsae.getExportReference()).getExportedEndpointAlways();
            setIfNotNull(props, "export.registration", epd);
        }

        if (epd != null) {
            setIfNotNull(props, "service.remote.id", epd.getServiceId());
            setIfNotNull(props, "service.remote.uuid", epd.getFrameworkUUID());
            setIfNotNull(props, "service.remote.uri", epd.getId());
            setIfNotNull(props, "objectClass", epd.getInterfaces().toArray());
            setIfNotNull(props, "service.imported.configs", epd.getConfigurationTypes());
        }
        props.put("timestamp", System.currentTimeMillis());
        props.put("event", rsae);

        Event ev = createEvent(props, topic);

        EventAdmin[] eas = getEventAdmins();
        if (eas != null) {
            LOG.fine("Publishing event to "+eas.length+" EventAdmins;  Topic:["+topic+"]");
            for (EventAdmin eventAdmin : eas) {
                eventAdmin.postEvent(ev);
            }
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private void setIfNotNull(Dictionary props, String key, Object o) {
        if (o != null)
            props.put(key, o);
    }

    private EventAdmin[] getEventAdmins() {
        ServiceReference[] refs = null;
        try {
            refs = bctx.getAllServiceReferences(EventAdmin.class.getName(), null);
        } catch (InvalidSyntaxException e) {
            LOG.log(Level.SEVERE, "Failed to get EventAdmin: " + e.getMessage(), e);
        }

        if (refs == null)
            return null;

        EventAdmin[] eas = new EventAdmin[refs.length];
        for (int x = 0; x < refs.length; ++x) {

            ServiceReference serviceReference = refs[x];
            eas[x] = (EventAdmin)bctx.getService(serviceReference);
        }

        return eas;
    }

    static String remoteServiceAdminEventTypeToString(int type){
        switch (type) {
        case RemoteServiceAdminEvent.EXPORT_ERROR:
            return "EXPORT_ERROR";
        case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
            return "EXPORT_REGISTRATION";
        case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION:
            return "EXPORT_UNREGISTRATION";
        case RemoteServiceAdminEvent.EXPORT_WARNING:
            return "EXPORT_WARNING";
        case RemoteServiceAdminEvent.IMPORT_ERROR:
            return "IMPORT_ERROR";
        case RemoteServiceAdminEvent.IMPORT_REGISTRATION:
            return "IMPORT_REGISTRATION";
        case RemoteServiceAdminEvent.IMPORT_UNREGISTRATION:
            return "IMPORT_UNREGISTRATION";
        case RemoteServiceAdminEvent.IMPORT_WARNING:
            return "IMPORT_WARNING";    
        default:
            return "UNKNOWN_EVENT";
        }
    }

}
