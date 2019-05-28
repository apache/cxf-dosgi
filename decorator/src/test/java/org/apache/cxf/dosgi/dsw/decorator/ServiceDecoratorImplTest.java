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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ServiceDecoratorImplTest {
    private static final Map<String, Object> EMPTY = new HashMap<>();
    private static final URL RES_SD = getResource("/test-resources/sd.xml");
    private static final URL RES_SD1 = getResource("/test-resources/sd1.xml");
    private static final URL RES_SD2 = getResource("/test-resources/sd2.xml");
    private static final URL RES_SD0 = getResource("/test-resources/sd0.xml");
    private static final URL RES_SD_1 = getResource("/test-resources/sd-1.xml");

    @Test
    @SuppressWarnings("rawtypes")
    public void testAddRemoveDecorations() {
        final Map<String, Object> serviceProps = new HashMap<>();
        serviceProps.put(Constants.OBJECTCLASS, new String[] {"org.acme.foo.Bar"});
        serviceProps.put("test.prop", "xyz");

        Bundle b = createBundleContaining(RES_SD);
        ServiceDecoratorImpl sd = new ServiceDecoratorImpl();
        assertEquals("Precondition failed", 0, sd.decorations.size());
        sd.addDecorations(b);
        assertEquals(1, sd.decorations.size());

        Map<String, Object> target = new HashMap<>();
        ServiceReference sref = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sref.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                return serviceProps.get(EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(sref);
        sd.decorate(sref, target);

        Map<String, Object> expected = new HashMap<>();
        expected.put("test.too", "ahaha");
        assertEquals(expected, target);

        // remove it again
        sd.removeDecorations(b);
        assertEquals(0, sd.decorations.size());
        Map<String, Object> target2 = new HashMap<>();
        sd.decorate(sref, target2);
        assertEquals(EMPTY, target2);
    }

    @Test
    public void testAddDecorations() {
        final Map<String, Object> serviceProps = new HashMap<>();
        serviceProps.put(Constants.OBJECTCLASS, new String[] {"org.acme.foo.Bar"});
        serviceProps.put("test.prop", "xyz");

        Map<String, Object> expected = new HashMap<>();
        expected.put("test.too", "ahaha");
        assertDecorate(serviceProps, expected, RES_SD);
    }

    @Test
    public void testAddDecorations1() {
        Map<String, Object> serviceProps = new HashMap<>();
        serviceProps.put(Constants.OBJECTCLASS, new String[] {"org.test.A"});

        Map<String, Object> expected = new HashMap<>();
        expected.put("A", "B");
        expected.put("C", 2);
        assertDecorate(serviceProps, expected, RES_SD1, RES_SD2);
    }

    @Test
    public void testAddDecorations2() {
        Map<String, Object> serviceProps = new HashMap<>();
        serviceProps.put(Constants.OBJECTCLASS, new String[] {"org.test.D"});

        assertDecorate(serviceProps, EMPTY, RES_SD1, RES_SD2);
    }

    @Test
    public void testAddDecorations3() {
        Map<String, Object> serviceProps = new HashMap<>();
        serviceProps.put(Constants.OBJECTCLASS, new String[] {"org.test.B"});
        serviceProps.put("x", "y");

        Map<String, Object> expected = new HashMap<>();
        expected.put("bool", Boolean.TRUE);
        assertDecorate(serviceProps, expected, RES_SD1, RES_SD2);
    }

    @Test
    public void testAddDecorations4() {
        Map<String, Object> serviceProps = new HashMap<>();
        serviceProps.put(Constants.OBJECTCLASS, new String[] {"org.test.C"});
        serviceProps.put("x", "z");

        Map<String, Object> expected = new HashMap<>();
        expected.put("bool", Boolean.FALSE);
        assertDecorate(serviceProps, expected, RES_SD1, RES_SD2);
    }

    @Test
    public void testAddDecorations5() {
        Map<String, Object> serviceProps = new HashMap<>();
        serviceProps.put(Constants.OBJECTCLASS, new String[] {"org.test.C"});
        serviceProps.put("x", "x");

        assertDecorate(serviceProps, EMPTY, RES_SD1, RES_SD2);
    }

    @Test
    public void testAddDecorations6() {
        Map<String, Object> serviceProps = new HashMap<>();
        serviceProps.put(Constants.OBJECTCLASS, new String[] {"org.test.D"});

        assertDecorate(serviceProps, EMPTY, RES_SD0);
    }

    @Test
    public void testAddDecorations7() {
        Map<String, Object> serviceProps = new HashMap<>();
        serviceProps.put(Constants.OBJECTCLASS, new String[] {"org.test.D"});

        assertDecorate(serviceProps, EMPTY, RES_SD_1);
    }

    private void assertDecorate(final Map<String, Object> serviceProps,
                                Map<String, Object> expected, URL... resources) {
        Map<String, Object> actual = testDecorate(serviceProps, resources);
        assertEquals(expected, actual);
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Object> testDecorate(final Map<String, Object> serviceProps, URL... resources) {
        Bundle b = createBundleContaining(resources);

        ServiceDecoratorImpl sd = new ServiceDecoratorImpl();
        sd.addDecorations(b);

        Map<String, Object> target = new HashMap<>();
        ServiceReference sref = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sref.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                return serviceProps.get(EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(sref);
        sd.decorate(sref, target);
        return target;
    }

    private Bundle createBundleContaining(URL... resources) {
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.findEntries("OSGI-INF/remote-service", "*.xml", false)).andReturn(
            Collections.enumeration(Arrays.asList(resources))).anyTimes();
        EasyMock.expect(b.getSymbolicName()).andReturn("bundlename");
        EasyMock.replay(b);
        return b;
    }

    private static URL getResource(String path) {
        URL resource = ServiceDecoratorImplTest.class.getResource(path);
        assertNotNull("Resource " + path + " not found!", resource);
        return resource;
    }

}
