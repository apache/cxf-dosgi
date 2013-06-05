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
package org.apache.cxf.dosgi.discovery.zookeeper;

import java.util.Arrays;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointListener;

public class UtilTest extends TestCase {

    public void testGetZooKeeperPath() {
        assertEquals(Util.PATH_PREFIX + '/' + "org/example/Test",
            Util.getZooKeeperPath("org.example.Test"));

        // used for the recursive discovery
        assertEquals(Util.PATH_PREFIX, Util.getZooKeeperPath(null));
        assertEquals(Util.PATH_PREFIX, Util.getZooKeeperPath(""));
    }

    @SuppressWarnings("unchecked")
    public void testGetStringPlusProperty() {
        String[] out = Util.getStringPlusProperty("MyString");
        assertEquals(1, out.length);
        assertEquals("MyString", out[0]);

        out = Util.getStringPlusProperty(new String[]{"MyString"});
        assertEquals(1, out.length);
        assertEquals("MyString", out[0]);

        out = Util.getStringPlusProperty(Arrays.asList("MyString"));
        assertEquals(1, out.length);
        assertEquals("MyString", out[0]);

        out = Util.getStringPlusProperty(Arrays.asList(1));
        assertEquals(0, out.length);

        out = Util.getStringPlusProperty(new Object());
        assertEquals(0, out.length);

        out = Util.getStringPlusProperty(null);
        assertEquals(0, out.length);
    }

    public void testRemoveEmpty() {
        String[] out = Util.removeEmpty(new String[0]);
        assertEquals(0, out.length);

        out = Util.removeEmpty(new String[] {null});
        assertEquals(0, out.length);

        out = Util.removeEmpty(new String[] {""});
        assertEquals(0, out.length);

        out = Util.removeEmpty(new String[] {"hi"});
        assertEquals(1, out.length);
        assertEquals("hi", out[0]);

        out = Util.removeEmpty(new String[] {"", "hi", null});
        assertEquals(1, out.length);
        assertEquals("hi", out[0]);

        out = Util.removeEmpty(new String[] {"hi", null, "", ""});
        assertEquals(1, out.length);
        assertEquals("hi", out[0]);

        out = Util.removeEmpty(new String[] {"", "hi", null, "", "", "bye", null});
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

        String[] ret = Util.getScopes(sref);

        c.verify();
        assertEquals(1, ret.length);
        assertEquals(scopes[0], ret[0]);
    }
}
