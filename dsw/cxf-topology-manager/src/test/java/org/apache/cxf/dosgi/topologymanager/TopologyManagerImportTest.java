package org.apache.cxf.dosgi.topologymanager;

import java.util.Dictionary;

import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

import org.junit.Test;

public class TopologyManagerImportTest {

    
    @Test
    public void testImportForNewlyAddedRSA(){
        
        IMocksControl c = EasyMock.createNiceControl();

        BundleContext bc = c.createMock(BundleContext.class);
        ServiceRegistration sreg = c.createMock(ServiceRegistration.class);
        EasyMock.expect(bc.registerService((String)EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary)EasyMock.anyObject())).andReturn(sreg).anyTimes();
        
        
        EndpointDescription epd = c.createMock(EndpointDescription.class);
        RemoteServiceAdmin rsa  = c.createMock(RemoteServiceAdmin.class);
        ImportRegistration ireg = c.createMock(ImportRegistration.class);
        
        EasyMock.expect(rsa.importService(EasyMock.eq(epd))).andReturn(ireg).once();

        c.replay();
        
        
        RemoteServiceAdminList rsaList = new RemoteServiceAdminList(bc);
        
        TopologyManagerImport tm = new TopologyManagerImport(bc, rsaList);

        tm.start();
        
        // no RSa available yet so no import ...  
        tm.addImportableService("myFilter", epd);
        
        rsaList.add(rsa);
        
        tm.triggerExportImportForRemoteSericeAdmin(rsa);
        
        tm.stop();
        
        c.verify();
        
    }
    
}
