package org.apache.cxf.dosgi.topologymanager;

import java.util.Dictionary;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

import org.junit.Test;
import static org.junit.Assert.*;

public class TopologyManagerImportTest {

    
    @Test
    public void testImportForNewlyAddedRSA() throws InterruptedException{
        
        IMocksControl c = EasyMock.createNiceControl();

        c.makeThreadSafe(true);
        
        final Semaphore sema = new Semaphore(0);
        
        BundleContext bc = c.createMock(BundleContext.class);
        ServiceRegistration sreg = c.createMock(ServiceRegistration.class);
        EasyMock.expect(bc.registerService((String)EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary)EasyMock.anyObject())).andReturn(sreg).anyTimes();
         
        
        EndpointDescription epd = c.createMock(EndpointDescription.class);
        RemoteServiceAdmin rsa  = c.createMock(RemoteServiceAdmin.class);
        final ImportRegistration ireg = c.createMock(ImportRegistration.class);
        ImportReference iref = c.createMock(ImportReference.class);
        
        EasyMock.expect(rsa.importService(EasyMock.eq(epd))).andAnswer(new IAnswer<ImportRegistration>( ) {

            public ImportRegistration answer() throws Throwable {
                sema.release();
                return ireg;
            }
        }).once();
        EasyMock.expect(ireg.getImportReference()).andReturn(iref).anyTimes();
        EasyMock.expect(iref.getImportedEndpoint()).andReturn(epd).anyTimes();
        c.replay();
        
        
        RemoteServiceAdminList rsaList = new RemoteServiceAdminList(bc);
        
        TopologyManagerImport tm = new TopologyManagerImport(bc, rsaList);

        tm.start();
        
        // no RSa available yet so no import ...  
        tm.addImportableService("myFilter", epd);
        
        rsaList.add(rsa);
        
        tm.triggerExportImportForRemoteServiceAdmin(rsa);
        
        assertTrue(sema.tryAcquire(10, TimeUnit.SECONDS));
        
        
        tm.stop();
        
        c.verify();
        
    }
    
}
