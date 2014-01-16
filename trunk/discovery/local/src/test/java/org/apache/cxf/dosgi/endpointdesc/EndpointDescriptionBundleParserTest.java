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
package org.apache.cxf.dosgi.endpointdesc;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.cxf.dosgi.discovery.local.util.Utils;
import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class EndpointDescriptionBundleParserTest extends TestCase {

    private Bundle createBundleContaining(URL ed1URL) {
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"),
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(ed1URL))).anyTimes();
        EasyMock.replay(b);
        return b;
    }

    public void testAllEndpoints1() {
        URL ed1URL = getClass().getResource("/ed1.xml");

        Bundle b = createBundleContaining(ed1URL);

        List<EndpointDescription> endpoints = new EndpointDescriptionBundleParser().getAllEndpointDescriptions(b);
        assertEquals(4, endpoints.size());
        EndpointDescription endpoint0 = endpoints.get(0);
        assertEquals("http://somewhere:12345", endpoint0.getId());
        assertEquals(Arrays.asList("SomeService"), endpoint0.getInterfaces());
        assertEquals(Arrays.asList("confidentiality"),
            endpoint0.getProperties().get("osgi.remote.requires.intents"));
        assertEquals("testValue", endpoint0.getProperties().get("testKey"));

        EndpointDescription endpoint1 = endpoints.get(1);
        assertEquals("myScheme://somewhere:12345", endpoint1.getId());
        assertEquals(Arrays.asList("SomeOtherService", "WithSomeSecondInterface"), endpoint1.getInterfaces());

        EndpointDescription endpoint2 = endpoints.get(2);
        assertEquals("http://somewhere", endpoint2.getId());
        assertEquals(Arrays.asList("SomeOtherService", "WithSomeSecondInterface"), endpoint2.getInterfaces());

        EndpointDescription endpoint3 = endpoints.get(3);
        assertEquals("http://somewhere:1/2/3/4?5", endpoint3.getId());
        assertEquals(Arrays.asList("SomeOtherService", "WithSomeSecondInterface"), endpoint3.getInterfaces());
    }

    public void testAllEndpoints2() throws Exception {
        URL ed2URL = getClass().getResource("/ed2.xml");

        Bundle b = createBundleContaining(ed2URL);

        List<EndpointDescription> endpoints = new EndpointDescriptionBundleParser().getAllEndpointDescriptions(b);
        assertEquals(2, endpoints.size());
        EndpointDescription endpoint0 = endpoints.get(0);
        assertEquals("foo:bar", endpoint0.getId());
        assertEquals(Arrays.asList("com.acme.HelloService"), endpoint0.getInterfaces());
        assertEquals(Arrays.asList("SOAP"), endpoint0.getIntents());
        // changed from exported to imported
        assertEquals("org.apache.cxf.ws", endpoint0.getProperties().get("service.imported.configs"));

        EndpointDescription endpoint1 = endpoints.get(1);
        Map<String, Object> props = endpoint1.getProperties();
        assertEquals(Arrays.asList("com.acme.HelloService", "some.other.Service"), endpoint1.getInterfaces());
        assertEquals("org.apache.cxf.ws", props.get("service.imported.configs"));
        // exports should have been removed
        assertNull(props.get("service.exported.configs"));

        assertEquals(Utils.normXML("<other:t1 xmlns:other='http://www.acme.org/xmlns/other/v1.0.0' "
            + "xmlns='http://www.acme.org/xmlns/other/v1.0.0'><foo type='bar'>haha</foo>\n"
            + "        </other:t1>"),
            Utils.normXML((String) props.get("someXML")));
        assertEquals(Long.MAX_VALUE, props.get("long"));
        assertEquals(-1L, props.get("long2"));
        assertEquals(Double.MAX_VALUE, props.get("double"));
        assertEquals(1.0d, props.get("Double2"));
        assertEquals(42.24f, props.get("float"));
        assertEquals(1.0f, props.get("Float2"));
        assertEquals(17, props.get("int"));
        assertEquals(42, props.get("Integer2"));
        assertEquals((byte) 127, props.get("byte"));
        assertEquals((byte) -128, props.get("Byte2"));
        assertEquals(Boolean.TRUE, props.get("boolean"));
        assertEquals(Boolean.TRUE, props.get("Boolean2"));
        assertEquals((short) 99, props.get("short"));
        assertEquals((short) -99, props.get("Short2"));
        assertEquals('@', props.get("char"));
        assertEquals('X', props.get("Character2"));

        int[] intArray = (int[]) props.get("int-array");
        assertTrue(Arrays.equals(new int[] {1, 2}, intArray));

        Integer[] integerArray = (Integer[]) props.get("Integer-array");
        assertTrue(Arrays.equals(new Integer[] {2, 1}, integerArray));

        assertEquals(Arrays.asList(true, false), props.get("bool-list"));
        assertEquals(new HashSet<Object>(), props.get("long-set"));
        Set<String> stringSet = new HashSet<String>();
        stringSet.add("Hello there");
        stringSet.add("How are you?");
        assertEquals(stringSet, props.get("string-set"));
        assertEquals("Hello", props.get("other1").toString().trim());

        List<?> l = (List<?>) props.get("other2");
        assertEquals(1, l.size());
        assertEquals(Utils.normXML("<other:t2 xmlns:other='http://www.acme.org/xmlns/other/v1.0.0' " 
                                   + "xmlns='http://www.osgi.org/xmlns/rsa/v1.0.0'/>"),
                                   Utils.normXML((String) l.get(0)));
    }

}
