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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.jdom.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ServiceDecoratorImplTest extends TestCase {
    public void testServiceDecorator() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        ServiceDecoratorImpl sd = new ServiceDecoratorImpl(bc);
        
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.findEntries("OSGI-INF/remote-service", "*.xml", false)).andReturn(
            Collections.enumeration(Arrays.asList(getClass().getResource("/test-resources/sd.xml")))).anyTimes();
        EasyMock.replay(b);
    }
    
    public void testGetDecoratorElements() {
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.findEntries("OSGI-INF/remote-service", "*.xml", false)).andReturn(
            Collections.enumeration(Arrays.asList(getClass().getResource("/test-resources/sd.xml")))).anyTimes();
        EasyMock.replay(b);

        List<Element> elements = ServiceDecoratorImpl.getDecorationElements(b);
        assertEquals(1, elements.size());
        assertEquals("service-decoration", elements.get(0).getName());
        assertEquals("http://cxf.apache.org/xmlns/service-decoration/1.0.0", elements.get(0).getNamespaceURI());
    }
}
