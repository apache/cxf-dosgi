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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps intent names to intent objects
 * An intent object can be a Feature, a BindingConfiguration or a String
 * 
 * Also supports a default intent map. Custom intents can override the defaults 
 */
public class IntentMap extends ConcurrentHashMap<String, Object> {
    private static final long serialVersionUID = 2606460607920520767L;
    private Map<String, Object> defaultMap;
    
    public IntentMap() {
        this(new HashMap<String, Object>());
    }
    
    public IntentMap(Map<String, Object> defaultMap) {
        super();
        this.defaultMap = defaultMap;
        putAll(defaultMap);
    }
    
    @Override
    public Object put(String key, Object value) {
        Object result = super.put(key, value);
        synchronized (this) {
            notifyAll();
            return result;
        }
    }

    @Override
    public Object remove(Object key) {
        Object old = super.remove(key);
        if (defaultMap.containsKey(key)) {
            put((String)key, defaultMap.get(key));
        }
        return old;
    }
    
}
