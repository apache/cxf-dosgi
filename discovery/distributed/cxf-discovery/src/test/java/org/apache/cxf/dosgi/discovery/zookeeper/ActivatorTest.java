package org.apache.cxf.dosgi.discovery.zookeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class ActivatorTest extends TestCase {
    public void testActivatorStartStop() throws Exception {
        BundleContext bc = getDefaultBundleContext();        
        Activator a = new Activator();
        a.start(bc);

        Dictionary<String, Object> expected = getDefaultProperties();
        assertEquals(expected, propsAsDict(a.cmReg.getReference()));
        
        assertFalse("Precondition failed", ((TestServiceRegistration) a.cmReg).unregisterCalled);
        a.stop(bc);
        assertTrue(((TestServiceRegistration) a.cmReg).unregisterCalled);
    }

    public void testConfigUpdate() throws Exception {
        final DiscoveryDriver mockDriver = EasyMock.createMock(DiscoveryDriver.class);
        
        BundleContext bc = getDefaultBundleContext();
        final List<Dictionary> configs = new ArrayList<Dictionary>();
        Activator a = new Activator() {
            @Override
            DiscoveryDriver createDriver(Dictionary configuration)
                    throws IOException, ConfigurationException {
                configs.add(configuration);
                return mockDriver;
            }            
        };
        a.start(bc);

        a.updated(null);
        assertFalse("Should not do anything with a null argument",
            ((TestServiceRegistration) a.cmReg).setPropertiesCalled);

        EasyMock.replay(mockDriver);

        assertEquals("Precondition failed", 0, configs.size());
        Dictionary<String, Object> d = new Hashtable<String, Object>();
        d.put("a", "b");
        a.updated(d);
        
        Dictionary<String, Object> expected = getDefaultProperties();
        expected.put("a", "b");
        assertEquals(Arrays.asList(expected), configs);
        assertTrue(((TestServiceRegistration) a.cmReg).setPropertiesCalled);
        assertEquals(expected, propsAsDict(a.cmReg.getReference()));
        EasyMock.verify(mockDriver);
        
        Dictionary<String, Object> d2 = new Hashtable<String, Object>();
        d2.put("c", "d");
        
        Dictionary<String, Object> expected2 = getDefaultProperties();
        expected2.put("c", "d");

        EasyMock.reset(mockDriver);
        mockDriver.updateConfiguration(expected2);
        EasyMock.expectLastCall();
        EasyMock.replay(mockDriver);
        
        a.updated(d2);        
        assertEquals(expected2, propsAsDict(a.cmReg.getReference()));
        EasyMock.verify(mockDriver);
        
        EasyMock.reset(mockDriver);
        mockDriver.destroy();
        EasyMock.expectLastCall();
        EasyMock.replay(mockDriver);
        a.stop(bc);

        EasyMock.verify(mockDriver);
    }

    private BundleContext getDefaultBundleContext() {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.registerService(EasyMock.eq(ManagedService.class.getName()), 
            EasyMock.anyObject(), (Dictionary) EasyMock.anyObject())).
            andAnswer(new IAnswer<ServiceRegistration>() {
                public ServiceRegistration answer() throws Throwable {
                    Dictionary d = (Dictionary) EasyMock.getCurrentArguments()[2];
                    return new TestServiceRegistration(d);
                }

            }).anyTimes();
        EasyMock.replay(bc);
        return bc;
    }
    
    private Dictionary<String, Object> getDefaultProperties() {
        Dictionary<String, Object> expected = new Hashtable<String, Object>(); 
        expected.put("zookeeper.timeout", "3000");
        expected.put("zookeeper.port", "2181");
        expected.put(Constants.SERVICE_PID, "org.apache.cxf.dosgi.discovery.zookeeper");
        return expected;
    }
    
    public Dictionary<String, Object> propsAsDict(ServiceReference ref) {
        Dictionary<String, Object> m = new Hashtable<String, Object>();
        
        for (String key : ref.getPropertyKeys()) {
            m.put(key, ref.getProperty(key));
        }
        return m;
    }
    
    private static class TestServiceRegistration implements ServiceRegistration {
        private boolean setPropertiesCalled = false;
        private boolean unregisterCalled = false;
        private final TestServiceReference tsr;
        
        private TestServiceRegistration(Dictionary properties) {
            tsr = new TestServiceReference(properties);
        }

        public ServiceReference getReference() {
            return tsr;
        }

        public void setProperties(Dictionary d) {
            setPropertiesCalled = true;
            tsr.properties = d;
        }

        public void unregister() {
            unregisterCalled = true;
        }            
    }                    

    private static class TestServiceReference implements ServiceReference {
        private Dictionary properties;

        private TestServiceReference(Dictionary p) {
            properties = p;
        }
        
        public int compareTo(Object arg0) {
            return 0;
        }

        public Bundle getBundle() {
            return null;
        }

        public Object getProperty(String key) {
            return properties.get(key);
        }

        public String[] getPropertyKeys() {
            return (String[]) Collections.list(properties.keys()).toArray(new String [] {});
        }

        public Bundle[] getUsingBundles() {
            return new Bundle [] {};
        }

        public boolean isAssignableTo(Bundle arg0, String arg1) {
            return false;
        }        
    };
}
