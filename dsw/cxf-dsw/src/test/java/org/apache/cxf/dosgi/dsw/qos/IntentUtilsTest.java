package org.apache.cxf.dosgi.dsw.qos;

import java.util.Arrays;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class IntentUtilsTest {
    
    public void testNoIntentMap() {
        Bundle b = EasyMock.createNiceMock(Bundle.class);
        EasyMock.replay(b);
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.replay(bc);
        
        Assert.assertNull(IntentUtils.readIntentMap(bc));
        Assert.assertNotNull(IntentUtils.getIntentMap(bc));
    }    
    
    public void testIntentsParsingAndFormatting() {
        String initial = "A SOAP_1.1 integrity";

        String[] expected = {"A", "SOAP_1.1", "integrity"};
        String[] actual = IntentUtils.parseIntents(initial);
        Assert.assertTrue(Arrays.equals(expected, actual));
        
        Assert.assertEquals(initial, IntentUtils.formatIntents(actual));
    }

}
