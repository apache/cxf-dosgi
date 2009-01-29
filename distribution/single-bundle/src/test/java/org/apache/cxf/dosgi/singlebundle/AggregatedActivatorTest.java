package org.apache.cxf.dosgi.singlebundle;

import java.util.Arrays;

import junit.framework.TestCase;

public class AggregatedActivatorTest extends TestCase {
    public void testReadResourcesFile() throws Exception {
        String[] expected = {
            "org.ops4j.pax.web.service.internal.Activator",
            "org.apache.cxf.dosgi.discovery.local.Activator",
            "org.apache.cxf.dosgi.dsw.Activator",
            "org.springframework.osgi.extender.internal.activator.ContextLoaderListener"};
        
        AggregatedActivator aa = new AggregatedActivator();
        assertEquals(Arrays.asList(expected), aa.getActivators());
    }
}
