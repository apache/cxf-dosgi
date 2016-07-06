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
package org.apache.cxf.dosgi.dsw.handlers.ws;

public final class WsConstants {
    public static final String WS_CONFIG_TYPE = "org.apache.cxf.ws";
    public static final String WS_ADDRESS_PROPERTY = WS_CONFIG_TYPE + ".address";
    public static final String WS_PORT_PROPERTY = WS_CONFIG_TYPE + ".port";
    public static final String WS_HTTP_SERVICE_CONTEXT = WS_CONFIG_TYPE + ".httpservice.context";

    public static final String WS_FRONTEND_PROP_KEY = WS_CONFIG_TYPE + ".frontend";
    public static final String WS_FRONTEND_JAXWS = "jaxws";
    public static final String WS_FRONTEND_SIMPLE = "simple";

    public static final String WS_CONTEXT_PROPS_PROP_KEY = WS_CONFIG_TYPE + ".context.properties";

    public static final String WS_DATABINDING_PROP_KEY = WS_CONFIG_TYPE + ".databinding";
    public static final String WS_DATABINDING_BEAN_PROP_KEY = WS_DATABINDING_PROP_KEY + ".bean";
    public static final String WS_DATA_BINDING_JAXB = "jaxb";
    public static final String WS_DATA_BINDING_AEGIS = "aegis";

    public static final String WS_WSDL_SERVICE_NAMESPACE = WS_CONFIG_TYPE + ".service.ns";
    public static final String WS_WSDL_SERVICE_NAME = WS_CONFIG_TYPE + ".service.name";
    public static final String WS_WSDL_PORT_NAME = WS_CONFIG_TYPE + ".port.name";
    public static final String WS_WSDL_LOCATION = WS_CONFIG_TYPE + ".wsdl.location";

    private WsConstants() {
        // never constructed
    }
}
