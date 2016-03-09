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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages a reference count per key.
 *
 * @param <K> the key type
 */
public class ReferenceCounter<K> {

    private final ConcurrentMap<K, Integer> counts = new ConcurrentHashMap<K, Integer>();

    /**
     * Increases the reference count for the given key,
     * or sets it to 1 if the key has no existing count.
     *
     * @param key a key
     * @return the updated reference count
     */
    public int add(K key) {
        while (true) {
            Integer count = counts.get(key);
            if (count == null) {
                if (counts.putIfAbsent(key, 1) == null) {
                    return 1;
                }
            } else if (counts.replace(key, count, count + 1)) {
                return count + 1;
            }
        }
    }

    /**
     * Decreases the reference count for the given key,
     * and removes it if it reaches 0.
     * If the key has no existing count, -1 is returned.
     *
     * @param key a key
     * @return the updated reference count, or -1 if the key has no existing count
     */
    public int remove(K key) {
        while (true) {
            Integer count = counts.get(key);
            if (count == null) {
                return -1;
            }
            if (count == 1) {
                if (counts.remove(key, 1)) {
                    return 0;
                }
            } else if (counts.replace(key, count, count - 1)) {
                return count - 1;
            }
        }
    }
}
