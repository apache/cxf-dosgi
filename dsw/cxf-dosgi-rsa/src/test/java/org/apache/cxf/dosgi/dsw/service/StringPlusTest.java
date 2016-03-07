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

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class StringPlusTest {

    @Test
    public void testSplitString() {
        String[] values = StringPlus.normalize("1, 2");
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

        assertArrayEquals(null, StringPlus.normalize(new Object()));
        assertArrayEquals(new String[] {
            s1
        }, StringPlus.normalize(s1));
        assertArrayEquals(sa, StringPlus.normalize(sa));
        assertArrayEquals(sa, StringPlus.normalize(sl));
    }

}
