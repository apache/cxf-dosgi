package org.apache.cxf.dosgi.dsw.qos;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class IntentUtilsTest {
    private static final String INTENT_MAP_URL = "/OSGI-INF/cxf/intents/intent-map.xml";
    
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

    @Test
    public void testgetIntentMap() throws IOException, ClassNotFoundException, InvalidSyntaxException {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(INTENT_MAP_URL);
        IntentMap intentMap = (IntentMap) ctx.getBean("intentMap", IntentMap.class);
        Assert.assertEquals(6, intentMap.getIntents().size());
        
        IntentMap intentMap2 = new DefaultIntentMapFactory().create();
        Object soap11_1 = intentMap.getIntents().get("addressing");
        Object soap11_2 = intentMap2.getIntents().get("addressing");
    }
}
