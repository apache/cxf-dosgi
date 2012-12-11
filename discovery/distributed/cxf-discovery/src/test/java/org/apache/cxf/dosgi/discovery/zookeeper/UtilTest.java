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
package org.apache.cxf.dosgi.discovery.zookeeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointListener;

public class UtilTest extends TestCase {
    
    public void testMultiValuePropertyAsString() {
        assertEquals(Collections.singleton("hi"), 
            Util.getMultiValueProperty("hi"));            
    }
    
    public void testMultiValuePropertyAsArray() {
        assertEquals(Arrays.asList("a", "b"), 
            Util.getMultiValueProperty(new String [] {"a", "b"}));
    }
    
    public void testMultiValuePropertyAsCollection() {
        List<String> list = new ArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");
        assertEquals(list, Util.getMultiValueProperty(list)); 
        
        assertEquals(Collections.emptySet(), Util.getMultiValueProperty(null));
        
    }
    
    public void testGetZooKeeperPath() {
        assertEquals(Util.PATH_PREFIX + '/' + "org/example/Test", 
            Util.getZooKeeperPath("org.example.Test"));
        
        // used for the recursive discovery
        assertEquals(Util.PATH_PREFIX, Util.getZooKeeperPath(null));
        assertEquals(Util.PATH_PREFIX, Util.getZooKeeperPath(""));
    }
    
    
    @SuppressWarnings("unchecked")
    public void testGetStringPlusProperty() {
        Object in = "MyString";
        String[] out = Util.getStringPlusProperty(in);
        assertEquals(1, out.length);
        assertEquals("MyString", out[0]);
        
        
        in = new String[]{"MyString"};
        out = Util.getStringPlusProperty(in);
        assertEquals(1, out.length);
        assertEquals("MyString", out[0]);
        
        in = new ArrayList<String>();
        ((List<String>)in).add("MyString");
        out = Util.getStringPlusProperty(in);
        assertEquals(1, out.length);
        assertEquals("MyString", out[0]);
        
        in = new Object();
        out = Util.getStringPlusProperty(in);
        assertEquals(0, out.length);
    }
    
    public void testGetScopes() {
        IMocksControl c = EasyMock.createNiceControl();
        
        String[] scopes = new String[]{"myScope=test", ""};
        
        ServiceReference sref = c.createMock(ServiceReference.class);
        EasyMock.expect(sref.getProperty(EasyMock.eq(EndpointListener.ENDPOINT_LISTENER_SCOPE)))
            .andReturn(scopes).anyTimes();
        
        c.replay();
        
        String[] ret = Util.getScopes(sref);
        
        c.verify();
        assertEquals(1, ret.length);
        assertEquals(scopes[0], ret[0]);
        
    }
}
