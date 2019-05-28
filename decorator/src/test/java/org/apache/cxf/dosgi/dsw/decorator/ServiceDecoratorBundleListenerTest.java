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
package org.apache.cxf.dosgi.dsw.decorator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;

import static org.junit.Assert.assertEquals;

public class ServiceDecoratorBundleListenerTest {

    @Test
    public void testBundleListener() {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.replay(bc);

        final List<String> called = new ArrayList<>();
        ServiceDecoratorImpl serviceDecorator = new ServiceDecoratorImpl() {
            @Override
            void addDecorations(Bundle bundle) {
                called.add("addDecorations");
            }

            @Override
            void removeDecorations(Bundle bundle) {
                called.add("removeDecorations");
            }
        };

        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.replay(b);

        ServiceDecoratorBundleListener listener = new ServiceDecoratorBundleListener(serviceDecorator);

        assertEquals("Precondition failed", 0, called.size());
        listener.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, b));
        assertEquals(0, called.size());

        listener.bundleChanged(new BundleEvent(BundleEvent.STARTED, b));
        assertEquals(Arrays.asList("addDecorations"), called);

        listener.bundleChanged(new BundleEvent(BundleEvent.STOPPING, b));
        assertEquals(Arrays.asList("addDecorations", "removeDecorations"), called);

    }
}
