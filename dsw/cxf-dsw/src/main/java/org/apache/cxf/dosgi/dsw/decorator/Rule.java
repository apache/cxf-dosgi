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

import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public interface Rule {
    /** When the ServiceReference passed in matches the rule's condition,
     * set the additional properties in the target.
     * @param sref The Service Reference to be checked.
     * @param target Any additional properties are to be set in this map.
     */
    void apply(ServiceReference sref, Map<String, Object> target);
    
    /** Returns the bundle that provided this rule.
     * @return The Bundle where the Rule was defined.
     */
    Bundle getBundle();
}
