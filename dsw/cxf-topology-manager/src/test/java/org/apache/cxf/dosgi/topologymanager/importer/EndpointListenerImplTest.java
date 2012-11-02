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
package org.apache.cxf.dosgi.topologymanager.importer;

import java.util.Dictionary;
import java.util.List;

import org.apache.cxf.dosgi.topologymanager.importer.EndpointListenerManager;
import org.apache.cxf.dosgi.topologymanager.importer.TopologyManagerImport;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointListener;

import org.junit.Test;

import static org.junit.Assert.*;

public class EndpointListenerImplTest {
   
    int testCase = 0;

    @Test
    public void testScopeChange(){
        
        
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        TopologyManagerImport tm = EasyMock.createNiceMock(TopologyManagerImport.class);
        ServiceRegistration sr = EasyMock.createNiceMock(ServiceRegistration.class);
        
        // expect Listener registration
        EasyMock.expect(bc.registerService((String)EasyMock.anyObject(),EasyMock.anyObject() , (Dictionary)EasyMock.anyObject())).andReturn(sr).atLeastOnce();
        
        
        sr.setProperties((Dictionary)EasyMock.anyObject());
        
        
        // expect property changes based on later calls
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            
            public Object answer() throws Throwable {
                Object[] args =  EasyMock.getCurrentArguments();
                Dictionary props = (Dictionary)args[0];
                List<String> scope = (List<String>)props.get(EndpointListener.ENDPOINT_LISTENER_SCOPE);
                
                //System.out.println("Test: change scope -> case: "+testCase);
                //System.out.println(scope);
                
                switch (testCase) {
                case 1:
                    assertEquals(1,scope.size());
                    assertEquals("(a=b)",scope.get(0));
                    break;
                    
                case 2:
                    assertEquals(0,scope.size());
                    break;
                    
                    
                case 3:
                    assertEquals("adding entry to empty list failed",1,scope.size());
                    assertEquals("(a=b)",scope.get(0));
                    break;
                    
                case 4:
                    assertEquals("adding second entry failed",2,scope.size());
                    assertNotNull(scope.contains("(a=b)"));
                    assertNotNull(scope.contains("(c=d)"));
                    break;
                    
                case 5:
                    assertEquals("remove failed",1,scope.size());
                    assertEquals("(c=d)",scope.get(0));
                    break;
                    
                default:
                    assertTrue("This should not happen !", false);
                }
                
                
                
                
                return null;
            }
        }).atLeastOnce();
        
        
        EasyMock.replay(bc);
        EasyMock.replay(tm);
        EasyMock.replay(sr);
        
        EndpointListenerManager endpointListener = new EndpointListenerManager(bc, tm);
                
        endpointListener.start();

        
        testCase = 1;
        endpointListener.extendScope("(a=b)");        
        testCase = 2;
        endpointListener.reduceScope("(a=b)");
        
        testCase = 3;
        endpointListener.extendScope("(a=b)");
        testCase = 4;
        endpointListener.extendScope("(c=d)");
        testCase = 5;       
        endpointListener.reduceScope("(a=b)");
                
        endpointListener.stop();
        
        EasyMock.verify(bc);
        EasyMock.verify(tm);
        EasyMock.verify(sr);
    }
    
}
