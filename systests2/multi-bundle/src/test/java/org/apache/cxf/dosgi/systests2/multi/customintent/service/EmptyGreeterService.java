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
package org.apache.cxf.dosgi.systests2.multi.customintent.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.dosgi.samples.greeter.GreeterData;
import org.apache.cxf.dosgi.samples.greeter.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;

public final class EmptyGreeterService implements GreeterService {
    /**
     * Return an empty array. Our custom intent should add a GreetingPhrase
     */
    public GreetingPhrase[] greetMe(GreeterData name) throws GreeterException {
        return new GreetingPhrase[]{};
    }

    public Map<GreetingPhrase, String> greetMe(String name) {
        HashMap<GreetingPhrase, String> result = new HashMap<GreetingPhrase, String>();
        return result;
    }
}