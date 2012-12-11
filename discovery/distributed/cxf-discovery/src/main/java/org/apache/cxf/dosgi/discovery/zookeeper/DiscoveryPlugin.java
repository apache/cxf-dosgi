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

import java.util.Map;

/**
 * This interface allows transformation of service registration information before it is pushed into the ZooKeeper
 * discovery system.
 * It can be useful for situations where a host name or port number needs to be changed in cases where the host running
 * the service is known differently from the outside to what the local Java process thinks it is.
 * Extra service properties can also be added to the registration which can be useful to refine the remote service
 * lookup process. <p/>
 *
 * DiscoveryPlugins use the OSGi WhiteBoard pattern. To add one to the system, register an instance under this interface
 * with the OSGi Service Registry. All registered DiscoveryPlugin instances are visited and given a chance to 
 * process the information before it is pushed into ZooKeeper. <p/>
 *
 * Note that the changes made using this plugin do not modify the local service registration.
 *
 */
public interface DiscoveryPlugin {
    /**
     * Process service registration information. Plugins can change this information before it is published into the
     * ZooKeeper discovery system.
     *
     * @param mutableProperties A map of service registration properties. The map is mutable and any changes to the map
     * will be reflected in the ZooKeeper registration.
     * @param endpointKey The key under which the service is registered in ZooKeeper. This key typically has the
     * following format: hostname#port##context. While the actual value of this key is not actually used by the 
     * system (people can use it as a hint to understand where the service is located), the value <i>must</i> be
     * unique for all services of a given type.
     * @return The <tt>endpointKey</tt> value to be used. If there is no need to change this simply return the value 
     * of the <tt>endpointKey</tt> parameter.
     */
    String process(Map<String, Object> mutableProperties, String endpointKey);
}
