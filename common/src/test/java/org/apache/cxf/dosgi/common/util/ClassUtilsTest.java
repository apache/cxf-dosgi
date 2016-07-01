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
package org.apache.cxf.dosgi.common.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

public class ClassUtilsTest extends TestCase {

    public void testLoadProvidersAsString() throws Exception {
        BundleContext bc = mockBundleContext();
        Map<String, Object> sd = Collections.<String, Object>singletonMap("providers", Provider.class.getName());
        List<Object> providers = ClassUtils.loadProviderClasses(bc, sd, "providers");
        assertEquals(1, providers.size());
        assertTrue(providers.get(0) instanceof Provider);
    }

    public void testLoadProvidersAsStringArray() throws Exception {
        BundleContext bc = mockBundleContext();
        Map<String, Object> sd = Collections.<String, Object>singletonMap("providers",
                new String[]{Provider.class.getName()});
        List<Object> providers = ClassUtils.loadProviderClasses(bc, sd, "providers");
        assertEquals(1, providers.size());
        assertTrue(providers.get(0) instanceof Provider);
    }

    public void testLoadProvidersAsObject() throws Exception {
        Map<String, Object> sd = Collections.<String, Object>singletonMap("providers", new Provider());
        List<Object> providers = ClassUtils.loadProviderClasses(null, sd, "providers");
        assertEquals(1, providers.size());
        assertTrue(providers.get(0) instanceof Provider);
    }

    public void testLoadProvidersAsObjectArray() throws Exception {
        Map<String, Object> sd = Collections.<String, Object>singletonMap("providers", new Object[]{new Provider()});
        List<Object> providers = ClassUtils.loadProviderClasses(null, sd, "providers");
        assertEquals(1, providers.size());
        assertTrue(providers.get(0) instanceof Provider);
    }

    public void testLoadProvidersAsObjectList() throws Exception {
        List<Object> list = new LinkedList<Object>();
        list.add(new Provider());
        Map<String, Object> sd = Collections.<String, Object>singletonMap("providers", list);
        List<Object> providers = ClassUtils.loadProviderClasses(null, sd, "providers");
        assertEquals(1, providers.size());
        assertTrue(providers.get(0) instanceof Provider);
    }

    public void testLoadProvidersAsStringList() throws Exception {
        List<Object> list = new LinkedList<Object>();
        list.add(Provider.class.getName());
        Map<String, Object> sd = Collections.<String, Object>singletonMap("providers", list);
        List<Object> providers = ClassUtils.loadProviderClasses(mockBundleContext(), sd, "providers");
        assertEquals(1, providers.size());
        assertTrue(providers.get(0) instanceof Provider);
    }

    private BundleContext mockBundleContext() throws Exception {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        Bundle bundle = EasyMock.createMock(Bundle.class);
        bc.getBundle();
        EasyMock.expectLastCall().andReturn(bundle);
        bundle.loadClass(Provider.class.getName());
        EasyMock.expectLastCall().andReturn(Provider.class);
        EasyMock.replay(bc, bundle);
        return bc;
    }

    @SuppressWarnings({ "serial", "rawtypes" })
    private static class MyMapSubclass extends HashMap {
    }

    @SuppressWarnings("serial")
    static class MySubclassOne extends MyMapSubclass {
    }

    @SuppressWarnings("serial")
    static class MySubclassTwo extends MySubclassOne {
    }

    @SuppressWarnings("serial")
    static class MySubclassThree extends MySubclassTwo {
    }

    @SuppressWarnings("serial")
    static class MySubclassFour extends MySubclassThree {
    }
}
