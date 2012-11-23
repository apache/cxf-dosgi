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
package org.apache.cxf.dosgi.dsw.qos;

import static org.easymock.EasyMock.expect;
import junit.framework.Assert;

import org.apache.cxf.dosgi.dsw.Constants;
import org.apache.cxf.feature.AbstractFeature;
import org.easymock.Capture;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class IntentTrackerTest {
    private static final String MY_INTENT_NAME = "myIntent";

    @Test
    public void testIntentAsService() throws InvalidSyntaxException {
        IMocksControl c = EasyMock.createControl();
        BundleContext bc = c.createMock(BundleContext.class);
        Filter filter = c.createMock(Filter.class);
        expect(bc.createFilter(EasyMock.<String>anyObject())).andReturn(filter);
        expect(bc.getProperty(org.osgi.framework.Constants.FRAMEWORK_VERSION)).andReturn("1.6.0");
        final Capture<ServiceListener> capturedListener = new Capture<ServiceListener>();
        bc.addServiceListener(EasyMock.capture(capturedListener), EasyMock.<String>anyObject());
        EasyMock.expectLastCall().atLeastOnce();
        expect(bc.getServiceReferences(EasyMock.<String>anyObject(), EasyMock.<String>anyObject())).andReturn(new ServiceReference[]{});
        IntentMap intentMap = new IntentMap();

        // Create a custom intent
        ServiceReference reference = c.createMock(ServiceReference.class);
        expect(reference.getProperty(Constants.INTENT_NAME_PROP)).andReturn(MY_INTENT_NAME);
        AbstractFeature testIntent = new AbstractFeature() {};
        expect(bc.getService(reference)).andReturn(testIntent).atLeastOnce();

        c.replay();

        IntentTracker tracker = new IntentTracker(bc, intentMap);
        tracker.open();
        
        Assert.assertFalse("IntentMap should not contain " + MY_INTENT_NAME, intentMap.containsKey(MY_INTENT_NAME));
        ServiceListener listener = capturedListener.getValue();
        
        // Simulate adding custom intent service
        ServiceEvent event = new ServiceEvent(ServiceEvent.REGISTERED, reference);
        listener.serviceChanged(event);
        
        // our custom intent should now be available
        Assert.assertTrue("IntentMap should contain " + MY_INTENT_NAME, intentMap.containsKey(MY_INTENT_NAME));
        Assert.assertEquals(testIntent, intentMap.get(MY_INTENT_NAME));
        
        c.verify();
        
    }
}
