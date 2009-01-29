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
package org.apache.cxf.dosgi.dsw;

public class Constants {
    public static final String INTENTS_PROPERTY = "osgi.intents";
    
    public static final String REMOTE_PROPERTY_PREFIX = "osgi.remote";
    
    public static final String REMOTE_INTERFACES_PROPERTY = REMOTE_PROPERTY_PREFIX + ".interfaces";
    public static final String REQUIRES_INTENTS_PROPERTY = REMOTE_PROPERTY_PREFIX + ".requires.intents";
    public static final String CONFIGURATION_PROPERTY = ".configuration";

    public static final String CONFIG_TYPE_PROPERTY = 
        REMOTE_PROPERTY_PREFIX + CONFIGURATION_PROPERTY + ".type";
    
    // TODO : these config type properties should better be enums ? 
    
    // SCA
    public static final String SCA_CONFIG_TYPE = "sca";
    public static final String SCA_CONFIG_PREFIX = 
        REMOTE_PROPERTY_PREFIX + CONFIGURATION_PROPERTY + ".sca";
    public static final String SCA_REMOTE_BINDINGS = SCA_CONFIG_PREFIX + ".bindings";
    public static final String SCA_REMOTE_POLICIES = SCA_CONFIG_PREFIX + ".policies";
    
    // WSDL
    public static final String WSDL_CONFIG_TYPE = "wsdl";
    public static final String WSDL_CONFIG_PREFIX = 
        REMOTE_PROPERTY_PREFIX + CONFIGURATION_PROPERTY + "." + WSDL_CONFIG_TYPE;
    public static final String SERVICE_NAMESPACE = WSDL_CONFIG_PREFIX + ".service.ns";
    
    // POJO
    public static final String POJO_CONFIG_TYPE = "pojo";
    public static final String POJO_CONFIG_PREFIX = 
        REMOTE_PROPERTY_PREFIX + CONFIGURATION_PROPERTY + "." + POJO_CONFIG_TYPE;
    public static final String POJO_ADDRESS_PROPERTY = POJO_CONFIG_PREFIX + ".address";
    public static final String POJO_HTTP_SERVICE_CONTEXT = POJO_CONFIG_PREFIX + ".httpservice.context"; 
        
    // Common Configuration Properties
    public static final String CHECK_BUNDLE = "check.bundle";
    public static final String DEFAULT_PORT_CONFIG = "default.port";
    public static final String DEFAULT_HOST_CONFIG = "default.host";
    public static final String DEFAULT_PORT_VALUE = "9000";
    public static final String DEFAULT_HOST_VALUE = "localhost";
    public final static String USE_MASTER_MAP = "use.master.map";
    
    // DSW Identification - TODO do we really need this one?
    public static final String DSW_CLIENT_ID = "org.apache.cxf.remote.dsw.client";
}
