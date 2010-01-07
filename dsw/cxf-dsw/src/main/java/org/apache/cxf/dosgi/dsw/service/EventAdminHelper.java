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

import java.util.Arrays;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;

public class EventAdminHelper {

    private BundleContext bctx;

    public EventAdminHelper(BundleContext bc) {
        bctx = bc;
    }

    private Event createEvent(Properties p, String t) {

        String topic = "org/osgi/service/remoteserviceadmin/" + t;
        Properties props = new Properties(p);

        props.put("bundle", bctx.getBundle());
        props.put("bundle-id", bctx.getBundle().getBundleId());
        props.put("bundle-symbolicname", bctx.getBundle().getSymbolicName());
        // FIXME is this correct?
        setIfNotNull(props, "bundle-version", bctx.getBundle().getHeaders("Bundle-Version"));

        return new Event(topic, props);
    }

    public void notifyEventAdmin(RemoteServiceAdminEvent rsae) {

        String topic = Utils.remoteServiceAdminEventTypeToString(rsae.getType());

        EndpointDescription epd = null;
        if (rsae.getImportReference()!= null) {
            epd = rsae.getImportReference().getImportedEndpoint();
        } else if (rsae.getExportReference() != null) {
            epd = rsae.getExportReference().getExportedEndpoint();
        }

        Properties props = new Properties();
        setIfNotNull(props, "cause", rsae.getException());
//FIXME !!!!!!!!!!!!!
//        setIfNotNull(props, "import.registration", rsae.getImportRegistration());
//        setIfNotNull(props, "export.registration", rsae.getExportRegistration());
        if (epd != null) {
            setIfNotNull(props, "service.remote.id", epd.getRemoteServiceID());
            setIfNotNull(props, "service.remote.uuid", epd.getRemoteFrameworkUUID());
            setIfNotNull(props, "service.remote.uri", epd.getRemoteID());
            // FIXME: correct ?!?
            setIfNotNull(props, "objectClass", epd.getInterfaces().toArray());
            setIfNotNull(props, "service.imported.configs", epd.getConfigurationTypes());
            setIfNotNull(props, "service.imported.configs", epd.getConfigurationTypes());
        }
        props.put("timestamp", System.currentTimeMillis());
        props.put("event", rsae);

        Event ev = createEvent(props, topic);

        EventAdmin[] eas = getEventAdmins();
        if (eas != null) {
            for (EventAdmin eventAdmin : eas) {
                eventAdmin.postEvent(ev);
            }
        }

    }

    private void setIfNotNull(Properties props, String key, Object o) {
        if (o != null)
            props.put(key, o);
    }

    private EventAdmin[] getEventAdmins() {
        ServiceReference[] refs = null;
        try {
            refs = bctx.getAllServiceReferences(EventAdmin.class.getName(), null);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
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

}
