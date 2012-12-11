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
package org.apache.cxf.dosgi.dsw.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class ClassUtilsTest extends TestCase {

    public void testGetInterfaceClass() {
        assertEquals(String.class, ClassUtils.getInterfaceClass("Hello", "java.lang.String"));
        assertNull(ClassUtils.getInterfaceClass("Hello", "java.lang.Integer"));
        assertEquals(List.class, ClassUtils.getInterfaceClass(new ArrayList<String>(), "java.util.List"));
        assertEquals(Collection.class, ClassUtils.getInterfaceClass(new ArrayList<String>(), "java.util.Collection"));
    }
    
    public void testGetInterfaceClassFromSubclass() {
        assertEquals(Map.class, ClassUtils.getInterfaceClass(new MySubclassFour(), "java.util.Map"));
        assertNull(ClassUtils.getInterfaceClass(new MySubclassFour(), "java.util.UnknownType"));
    }

    @SuppressWarnings({ "serial", "rawtypes" })
    private static class MyMapSubclass extends HashMap {
    }

    @SuppressWarnings("serial")
    static class MySubclassOne extends MyMapSubclass {
    }

    @SuppressWarnings("serial")
    static class MySubclassTwo extends MySubclassOne {
    }

    @SuppressWarnings("serial")
    static class MySubclassThree extends MySubclassTwo {
    }

    @SuppressWarnings("serial")
    static class MySubclassFour extends MySubclassThree {
    }
}
