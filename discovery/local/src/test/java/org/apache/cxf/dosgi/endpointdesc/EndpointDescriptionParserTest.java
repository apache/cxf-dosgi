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

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.xmlns.rsa.v1_0.EndpointDescriptionType;

public class EndpointDescriptionParserTest {

    @Test
    public void testNoRemoteServicesXMLFiles() {
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.replay(b);

        List<EndpointDescriptionType> rsElements = new EndpointDescriptionBundleParser().getAllDescriptionElements(b);
        Assert.assertEquals(0, rsElements.size());
    }

    @Test
    public void testEndpointDescriptionsFromURL() throws IOException {
        URL ed1URL = getClass().getResource("/ed1.xml");
        List<EndpointDescriptionType> edElements = new EndpointDescriptionParser().
            getEndpointDescriptions(ed1URL.openStream());
        Assert.assertEquals(4, edElements.size());
    }
}
