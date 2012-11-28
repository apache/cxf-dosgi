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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.Feature;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;

public class IntentManagerImplTest {
    
    @Test
    public void testIntents() throws Exception {
        Map<String, Object> intents = new HashMap<String, Object>();
        intents.put("A", new TestFeature("A"));
        intents.put("SOAP", new TestFeature("SOAP"));
        final IntentMap intentMap = new IntentMap(intents);
        
        IMocksControl control = EasyMock.createNiceControl();
        List<Feature> features = new ArrayList<Feature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();

        IntentManager intentManager = new IntentManagerImpl(intentMap, 10000);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "A");
        //ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);

        List<String> effectiveIntents = Arrays.asList(intentManager.applyIntents(features, factory, props));
        assertEquals(Arrays.asList("A", "SOAP"), effectiveIntents);
    }    
    
    @Test
    public void testMultiIntents() {
        final IntentMap intentMap = new IntentMap(new DefaultIntentMapFactory().create());
        intentMap.put("confidentiality.message", new TestFeature("confidentiality.message"));
        intentMap.put("transactionality", new TestFeature("transactionality"));
        
        IMocksControl control = EasyMock.createNiceControl();
        List<Feature> features = new ArrayList<Feature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();

        IntentManager intentManager = new IntentManagerImpl(intentMap);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "transactionality confidentiality.message");
        //ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);

        List<String> effectiveIntents = Arrays.asList(intentManager.applyIntents(features, factory, props));
        assertTrue(effectiveIntents.contains("transactionality"));        
        assertTrue(effectiveIntents.contains("confidentiality.message"));        
    }
    
    @Test
    public void testFailedIntent() {
        Map<String, Object> intents = new HashMap<String, Object>();
        intents.put("A", new TestFeature("A"));
        final IntentMap intentMap = new IntentMap(intents);
                
        IMocksControl control = EasyMock.createNiceControl();
        List<Feature> features = new ArrayList<Feature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();
        
        IntentManager intentManager = new IntentManagerImpl(intentMap);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "A B");
        //        ServiceEndpointDescription sd = 
        //            new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);

        try {
            intentManager.applyIntents(features, factory, props);
            Assert.fail("applyIntents() should have thrown an exception as there was an unsatisfiable intent");
        } catch (IntentUnsatifiedException iue) {
            assertEquals("B", iue.getIntent());
        }
    }
 
    @Test
    public void testInferIntents() {
        Map<String, Object> intents = new HashMap<String, Object>();
        intents.put("SOAP", new TestFeature("SOAP"));
        intents.put("Prov", "PROVIDED");
        AbstractFeature feat1 = new TestFeature("feat1");
        intents.put("A", feat1);
        intents.put("A_alt", feat1);
        intents.put("B", new TestFeature("B"));
        final IntentMap intentMap = new IntentMap(intents);
        
        IMocksControl control = EasyMock.createNiceControl();
        List<Feature> features = new ArrayList<Feature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();

        IntentManager intentManager = new IntentManagerImpl(intentMap);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "A");
        //        ServiceEndpointDescription sd = 
        //            new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);
        
        List<String> effectiveIntents = Arrays.asList(intentManager.applyIntents(features, factory, props));
        assertEquals(4, effectiveIntents.size());
        assertTrue(effectiveIntents.contains("Prov"));
        assertTrue(effectiveIntents.contains("A"));
        assertTrue(effectiveIntents.contains("A_alt"));
    }
    
    @Test
    public void testDefaultBindingIntent() {        
        IMocksControl control = EasyMock.createNiceControl();

        Map<String, Object> intents = new HashMap<String, Object>();
        BindingConfiguration feat1 = control.createMock(BindingConfiguration.class);
        intents.put("A", new AbstractFeature() {});
        intents.put("SOAP", feat1);
        intents.put("SOAP.1_1", feat1);
        intents.put("SOAP.1_2", control.createMock(BindingConfiguration.class));
        final IntentMap intentMap = new IntentMap(intents);

        List<Feature> features = new ArrayList<Feature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();
        IntentManager intentManager = new IntentManagerImpl(intentMap);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "A");
        //        ServiceEndpointDescription sd = 
        //            new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);
        
        List<String> effectiveIntents = 
            Arrays.asList(intentManager.applyIntents(features, factory, props));
        assertEquals(3, effectiveIntents.size());
        assertTrue(effectiveIntents.contains("A"));
        assertTrue(effectiveIntents.contains("SOAP"));
        assertTrue(effectiveIntents.contains("SOAP.1_1"));
    }
    
    public void testExplicitBindingIntent() {
        IMocksControl control = EasyMock.createNiceControl();

        Map<String, Object> intents = new HashMap<String, Object>();
        BindingConfiguration feat1 = control.createMock(BindingConfiguration.class);
        intents.put("A", new AbstractFeature() {});
        intents.put("SOAP", feat1);
        intents.put("SOAP.1_1", feat1);
        intents.put("SOAP.1_2", control.createMock(BindingConfiguration.class));
        final IntentMap intentMap = new IntentMap(intents);

        List<Feature> features = new ArrayList<Feature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();

        IntentManager intentManager = new IntentManagerImpl(intentMap);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "A SOAP.1_2");
        //        ServiceEndpointDescription sd = 
        //            new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);
        
        List<String> effectiveIntents = 
            Arrays.asList(intentManager.applyIntents(features, factory, props));
        assertEquals(2, effectiveIntents.size());
        assertTrue(effectiveIntents.contains("A"));
        assertTrue(effectiveIntents.contains("SOAP.1_2"));        
    }
    
    public void testInheritMasterIntentMapDefault() {
        List<String> features = runTestInheritMasterIntentMap("A B");
        
        assertEquals(2, features.size());
        assertTrue(features.contains("appFeatureA"));
        assertTrue(features.contains("masterFeatureB"));
    }
    
    public void testInheritMasterIntentMap() {
        List<String> features = runTestInheritMasterIntentMap("A B");
        
        assertEquals(2, features.size());
        assertTrue(features.contains("appFeatureA"));
        assertTrue(features.contains("masterFeatureB"));
    }

    private List<String> runTestInheritMasterIntentMap(String requestedIntents) {
        Map<String, Object> masterIntents = new HashMap<String, Object>();
        masterIntents.put("A", new TestFeature("masterFeatureA"));
        masterIntents.put("B", new TestFeature("masterFeatureB"));
        final IntentMap intentMap = new IntentMap(masterIntents);
        intentMap.put("A", new TestFeature("appFeatureA"));

        IMocksControl control = EasyMock.createNiceControl();
        List<Feature> features = new ArrayList<Feature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", requestedIntents);
        //ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);
        
        IntentManagerImpl intentManager = new IntentManagerImpl(intentMap);
        intentManager.applyIntents(features, factory, props);

        List<String> featureNames = new ArrayList<String>();
        for (Feature f : features) {
            featureNames.add(f.toString());
        }
        return featureNames;
    }
    
    @Test
    public void testProvidedIntents() {
        Map<String, Object> masterIntents = new HashMap<String, Object>();
        masterIntents.put("SOAP", "SOAP");
        masterIntents.put("A", "Provided");
        masterIntents.put("B", "PROVIDED");
        final IntentMap intentMap = new IntentMap(masterIntents);

        IMocksControl control = EasyMock.createNiceControl();
        List<Feature> features = new ArrayList<Feature>();
        AbstractEndpointFactory factory = control.createMock(AbstractEndpointFactory.class);
        control.replay();
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("osgi.remote.requires.intents", "B A");
        //ServiceEndpointDescription sd = new ServiceEndpointDescriptionImpl(Arrays.asList(String.class.getName()), props);
        
        IntentManager intentManager = new IntentManagerImpl(intentMap);
        
        Set<String> effectiveIntents = new HashSet<String>(Arrays.asList( 
            intentManager.applyIntents(features, factory, props)));
        Set<String> expectedIntents = new HashSet<String>(Arrays.asList(new String [] {"A", "B", "SOAP"}));
        assertEquals(expectedIntents, effectiveIntents);
    }
    
    private static class TestFeature extends AbstractFeature {
        private final String name;
        
        private TestFeature(String n) {
            name = n;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
