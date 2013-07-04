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
package org.apache.cxf.dosgi.discovery.local.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.osgi.framework.Bundle;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class EndpointUtilsTest extends TestCase {

    private static final String LF = "\n";

    public void testNoRemoteServicesXMLFiles() {
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.replay(b);

        List<Element> rsElements = EndpointUtils.getAllDescriptionElements(b);
        assertEquals(0, rsElements.size());
    }

    public void testEndpointDescriptionXMLFiles() {
        URL ed1URL = getClass().getResource("/ed1.xml");

        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"),
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(ed1URL))).anyTimes();
        EasyMock.replay(b);

        List<Element> edElements = EndpointUtils.getAllDescriptionElements(b);
        assertEquals(4, edElements.size());
    }

    public void testAllEndpoints1() {
        URL ed1URL = getClass().getResource("/ed1.xml");

        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"),
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(ed1URL))).anyTimes();
        EasyMock.replay(b);

        List<EndpointDescription> endpoints = EndpointUtils.getAllEndpointDescriptions(b);
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

        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"),
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(ed2URL))).anyTimes();
        EasyMock.replay(b);

        List<EndpointDescription> endpoints = EndpointUtils.getAllEndpointDescriptions(b);
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

        assertEquals(normXML("<other:t1 xmlns:other='http://www.acme.org/xmlns/other/v1.0.0' "
                             + "xmlns='http://www.acme.org/xmlns/other/v1.0.0'><foo type='bar'>haha</foo></other:t1>"),
            normXML((String) props.get("someXML")));

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
        assertEquals(normXML("<other:t2 xmlns:other='http://www.acme.org/xmlns/other/v1.0.0'/>"),
            normXML((String) l.get(0)));
    }

    public void testLegacyServiceDescriptionFormat() {
        URL sdURL = getClass().getResource("/sd.xml");

        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"),
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(sdURL))).anyTimes();
        EasyMock.replay(b);

        List<EndpointDescription> endpoints = EndpointUtils.getAllEndpointDescriptions(b);
        assertEquals(1, endpoints.size());
        EndpointDescription endpoint = endpoints.get(0);
        assertEquals("http://localhost:9090/greeter", endpoint.getId());
        assertEquals(Arrays.asList("org.apache.cxf.ws"), endpoint.getConfigurationTypes());
        assertEquals(Arrays.asList("org.apache.cxf.dosgi.samples.greeter.GreeterService"), endpoint.getInterfaces());
        assertNull("Should not contain service.exported.*",
                endpoint.getProperties().get(RemoteConstants.SERVICE_EXPORTED_INTERFACES));
        assertNull("Should not contain service.exported.*",
                endpoint.getProperties().get(RemoteConstants.SERVICE_EXPORTED_CONFIGS));
    }

    public void testLegacyServiceDescriptionFormat2() {
        URL sdURL = getClass().getResource("/sd2.xml");

        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(b.findEntries(
            EasyMock.eq("OSGI-INF/remote-service"),
            EasyMock.eq("*.xml"), EasyMock.anyBoolean())).andReturn(
                Collections.enumeration(Arrays.asList(sdURL))).anyTimes();
        EasyMock.replay(b);

        List<EndpointDescription> endpoints = EndpointUtils.getAllEndpointDescriptions(b);
        assertEquals(2, endpoints.size());

        EndpointDescription endpoint0 = endpoints.get(0);
        assertEquals("http://localhost:9000/org/example/SomeService", endpoint0.getId());
        assertEquals(Arrays.asList("org.apache.cxf.ws"), endpoint0.getConfigurationTypes());
        assertEquals(Arrays.asList("org.example.SomeService"), endpoint0.getInterfaces());
        assertEquals(Arrays.asList("confidentiality"), endpoint0.getIntents());

        EndpointDescription endpoint1 = endpoints.get(1);
        assertEquals(Arrays.asList("SomeOtherService", "WithSomeSecondInterface"), endpoint1.getInterfaces());
        assertEquals("5", endpoint1.getProperties().get("blah"));
    }

    public void testCreateXML() throws Exception {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("service.imported.configs", "org.apache.cxf.ws");
        m.put("endpoint.id", "foo:bar");
        m.put("objectClass", new String[] {"com.acme.HelloService", "some.other.Service"});
        m.put("SomeObject", new Object());
        m.put("long", 9223372036854775807L);
        m.put("Long2", -1L);
        m.put("double", 1.7976931348623157E308);
        m.put("Double2", 1.0d);
        m.put("float", 42.24f);
        m.put("Float2", 1.0f);
        m.put("int", 17);
        m.put("Integer2", 42);
        m.put("byte", (byte) 127);
        m.put("Byte2", (byte) -128);
        m.put("boolean", true);
        m.put("Boolean2", false);
        m.put("short", (short) 99);
        m.put("Short2", (short) -99);
        m.put("char", '@');
        m.put("Character2", 'X');

        List<Boolean> boolList = new ArrayList<Boolean>();
        boolList.add(true);
        boolList.add(false);
        m.put("bool-list", boolList);
        m.put("empty-set", new HashSet<Object>());

        Set<String> stringSet = new LinkedHashSet<String>();
        stringSet.add("Hello there");
        stringSet.add("How are you?");
        m.put("string-set", stringSet);

        int[] intArray = new int[] {1, 2};
        m.put("int-array", intArray);

        String xml = "<xml>" + LF
            + "<t1 xmlns=\"http://www.acme.org/xmlns/other/v1.0.0\">" + LF
            + "<foo type='bar'>haha</foo>" + LF
            + "</t1>" + LF
            + "</xml>";
        m.put("someXML", xml);

        String actual = EndpointUtils.getEndpointDescriptionXML(m);

        URL edURL = getClass().getResource("/ed2-generated.xml");
        String expected = new String(drainStream(edURL.openStream()));
        assertEquals(normXML(expected), normXML(actual));
    }

    private static String normXML(String s) throws JDOMException, IOException {
        String s2 = stripComment(s);
        String s3 = stripProlog(s2);
        Document d = new SAXBuilder().build(new ByteArrayInputStream(s3.getBytes()));
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        return outputter.outputString(d);
    }

    private static String stripComment(String s) {
        return Pattern.compile("<!--(.*?)-->", Pattern.DOTALL).matcher(s).replaceAll("");
    }

    private static String stripProlog(String s) {
        return s.replaceAll("<\\?(.*?)\\?>", "");
    }

    public static void drainStream(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[8192];

        int length;
        int offset = 0;

        while ((length = is.read(bytes, offset, bytes.length - offset)) != -1) {
            offset += length;

            if (offset == bytes.length) {
                os.write(bytes, 0, bytes.length);
                offset = 0;
            }
        }
        if (offset != 0) {
            os.write(bytes, 0, offset);
        }
    }

    public static byte[] drainStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            drainStream(is, baos);
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }
}
