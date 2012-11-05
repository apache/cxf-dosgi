package org.apache.cxf.dosgi.dsw.handlers;

import org.apache.cxf.endpoint.Server;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public interface CXFExportRegistration {

    public abstract ServiceReference getExportedService() throws IllegalStateException;

    public abstract void setEndpointdescription(EndpointDescription epd);

    public abstract void setServer(Server server);

    public abstract Server getServer();

    public abstract void setException(Throwable ex);

}