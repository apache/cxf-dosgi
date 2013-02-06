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
package org.apache.cxf.dosgi.dsw.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.util.OsgiUtils;
import org.apache.cxf.dosgi.dsw.util.Utils;
import org.junit.Test;
import org.osgi.framework.Constants;


public class UtilsTest {

    @Test
    public void testSplitString() {
        String[] values = Utils.normalizeStringPlus("1, 2");
        assertEquals(2, values.length);
        assertEquals(values[0], "1");
        assertEquals(values[1], "2");
    }

    @Test
    public void testNormalizeStringPlus() {

        String s1 = "s1";
        String s2 = "s2";
        String s3 = "s3";

        String[] sa = new String[] {
            s1, s2, s3
        };

        Collection<Object> sl = new ArrayList<Object>(4);
        sl.add(s1);
        sl.add(s2);
        sl.add(s3);
        sl.add(new Object()); // must be skipped

        assertArrayEquals(null, Utils.normalizeStringPlus(new Object()));
        assertArrayEquals(new String[] {
            s1
        }, Utils.normalizeStringPlus(s1));
        assertArrayEquals(sa, Utils.normalizeStringPlus(sa));
        assertArrayEquals(sa, Utils.normalizeStringPlus(sl));

    }

    @Test
    public void testOverlayProperties() {

        Map<String, Object> original = new HashMap<String, Object>();

        original.put("MyProp", "my value");
        original.put(Constants.OBJECTCLASS, "myClass");

        Map<String, Object> copy = new HashMap<String, Object>();
        copy.putAll(original);

        // nothing should change here
        Map<String, Object> overload = new HashMap<String, Object>();
        OsgiUtils.overlayProperties(copy, overload);

        assertEquals(original.size(), copy.size());
        for (Object key : original.keySet()) {
            assertEquals(original.get(key), copy.get(key));
        }

        copy.clear();
        copy.putAll(original);

        // a property should be added
        overload = new HashMap<String, Object>();
        overload.put("new", "prop");

        OsgiUtils.overlayProperties(copy, overload);

        assertEquals(original.size() + 1, copy.size());
        for (Object key : original.keySet()) {
            assertEquals(original.get(key), copy.get(key));
        }
        assertNotNull(overload.get("new"));
        assertEquals("prop", overload.get("new"));

        copy.clear();
        copy.putAll(original);

        // only one property should be added
        overload = new HashMap<String, Object>();
        overload.put("new", "prop");
        overload.put("NEW", "prop");

        OsgiUtils.overlayProperties(copy, overload);

        assertEquals(original.size() + 1, copy.size());
        for (Object key : original.keySet()) {
            assertEquals(original.get(key), copy.get(key));
        }
        assertNotNull(overload.get("new"));
        assertEquals("prop", overload.get("new"));

        copy.clear();
        copy.putAll(original);

        // nothing should change here
        overload = new HashMap<String, Object>();
        overload.put(Constants.OBJECTCLASS, "assd");
        overload.put(Constants.SERVICE_ID, "asasdasd");
        OsgiUtils.overlayProperties(copy, overload);

        assertEquals(original.size(), copy.size());
        for (Object key : original.keySet()) {
            assertEquals(original.get(key), copy.get(key));
        }

        copy.clear();
        copy.putAll(original);

        // overwrite own prop
        overload = new HashMap<String, Object>();
        overload.put("MyProp", "newValue");
        OsgiUtils.overlayProperties(copy, overload);

        assertEquals(original.size(), copy.size());
        for (Object key : original.keySet()) {
            if (!"MyProp".equals(key)) {
                assertEquals(original.get(key), copy.get(key));
            }
        }
        assertEquals("newValue", copy.get("MyProp"));

        copy.clear();
        copy.putAll(original);

        // overwrite own prop in different case
        overload = new HashMap<String, Object>();
        overload.put("MYPROP", "newValue");
        OsgiUtils.overlayProperties(copy, overload);

        assertEquals(original.size(), copy.size());
        for (Object key : original.keySet()) {
            if (!"MyProp".equals(key)) {
                assertEquals(original.get(key), copy.get(key));
            }
        }
        assertEquals("newValue", copy.get("MyProp"));

    }

}
