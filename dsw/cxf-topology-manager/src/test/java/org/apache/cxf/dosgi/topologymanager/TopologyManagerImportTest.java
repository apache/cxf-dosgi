package org.apache.cxf.dosgi.topologymanager;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class TopologyManagerImportTest {

    
    @SuppressWarnings("rawtypes")
    @Test
    public void testImportForNewlyAddedRSA() throws InterruptedException{
        
        IMocksControl c = EasyMock.createControl();

        c.makeThreadSafe(true);
        
        final Semaphore sema = new Semaphore(0);
        
        BundleContext bc = c.createMock(BundleContext.class);
        RemoteServiceAdminTracker rsaTracker = c.createMock(RemoteServiceAdminTracker.class);
        ServiceRegistration sreg = c.createMock(ServiceRegistration.class);
        EasyMock.expect(bc.registerService((String)EasyMock.anyObject(), EasyMock.anyObject(), (Dictionary)EasyMock.anyObject())).andReturn(sreg).anyTimes();
        
        EndpointDescription epd = c.createMock(EndpointDescription.class);
        RemoteServiceAdmin rsa  = c.createMock(RemoteServiceAdmin.class);
        final ImportRegistration ireg = c.createMock(ImportRegistration.class);
        EasyMock.expect(ireg.getException()).andReturn(null);
        ImportReference iref = c.createMock(ImportReference.class);
        
        rsaTracker.addListener(EasyMock.<RemoteServiceAdminLifeCycleListener>anyObject());
        EasyMock.expect(rsaTracker.getList()).andReturn(Arrays.asList(rsa)).anyTimes();
        
        EasyMock.expect(rsa.importService(EasyMock.eq(epd))).andAnswer(new IAnswer<ImportRegistration>( ) {

            public ImportRegistration answer() throws Throwable {
                sema.release();
                return ireg;
            }
        }).once();
        EasyMock.expect(ireg.getImportReference()).andReturn(iref).anyTimes();
        EasyMock.expect(iref.getImportedEndpoint()).andReturn(epd).anyTimes();
        c.replay();
        
        TopologyManagerImport tm = new TopologyManagerImport(bc, rsaTracker);

        tm.start();
        // no RSa available yet so no import ...  
        tm.endpointAdded(epd, "myFilter");
        tm.triggerImportsForRemoteServiceAdmin(rsa);
        assertTrue("importService should have been called on RemoteServiceAdmin", sema.tryAcquire(100, TimeUnit.SECONDS));
        tm.stop();
        
        c.verify();
        
    }
    
}
