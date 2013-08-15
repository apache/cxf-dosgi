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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.dosgi.discovery.local.util.Utils;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.xmlns.rsa.v1_0.EndpointDescriptionType;
import org.osgi.xmlns.rsa.v1_0.PropertyType;

public class PropertiesMapperTest {
    private static final String LF = "\n";

    @Test
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

        List<PropertyType> props = new PropertiesMapper().fromProps(m);
        EndpointDescriptionType epd = new EndpointDescriptionType();
        epd.getProperty().addAll(props);
        byte[] epData = new EndpointDescriptionParser().getData(epd);
        String actual = new String(epData, Charset.defaultCharset());

        URL edURL = getClass().getResource("/ed2-generated.xml");
        String expected = new String(drainStream(edURL.openStream()));
        Assert.assertEquals(Utils.normXML(expected), Utils.normXML(actual));
    }



    private static void drainStream(InputStream is, OutputStream os) throws IOException {
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

    private static byte[] drainStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            drainStream(is, baos);
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }
}
