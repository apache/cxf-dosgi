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
package org.apache.cxf.dosgi.samples.greeter.impl2.rest;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.dosgi.samples.greeter.rest.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.rest.GreeterInfo;
import org.apache.cxf.dosgi.samples.greeter.rest.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.rest.GreetingPhrase;

public class GreeterServiceImpl2 implements GreeterService {

    private final static String STRANGER_NAME = "Stranger";

    public GreeterInfo greetMe(String name) throws GreeterException {
        System.out.println("Invoking from GreeterServiceImpl2: greetMe(" + name + ")");
        
        if (name.equals(STRANGER_NAME)) {
            throw new GreeterException(name);
        }

        GreeterInfo info = new GreeterInfo();
        List<GreetingPhrase> list = new ArrayList<GreetingPhrase>();
        list.add(new GreetingPhrase("Hello", name));
        list.add(new GreetingPhrase("Hoi", name));
        list.add(new GreetingPhrase("Hola", name));
        list.add(new GreetingPhrase("Bonjour", name));
        info.setGreetings(list);
        return info;
    }

}
