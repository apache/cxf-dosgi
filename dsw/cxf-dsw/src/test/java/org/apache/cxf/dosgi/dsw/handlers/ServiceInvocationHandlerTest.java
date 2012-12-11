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
package org.apache.cxf.dosgi.dsw.handlers;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class ServiceInvocationHandlerTest extends TestCase {
    private static final Map<String, Method> OBJECT_METHODS = new HashMap<String, Method>(); {
        for (Method m : Object.class.getMethods()) {
            OBJECT_METHODS.put(m.getName(), m);
        }
    }
    
    public void testInvoke() throws Throwable {
        ServiceInvocationHandler sih = new ServiceInvocationHandler("hello", String.class);
        Method m = String.class.getMethod("length", new Class [] {});
        assertEquals(5, sih.invoke(null, m, new Object [] {}));
    }
    
    public void testInvokeObjectMethod() throws Throwable {
        final List<String> called = new ArrayList<String>();
        ServiceInvocationHandler sih = new ServiceInvocationHandler("hi", String.class) {
            public boolean equals(Object obj) {
                called.add("equals");
                return super.equals(obj);
            }

            public int hashCode() {
                called.add("hashCode");
                return super.hashCode();
            }

            public String toString() {
                called.add("toString");
                return "somestring";
            }            
        };

        Object proxy = Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class [] {Runnable.class}, sih);                
        
        assertEquals(true, 
                sih.invoke(null, OBJECT_METHODS.get("equals"), new Object [] {proxy}));
        assertEquals(System.identityHashCode(sih), 
                sih.invoke(null, OBJECT_METHODS.get("hashCode"), new Object [] {}));
        assertEquals("somestring", 
                sih.invoke(null, OBJECT_METHODS.get("toString"), new Object [] {}));
        assertEquals(Arrays.asList("equals", "hashCode", "toString"), called);        
    }
}
