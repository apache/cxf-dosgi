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
package org.apache.cxf.dosgi.dsw.qos;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.dosgi.dsw.Constants;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.remoteserviceadmin.RemoteConstants;


public class IntentUtilsTest {
    @Test
    public void testMergeArrays() {
        Assert.assertNull(IntentUtils.mergeArrays(null, null));

        String[] sa1 = {};
        Assert.assertEquals(0, IntentUtils.mergeArrays(sa1, null).length);

        String[] sa2 = {"X"};
        Assert.assertEquals(1, IntentUtils.mergeArrays(null, sa2).length);
        Assert.assertEquals("X", IntentUtils.mergeArrays(null, sa2)[0]);

        String[] sa3 = {"Y", "Z"};
        String[] sa4 = {"A", "Z"};
        Assert.assertEquals(3, IntentUtils.mergeArrays(sa3, sa4).length);
        Assert.assertEquals(new HashSet<String>(Arrays.asList("A", "Y", "Z")),
                new HashSet<String>(Arrays.asList(IntentUtils.mergeArrays(sa3, sa4))));
    }

    @Test
    public void testRequestedIntents() {
        Map<String, Object> props = new HashMap<String, Object>();
        Assert.assertEquals(0, IntentUtils.getRequestedIntents(props).size());

        props.put(RemoteConstants.SERVICE_EXPORTED_INTENTS, "one");
        Assert.assertEquals(Collections.singleton("one"), IntentUtils.getRequestedIntents(props));

        props.put(RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA, new String[] {"two", "three"});
        Set<String> expected1 = new HashSet<String>(Arrays.asList("one", "two", "three"));
        Assert.assertEquals(expected1, IntentUtils.getRequestedIntents(props));

        props.put(Constants.EXPORTED_INTENTS_OLD, "A B C");
        Set<String> expected2 = new HashSet<String>(Arrays.asList("one", "two", "three", "A", "B", "C"));
        Assert.assertEquals(expected2, IntentUtils.getRequestedIntents(props));
    }
}
