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
package org.apache.cxf.dosgi.dsw.decorator;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceRule implements Rule {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceRule.class);
    
    private final Bundle bundle;
    private final Pattern matchPattern;
    private final Map<String, String> propMatches = new HashMap<String, String>();
    private final Map<String, Object> addProps = new HashMap<String, Object>();
    
    public InterfaceRule(Bundle b, String im) {
        bundle = b;
        matchPattern = Pattern.compile(im);
    }

    public synchronized void addPropMatch(String name, String value) {
        propMatches.put(name, value);
    }

    public synchronized void addProperty(String name, String value, String type) {
        Object obj = value;
        
        if (!String.class.getName().equals(type)) {
            try {
                Class<?> cls = getClass().getClassLoader().loadClass(type);
                Constructor<?> ctor = cls.getConstructor(new Class [] {String.class});
                obj = ctor.newInstance(value);
            } catch (Throwable th) {
                LOG.warn("Could not handle property '" + name +
                    "' with value '" + value + "' of type: " + type, th);
                return;
            }
        }
        
        addProps.put(name, obj);
    }

    public synchronized void apply(ServiceReference sref, Map<String, Object> target) {
        String [] objectClass = (String[]) sref.getProperty(Constants.OBJECTCLASS);
        boolean matches = false;
        for (String cls : objectClass) {            
            Matcher m = matchPattern.matcher(cls);
            if (m.matches()) {
                for (Map.Entry<String, String> pm : propMatches.entrySet()) {
                    Object value = sref.getProperty(pm.getKey());
                    if (value == null) {
                        return;
                    }
                    if (!Pattern.matches(pm.getValue(), value.toString())) {
                        return;
                    }
                }
                matches = true;
                break;
            }
        }
        if (!matches) {
            return;
        }
        
        LOG.info("Adding the following properties to " + sref + ": " + addProps);
        target.putAll(addProps);
    }

    public Bundle getBundle() {
        return bundle;
    }    
}
