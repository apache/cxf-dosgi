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
package org.apache.cxf.dosgi.discovery.zookeeper.util;

import java.util.Arrays;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointListener;

public class UtilsTest extends TestCase {

    public void testGetZooKeeperPath() {
        assertEquals(Utils.PATH_PREFIX + '/' + "org/example/Test",
            Utils.getZooKeeperPath("org.example.Test"));

        // used for the recursive discovery
        assertEquals(Utils.PATH_PREFIX, Utils.getZooKeeperPath(null));
        assertEquals(Utils.PATH_PREFIX, Utils.getZooKeeperPath(""));
    }

    public void testGetStringPlusProperty() {
        String[] out = Utils.getStringPlusProperty("MyString");
        assertEquals(1, out.length);
        assertEquals("MyString", out[0]);

        out = Utils.getStringPlusProperty(new String[]{"MyString"});
        assertEquals(1, out.length);
        assertEquals("MyString", out[0]);

        out = Utils.getStringPlusProperty(Arrays.asList("MyString"));
        assertEquals(1, out.length);
        assertEquals("MyString", out[0]);

        out = Utils.getStringPlusProperty(Arrays.asList(1));
        assertEquals(0, out.length);

        out = Utils.getStringPlusProperty(new Object());
        assertEquals(0, out.length);

        out = Utils.getStringPlusProperty(null);
        assertEquals(0, out.length);
    }

    public void testRemoveEmpty() {
        String[] out = Utils.removeEmpty(new String[0]);
        assertEquals(0, out.length);

        out = Utils.removeEmpty(new String[]{null});
        assertEquals(0, out.length);

        out = Utils.removeEmpty(new String[]{""});
        assertEquals(0, out.length);

        out = Utils.removeEmpty(new String[]{"hi"});
        assertEquals(1, out.length);
        assertEquals("hi", out[0]);

        out = Utils.removeEmpty(new String[]{"", "hi", null});
        assertEquals(1, out.length);
        assertEquals("hi", out[0]);

        out = Utils.removeEmpty(new String[]{"hi", null, "", ""});
        assertEquals(1, out.length);
        assertEquals("hi", out[0]);

        out = Utils.removeEmpty(new String[]{"", "hi", null, "", "", "bye", null});
        assertEquals(2, out.length);
        assertEquals("hi", out[0]);
        assertEquals("bye", out[1]);
    }

    public void testGetScopes() {
        IMocksControl c = EasyMock.createNiceControl();

        String[] scopes = new String[]{"myScope=test", ""};

        ServiceReference sref = c.createMock(ServiceReference.class);
        EasyMock.expect(sref.getProperty(EasyMock.eq(EndpointListener.ENDPOINT_LISTENER_SCOPE)))
            .andReturn(scopes).anyTimes();

        c.replay();

        String[] ret = Utils.getScopes(sref);

        c.verify();
        assertEquals(1, ret.length);
        assertEquals(scopes[0], ret[0]);
    }
}
