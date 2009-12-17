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
package org.apache.cxf.dosgi.topologymanager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import org.junit.Test;

public class UtilsTest {

    @Test
    public void testNomalizeScopeForSingleString() {

        try {
            ServiceReference sr = EasyMock.createMock(ServiceReference.class);
            EasyMock.expect(sr.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE))
                .andReturn("Filterstring");

            Filter f = EasyMock.createNiceMock(Filter.class);
            
            BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
            EasyMock.expect(bc.createFilter((String)EasyMock.anyObject())).andReturn(f);

            EasyMock.replay(sr);
            EasyMock.replay(bc);

            List<Filter> res = Utils.normalizeScope(sr, bc);

            assertEquals(1, res.size());
            assertEquals(f, res.get(0));

            EasyMock.verify(sr);
            EasyMock.verify(bc);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }
    
    
    @Test
    public void testNomalizeScopeForStringArray() {

        try {
            
            String[] filterStrings = {"f1","f2","f3"};
            
            ServiceReference sr = EasyMock.createMock(ServiceReference.class);
            EasyMock.expect(sr.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE))
                .andReturn(filterStrings);

            Filter f = EasyMock.createNiceMock(Filter.class);
            
            
            BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
            EasyMock.expect(bc.createFilter((String)EasyMock.anyObject())).andReturn(f).times(filterStrings.length);

            EasyMock.replay(sr);
            EasyMock.replay(bc);

            List<Filter> res = Utils.normalizeScope(sr, bc);

            assertEquals(filterStrings.length, res.size());
            assertEquals(f, res.get(0));

            EasyMock.verify(sr);
            EasyMock.verify(bc);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }
    
    
    
    @Test
    public void testNomalizeScopeForCollection() {

        try {
            
            
            Collection<String> collection = new ArrayList<String>();
            collection.add("f1");
            collection.add("f2");
            collection.add("f3");
            
            ServiceReference sr = EasyMock.createMock(ServiceReference.class);
            EasyMock.expect(sr.getProperty(EndpointListener.ENDPOINT_LISTENER_SCOPE))
                .andReturn(collection);

            Filter f = EasyMock.createNiceMock(Filter.class);
            
            
            BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
            EasyMock.expect(bc.createFilter((String)EasyMock.anyObject())).andReturn(f).times(collection.size());

            EasyMock.replay(sr);
            EasyMock.replay(bc);

            List<Filter> res = Utils.normalizeScope(sr, bc);

            assertEquals(collection.size(), res.size());
            assertEquals(f, res.get(0));

            EasyMock.verify(sr);
            EasyMock.verify(bc);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }
    
    
    @Test
    public void testGetNewUUID(){
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getProperty(EasyMock.eq("org.osgi.framework.uuid"))).andReturn(null).atLeastOnce();
        EasyMock.replay(bc);
        String uuid = Utils.getUUID(bc);
        assertNotNull(uuid);
        
        assertEquals(System.getProperty("org.osgi.framework.uuid"),uuid );
        
        EasyMock.verify(bc);
    }
     
    
    
    @Test
    public void testGetExistingUUID(){
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getProperty(EasyMock.eq("org.osgi.framework.uuid"))).andReturn("MyUUID").atLeastOnce();
        EasyMock.replay(bc);
        String uuid = Utils.getUUID(bc);
        
        assertEquals("MyUUID",uuid );
        
        EasyMock.verify(bc);
    }

    
    @Test
    public void testUUIDFilterExtension() throws InvalidSyntaxException{
        String filter = "(a=b)";
        
        
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getProperty(EasyMock.eq("org.osgi.framework.uuid"))).andReturn("MyUUID").atLeastOnce();
        EasyMock.replay(bc);
        
        filter = Utils.extendFilter(filter, bc);
        
        Filter f = FrameworkUtil.createFilter(filter);
        
        Dictionary m = new Hashtable();
        m.put("a", "b");
        
        assertTrue(filter+" filter must match as uuid is missing",f.match(m));      
        m.put(RemoteConstants.ENDPOINT_FRAMEWORK_UUID , "MyUUID");
        assertFalse(filter+" filter must NOT match as uuid is the local one",f.match(m));
    }
}
