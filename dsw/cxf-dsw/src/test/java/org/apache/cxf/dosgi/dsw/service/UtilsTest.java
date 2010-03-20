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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Constants;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import org.junit.Test;

public class UtilsTest {

    @Test
    public void testNormalizeStringPlus() {

        String s1 = "s1";
        String s2 = "s2";
        String s3 = "s3";

        String[] sa = new String[] {
            s1, s2, s3
        };

        Collection<Object> sl = new ArrayList<Object>(4);
        sl.add(s1);
        sl.add(s2);
        sl.add(s3);
        sl.add(new Object()); // must be skipped

        assertEquals(null, Utils.normalizeStringPlus(new Object()));
        assertEquals(new String[] {
            s1
        }, Utils.normalizeStringPlus(s1));
        assertEquals(sa, Utils.normalizeStringPlus(sa));
        assertEquals(sa, Utils.normalizeStringPlus(sl));

    }

    @Test
    public void testOverlayProperties() {

        Properties original = new Properties();

        original.put("MyProp", "my value");
        original.put(Constants.OBJECTCLASS, "myClass");

        Properties copy = new Properties();
        copy.putAll(original);

        { // nothing should change here
            Properties overload = new Properties();
            Utils.overlayProperties(copy,overload);

            assertEquals(original.size(), copy.size());
            for (Object key : Collections.list(original.keys())) {
                assertEquals(original.get(key), copy.get(key));
            }
        }
        
        copy.clear();
        copy.putAll(original);
        
        { // a property should be added
            Properties overload = new Properties();
            overload.put("new", "prop");
            
            Utils.overlayProperties(copy,overload);

            assertEquals(original.size()+1, copy.size());
            for (Object key : Collections.list(original.keys())) {
                assertEquals(original.get(key), copy.get(key));
            }
            assertNotNull(overload.get("new"));
            assertEquals("prop",overload.get("new"));
        }
        
        copy.clear();
        copy.putAll(original);
        
        { // only one property should be added
            Properties overload = new Properties();
            overload.put("new", "prop");
            overload.put("NEW", "prop");
            
            Utils.overlayProperties(copy,overload);

            assertEquals(original.size()+1, copy.size());
            for (Object key : Collections.list(original.keys())) {
                assertEquals(original.get(key), copy.get(key));
            }
            assertNotNull(overload.get("new"));
            assertEquals("prop",overload.get("new"));
        }
        
        copy.clear();
        copy.putAll(original);
        
        { // nothing should change here
            Properties overload = new Properties();
            overload.put(Constants.OBJECTCLASS, "assd");
            overload.put(Constants.SERVICE_ID, "asasdasd");
            Utils.overlayProperties(copy,overload);

            assertEquals(original.size(), copy.size());
            for (Object key : Collections.list(original.keys())) {
                assertEquals(original.get(key), copy.get(key));
            }
        }
        
        copy.clear();
        copy.putAll(original);
        
        { // overwrite own prop
            Properties overload = new Properties();
            overload.put("MyProp", "newValue");
            Utils.overlayProperties(copy,overload);

            assertEquals(original.size(), copy.size());
            for (Object key : Collections.list(original.keys())) {
                if(!"MyProp".equals(key))
                    assertEquals(original.get(key), copy.get(key));
            }
            assertEquals("newValue",copy.get("MyProp"));
        }
        
        copy.clear();
        copy.putAll(original);
        
        { // overwrite own prop in different case
            Properties overload = new Properties();
            overload.put("MYPROP", "newValue");
            Utils.overlayProperties(copy,overload);

            assertEquals(original.size(), copy.size());
            for (Object key : Collections.list(original.keys())) {
                if(!"MyProp".equals(key))
                    assertEquals(original.get(key), copy.get(key));
            }
            assertEquals("newValue",copy.get("MyProp"));
        }

    }

}
