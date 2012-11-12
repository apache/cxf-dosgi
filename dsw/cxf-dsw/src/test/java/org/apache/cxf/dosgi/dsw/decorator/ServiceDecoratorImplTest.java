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
package org.apache.cxf.dosgi.dsw.decorator;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.jdom.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ServiceDecoratorImplTest extends TestCase {
    
    public void testServiceDecorator() {
        final BundleListener[] bundleListener = new BundleListener[1]; 
        
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        bc.addBundleListener((BundleListener) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {            
            public Object answer() throws Throwable {
                bundleListener[0] = (BundleListener) EasyMock.getCurrentArguments()[0];
                return null;
            }
        });
        EasyMock.replay(bc);
        
        ServiceDecoratorImpl sd = new ServiceDecoratorImpl(bc);
        EasyMock.verify(bc);
        assertNotNull(bundleListener[0]);
        
        EasyMock.reset(bc);
        bc.removeBundleListener(bundleListener[0]);
        EasyMock.replay(bc);
        sd.shutdown();
        
        EasyMock.verify(bc);
    }
    
    public void testGetDecoratorElements() {
        URL sdURL = getClass().getResource("/test-resources/sd.xml");
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.findEntries("OSGI-INF/remote-service", "*.xml", false)).andReturn(
            Collections.enumeration(Arrays.asList(sdURL))).anyTimes();
        EasyMock.replay(b);

        List<Element> elements = ServiceDecoratorImpl.getDecorationElements(b);
        assertEquals(1, elements.size());
        assertEquals("service-decoration", elements.get(0).getName());
        assertEquals("http://cxf.apache.org/xmlns/service-decoration/1.0.0", elements.get(0).getNamespaceURI());
    }

    public void testGetDecoratorElements2() {
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.findEntries("OSGI-INF/remote-service", "*.xml", false)).andReturn(null).anyTimes();
        EasyMock.replay(b);

        List<Element> elements = ServiceDecoratorImpl.getDecorationElements(b);
        assertEquals(0, elements.size());
    }

    public void testAddRemoveDecorations() {
        URL res = getClass().getResource("/test-resources/sd.xml");
        final Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.acme.foo.Bar"});
        serviceProps.put("test.prop", "xyz");
        
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.findEntries("OSGI-INF/remote-service", "*.xml", false)).andReturn(
            Collections.enumeration(Arrays.asList(res))).anyTimes();
        EasyMock.replay(b);

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        ServiceDecoratorImpl sd = new ServiceDecoratorImpl(bc);
        assertEquals("Precondition failed", 0, sd.decorations.size());
        sd.addDecorations(b);
        assertEquals(1, sd.decorations.size());
        
        Map<String, Object> target = new HashMap<String, Object>();
        ServiceReference sref = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sref.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                return serviceProps.get(EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(sref);
        sd.decorate(sref, target);
        
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("test.too", "ahaha");
        assertEquals(expected, target);
        
        // remove it again
        sd.removeDecorations(b);
        assertEquals(0, sd.decorations.size());
        Map<String, Object> target2 = new HashMap<String, Object>();
        sd.decorate(sref, target2);        
        Map<String, Object> expected2 = new HashMap<String, Object>();
        assertEquals(expected2, target2);
    }
   
    public void testAddDecorations() {
        URL res = getClass().getResource("/test-resources/sd.xml");
        final Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.acme.foo.Bar"});
        serviceProps.put("test.prop", "xyz");
        
        Map<String, Object> target = testDecorate(serviceProps, res);
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("test.too", "ahaha");
        assertEquals(expected, target);
    }
    
    public void testAddDecorations1() {
        URL r1 = getClass().getResource("/test-resources/sd1.xml");
        URL r2 = getClass().getResource("/test-resources/sd2.xml");
        
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.test.A"});

        Map<String, Object> actual = testDecorate(serviceProps, r1, r2);
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("A", "B");
        expected.put("C", new Integer(2));
        assertEquals(expected, actual);
    }

    public void testAddDecorations2() {
        URL r1 = getClass().getResource("/test-resources/sd1.xml");
        URL r2 = getClass().getResource("/test-resources/sd2.xml");
        
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.test.D"});

        Map<String, Object> actual = testDecorate(serviceProps, r1, r2);
        Map<String, Object> expected = new HashMap<String, Object>();
        assertEquals(expected, actual);
    }

    public void testAddDecorations3() {
        URL r1 = getClass().getResource("/test-resources/sd1.xml");
        URL r2 = getClass().getResource("/test-resources/sd2.xml");
        
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.test.B"});
        serviceProps.put("x", "y");

        Map<String, Object> actual = testDecorate(serviceProps, r1, r2);
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("bool", Boolean.TRUE);
        assertEquals(expected, actual);
    }

    public void testAddDecorations4() {
        URL r1 = getClass().getResource("/test-resources/sd1.xml");
        URL r2 = getClass().getResource("/test-resources/sd2.xml");
        
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.test.C"});
        serviceProps.put("x", "z");

        Map<String, Object> actual = testDecorate(serviceProps, r1, r2);
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("bool", Boolean.FALSE);
        assertEquals(expected, actual);
    }

    public void testAddDecorations5() {
        URL r1 = getClass().getResource("/test-resources/sd1.xml");
        URL r2 = getClass().getResource("/test-resources/sd2.xml");
        
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.test.C"});
        serviceProps.put("x", "x");

        Map<String, Object> actual = testDecorate(serviceProps, r1, r2);
        Map<String, Object> expected = new HashMap<String, Object>();
        assertEquals(expected, actual);
    }

    public void testAddDecorations6() {
        URL r1 = getClass().getResource("/test-resources/sd0.xml");
        
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.test.D"});

        Map<String, Object> actual = testDecorate(serviceProps, r1);
        Map<String, Object> expected = new HashMap<String, Object>();
        assertEquals(expected, actual);
    }

    public void testAddDecorations7() {
        URL r1 = getClass().getResource("/test-resources/sd-1.xml");
        
        Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.test.D"});

        Map<String, Object> actual = testDecorate(serviceProps, r1);
        Map<String, Object> expected = new HashMap<String, Object>();
        assertEquals(expected, actual);
    }

    private Map<String, Object> testDecorate(final Map<String, Object> serviceProps, URL ... resources) {
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.findEntries("OSGI-INF/remote-service", "*.xml", false)).andReturn(
            Collections.enumeration(Arrays.asList(resources))).anyTimes();
        EasyMock.replay(b);

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        ServiceDecoratorImpl sd = new ServiceDecoratorImpl(bc);
        sd.addDecorations(b);
        
        Map<String, Object> target = new HashMap<String, Object>();
        ServiceReference sref = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sref.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                return serviceProps.get(EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(sref);
        sd.decorate(sref, target);
        return target;
    }
    
    public void testBundleListener() {
        final BundleListener[] bundleListener = new BundleListener[1]; 
        
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        bc.addBundleListener((BundleListener) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {            
            public Object answer() throws Throwable {
                bundleListener[0] = (BundleListener) EasyMock.getCurrentArguments()[0];
                return null;
            }
        });
        EasyMock.replay(bc);
        
        final List<String> called = new ArrayList<String>();
        new ServiceDecoratorImpl(bc) {
            @Override
            void addDecorations(Bundle bundle) {
                called.add("addDecorations");
            }

            @Override
            void removeDecorations(Bundle bundle) {
                called.add("removeDecorations");
            }            
        };
        
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.replay(b);
        
        assertEquals("Precondition failed", 0, called.size());
        bundleListener[0].bundleChanged(new BundleEvent(BundleEvent.INSTALLED, b));
        assertEquals(0, called.size());
        
        bundleListener[0].bundleChanged(new BundleEvent(BundleEvent.STARTED, b));
        assertEquals(Arrays.asList("addDecorations"), called);
        
        bundleListener[0].bundleChanged(new BundleEvent(BundleEvent.STOPPING, b));
        assertEquals(Arrays.asList("addDecorations", "removeDecorations"), called);
    }
}
