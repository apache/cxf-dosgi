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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import junit.framework.TestCase;

public class PropertyHelperTest extends TestCase {

    public void testMultiValuePropertyAsString() {
        assertEquals(Collections.singleton("hi"),
            PropertyHelper.getMultiValueProperty("hi"));
    }

    public void testMultiValuePropertyAsArray() {
        assertEquals(Arrays.asList("a", "b"),
                PropertyHelper.getMultiValueProperty(new String[] {"a", "b"}));
    }

    public void testMultiValuePropertyAsCollection() {
        List<String> list = new ArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");
        assertEquals(list, PropertyHelper.getMultiValueProperty(list));
    }

    public void testMultiValuePropertyNull() {
        assertTrue(PropertyHelper.getMultiValueProperty(null).isEmpty());
    }

    public void testGetProperty() {
        Map<String, Object> p = new HashMap<String, Object>();
        p.put(RemoteConstants.ENDPOINT_ID, "http://google.de");
        p.put("notAString", new Object());
        p.put(org.osgi.framework.Constants.OBJECTCLASS, new String[]{"my.class"});
        p.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[]{"my.config"});

        EndpointDescription endpoint = new EndpointDescription(p);

        assertNull(PropertyHelper.getProperty(endpoint.getProperties(), "unknownProp"));
        assertEquals(p.get(RemoteConstants.ENDPOINT_ID), 
                     PropertyHelper.getProperty(endpoint.getProperties(), RemoteConstants.ENDPOINT_ID));
        assertEquals(null, PropertyHelper.getProperty(endpoint.getProperties(), "notAString"));
    }
}
