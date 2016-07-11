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
package org.apache.cxf.dosgi.dsw.handlers.rest;

import java.util.List;

import org.apache.cxf.dosgi.common.intent.IntentHandler;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.jaxrs.AbstractJAXRSFactoryBean;

public class ProviderIntentHandler implements IntentHandler {

    @SuppressWarnings({
     "rawtypes", "unchecked"
    })
    @Override
    public boolean apply(AbstractEndpointFactory factory, String intentName, Object intent) {
        if (!(factory instanceof AbstractJAXRSFactoryBean)) {
            throw new RuntimeException("RsIntentHandler only works on JAXRS factory");
        }
        AbstractJAXRSFactoryBean jaxrsFactory = (AbstractJAXRSFactoryBean)factory;
        List providers = jaxrsFactory.getProviders();
        providers.add(intent);
        return true;
    }

}
