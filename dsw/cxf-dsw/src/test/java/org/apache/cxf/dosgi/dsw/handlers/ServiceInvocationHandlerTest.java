package org.apache.cxf.dosgi.dsw.handlers;

import java.lang.reflect.Method;
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

        assertEquals(true, 
                sih.invoke(null, OBJECT_METHODS.get("equals"), new Object [] {sih}));
        assertEquals(System.identityHashCode(sih), 
                sih.invoke(null, OBJECT_METHODS.get("hashCode"), new Object [] {}));
        assertEquals("somestring", 
                sih.invoke(null, OBJECT_METHODS.get("toString"), new Object [] {}));
        assertEquals(Arrays.asList("equals", "hashCode", "toString"), called);
//        assertEquals("This one used to throw an exception", sih, sih);
//        assertEquals(System.identityHashCode(sih), sih.hashCode());
//        assertEquals("somestring", sih.toString());
        
    }
}
