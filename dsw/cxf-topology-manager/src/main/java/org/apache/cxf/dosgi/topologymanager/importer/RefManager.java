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

import java.util.HashMap;
import java.util.Map;

/**
 * Manages reference counters to keys
 */
public class RefManager {
    Map<String, Integer> refMap = new HashMap<String, Integer>();

    /**
     * Increase reference count for this key
     * 
     * @return reference Counter
     */
    public int addReference(String key) {
        synchronized (refMap) {
            Integer refCount = refMap.get(key);
            if (refCount == null) {
                refCount = 0;
            }
            refMap.put(key, refCount++);
            return refCount;
        }
    }

    /**
     * Decrease Reference count for this key
     * 
     * @param key
     * @return
     */
    public int removeReference(String key) {
        synchronized (refMap) {
            Integer refCount = refMap.get(key);
            refCount--;
            if (refCount > 0) {
                refMap.put(key, refCount);
            } else {
                refMap.remove(key);
            }
            return refCount;
        }
    }
}
