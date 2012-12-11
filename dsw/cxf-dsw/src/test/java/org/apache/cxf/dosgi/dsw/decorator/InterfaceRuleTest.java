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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class InterfaceRuleTest extends TestCase {
    
    public void testDUMMY() {
        assertTrue(true);
    }
    
    public void testInterfaceRuleGetBundle() {
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.replay(b);
        InterfaceRule ir = new InterfaceRule(b, "org.apache.Foo");
        assertSame(b, ir.getBundle());
    }
    
    public void testInterfaceRule1() {
        InterfaceRule ir = new InterfaceRule(null, "org.apache.Foo");
        ir.addProperty("x", "y", String.class.getName());        
        
        final Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"a.b.C", "org.apache.Foo"});
        ServiceReference sref = mockServiceReference(serviceProps);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("a", "b");
        ir.apply(sref, m);
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("a", "b");
        expected.put("x", "y");
        assertEquals(expected, m);
    }
    
    public void testInterfaceRule2() {
        InterfaceRule ir = new InterfaceRule(null, "org.apache.F(.*)");
        ir.addPropMatch("boo", "baah");
        ir.addProperty("x", "1", Integer.class.getName());        
        ir.addProperty("aaa.bbb", "true", Boolean.class.getName());        
        
        final Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put("boo", "baah");
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"a.b.C", "org.apache.Foo"});
        ServiceReference sref = mockServiceReference(serviceProps);

        Map<String, Object> m = new HashMap<String, Object>();
        ir.apply(sref, m);
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("x", new Integer(1));
        expected.put("aaa.bbb", Boolean.TRUE);
        assertEquals(expected, m);
    }

    public void testInterfaceRule3() {
        InterfaceRule ir = new InterfaceRule(null, "org.apache.F(.*)");
        ir.addProperty("x", "y", String.class.getName());        
        
        final Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put("boo", "baah");
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.apache.Boo"});
        ServiceReference sref = mockServiceReference(serviceProps);

        Map<String, Object> m = new HashMap<String, Object>();
        ir.apply(sref, m);
        assertEquals(0, m.size());
    }

    public void testInterfaceRule4() {
        InterfaceRule ir = new InterfaceRule(null, "org.apache.F(.*)");
        ir.addPropMatch("boo", "baah");
        ir.addProperty("x", "y", String.class.getName());        
        
        final Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.apache.Foo"});
        ServiceReference sref = mockServiceReference(serviceProps);

        Map<String, Object> m = new HashMap<String, Object>();
        ir.apply(sref, m);
        assertEquals(0, m.size());
    }

    public void testInterfaceRule5() {
        InterfaceRule ir = new InterfaceRule(null, "org.apache.Foo");
        ir.addPropMatch("test.int", "42");
        ir.addProperty("x", "1", Long.class.getName());        
        
        final Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put("test.int", new Integer(42));
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.apache.Foo"});
        ServiceReference sref = mockServiceReference(serviceProps);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("x", "foo");
        m.put("aaa.bbb", Boolean.TRUE);
        ir.apply(sref, m);
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("x", new Long(1));
        expected.put("aaa.bbb", Boolean.TRUE);
        assertEquals(expected, m);
    }
    
    public void testInterfaceRule6() {
        InterfaceRule ir = new InterfaceRule(null, "org.apache.Foo");
        ir.addPropMatch("test.int", "42");
        ir.addProperty("x", "1", Long.class.getName());        
        
        final Map<String, Object> serviceProps = new HashMap<String, Object>();
        serviceProps.put("test.int", new Integer(51));
        serviceProps.put(Constants.OBJECTCLASS, new String [] {"org.apache.Foo"});
        ServiceReference sref = mockServiceReference(serviceProps);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("x", "foo");
        m.put("aaa.bbb", Boolean.TRUE);
        ir.apply(sref, m);
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("x", "foo");
        expected.put("aaa.bbb", Boolean.TRUE);
        assertEquals(expected, m);
    }

    private ServiceReference mockServiceReference(final Map<String, Object> serviceProps) {
        ServiceReference sref = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(sref.getProperty((String) EasyMock.anyObject())).andAnswer(new IAnswer<Object>() {            
            public Object answer() throws Throwable {
                return serviceProps.get(EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.expect(sref.getPropertyKeys()).
            andReturn(serviceProps.keySet().toArray(new String [] {})).anyTimes();
        
        EasyMock.replay(sref);
        return sref;
    }
}
