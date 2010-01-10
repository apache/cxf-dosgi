package org.apache.cxf.dosgi.discovery.local;

import junit.framework.TestCase;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;

public class ActivatorTest extends TestCase {
    public void testActivator() throws Exception {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.createFilter((String) EasyMock.anyObject())).andAnswer(new IAnswer<Filter>() {            
            public Filter answer() throws Throwable {                
                return FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(bc);
        
        Activator a = new Activator();
        a.start(bc);
        assertNotNull(a.localDiscovery);
        
        a.localDiscovery = EasyMock.createMock(LocalDiscovery.class);
        a.localDiscovery.shutDown();
        EasyMock.expectLastCall();
        EasyMock.replay(a.localDiscovery);
        a.stop(bc);
        
        EasyMock.verify(a.localDiscovery);
    }
}
