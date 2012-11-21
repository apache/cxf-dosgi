package org.apache.cxf.dosgi.dsw.qos;

import java.util.Arrays;

import junit.framework.Assert;

public class IntentUtilsTest {
    
    public void testIntentsParsingAndFormatting() {
        String initial = "A SOAP_1.1 integrity";

        String[] expected = {"A", "SOAP_1.1", "integrity"};
        String[] actual = IntentUtils.parseIntents(initial);
        Assert.assertTrue(Arrays.equals(expected, actual));
        
        Assert.assertEquals(initial, IntentUtils.formatIntents(actual));
    }

}
