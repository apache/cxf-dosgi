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

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.cxf.xmlns.service_decoration._1_0.AddPropertyType;
import org.apache.cxf.xmlns.service_decoration._1_0.MatchPropertyType;
import org.apache.cxf.xmlns.service_decoration._1_0.MatchType;
import org.apache.cxf.xmlns.service_decoration._1_0.ServiceDecorationType;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DecorationParserTest {

    @Test
    public void testGetDecoratorForSD() throws JAXBException, IOException {
        URL resource = getClass().getResource("/test-resources/sd.xml");
        List<ServiceDecorationType> elements = new DecorationParser().getDecorations(resource);
        assertEquals(1, elements.size());
        ServiceDecorationType decoration = elements.get(0);
        assertEquals(1, decoration.getMatch().size());
        MatchType match = decoration.getMatch().get(0);
        assertEquals("org.acme.foo.*", match.getInterface());
        assertEquals(1, match.getMatchProperty().size());
        MatchPropertyType matchProp = match.getMatchProperty().get(0);
        assertEquals("test.prop", matchProp.getName());
        assertEquals("xyz", matchProp.getValue());
        assertEquals(1, match.getAddProperty().size());
        AddPropertyType addProp = match.getAddProperty().get(0);
        assertEquals("test.too", addProp.getName());
        assertEquals("ahaha", addProp.getValue());
        assertEquals("java.lang.String", addProp.getType());
    }

    @Test
    public void testGetDecorationForNull() throws JAXBException, IOException {
        List<ServiceDecorationType> elements = new DecorationParser().getDecorations(null);
        Assert.assertEquals(0, elements.size());
    }
}
