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
package org.apache.cxf.dosgi.dsw.service;

import junit.framework.TestCase;

public class DistributionProviderImplTest extends TestCase {
    public void testDUMMY() {
        assertTrue(true);
    }
    
//    public void testExposedServices() {
//        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
//        EasyMock.replay(bc);
//        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(bc);
//        
//        assertEquals(0, dp.getExposedServices().size());
//        assertEquals(0, dp.getRemoteServices().size());
//        ServiceReference sr = new TestServiceReference();
//        ServiceReference sr2 = new TestServiceReference();
//        
//        dp.addExposedService(sr, null);
//        assertEquals(1, dp.getExposedServices().size());
//        assertEquals(0, dp.getRemoteServices().size());
//        assertSame(sr, dp.getExposedServices().iterator().next());
//
//        dp.addExposedService(sr, null);
//        assertEquals(1, dp.getExposedServices().size());
//        assertEquals(0, dp.getRemoteServices().size());
//        assertSame(sr, dp.getExposedServices().iterator().next());
//
//        dp.addExposedService(sr2, null);
//        assertEquals(2, dp.getExposedServices().size());        
//        assertEquals(0, dp.getRemoteServices().size());
//    }   
//    
//    public void testRemoteServices() {
//        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
//        EasyMock.replay(bc);
//        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(bc);
//        
//        assertEquals(0, dp.getExposedServices().size());
//        assertEquals(0, dp.getRemoteServices().size());        
//        ServiceReference sr = new TestServiceReference();
//        ServiceReference sr2 = new TestServiceReference();
//        
//        dp.addRemoteService(sr);
//        assertEquals(0, dp.getExposedServices().size());
//        assertEquals(1, dp.getRemoteServices().size());
//        assertSame(sr, dp.getRemoteServices().iterator().next());
//
//        dp.addRemoteService(sr);
//        assertEquals(0, dp.getExposedServices().size());
//        assertEquals(1, dp.getRemoteServices().size());
//        assertSame(sr, dp.getRemoteServices().iterator().next());
//
//        dp.addRemoteService(sr2);
//        assertEquals(0, dp.getExposedServices().size());        
//        assertEquals(2, dp.getRemoteServices().size());
//    }
//    
//    public void testPublicationProperties() {
//        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
//        EasyMock.replay(bc);
//        
//        RemoteServiceAdminCore dp = new RemoteServiceAdminCore(bc);
//        ServiceReference sr = new TestServiceReference();
//        ServiceReference sr2 = new TestServiceReference();
//        
//        assertNull(dp.getExposedProperties(sr));
//        
//        dp.addExposedService(sr, null);
//        Map<String, String> pp = new HashMap<String, String>();
//        pp.put("a", "b");
//        dp.addExposedService(sr2, pp);
//
//        assertEquals(0, dp.getExposedProperties(sr).size());
//        assertEquals(pp, dp.getExposedProperties(sr2));
//    }
//    
//    private static class TestServiceReference implements ServiceReference {
//        public Bundle getBundle() {
//            return null;
//        }
//
//        public Object getProperty(String arg0) {
//            return null;
//        }
//
//        public String[] getPropertyKeys() {
//            return null;
//        }
//
//        public Bundle[] getUsingBundles() {
//            return null;
//        }
//
//        public boolean isAssignableTo(Bundle arg0, String arg1) {
//            return false;
//        }        
//        
//        public int compareTo(Object o) {
//            return 0;
//        }
//    }
}
