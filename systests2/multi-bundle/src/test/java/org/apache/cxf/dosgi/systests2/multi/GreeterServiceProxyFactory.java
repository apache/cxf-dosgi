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
package org.apache.cxf.dosgi.systests2.multi;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.frontend.ClientProxyFactoryBean;

public final class GreeterServiceProxyFactory {
    
    private GreeterServiceProxyFactory() {
    }

    protected static GreeterService createGreeterServiceProxy(String serviceUri) {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setServiceClass(GreeterService.class);
        factory.setAddress(serviceUri);
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());
        return (GreeterService)factory.create();
    }

}
