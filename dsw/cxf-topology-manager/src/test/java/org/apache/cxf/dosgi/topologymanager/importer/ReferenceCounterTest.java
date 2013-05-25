package org.apache.cxf.dosgi.topologymanager.importer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReferenceCounterTest {

    @Test
    public void testCounter() {
        ReferenceCounter<String> counter = new ReferenceCounter<String>();
        assertEquals(-1, counter.remove("a"));
        assertEquals(-1, counter.remove("a"));
        assertEquals(1, counter.add("a"));
        assertEquals(2, counter.add("a"));
        assertEquals(3, counter.add("a"));
        assertEquals(2, counter.remove("a"));
        assertEquals(1, counter.remove("a"));
        assertEquals(2, counter.add("a"));
        assertEquals(1, counter.remove("a"));
        assertEquals(0, counter.remove("a"));
        assertEquals(-1, counter.remove("a"));
    }
}
