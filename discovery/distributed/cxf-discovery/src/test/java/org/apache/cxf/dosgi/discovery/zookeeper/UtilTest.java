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
    }
    
    public void testGetZooKeeperPath() {
        assertEquals(Util.PATH_PREFIX + "org/example/Test", 
            Util.getZooKeeperPath("org.example.Test"));
    }
}
