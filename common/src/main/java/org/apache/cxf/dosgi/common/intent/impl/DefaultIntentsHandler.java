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
package org.apache.cxf.dosgi.common.intent.impl;

import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.dosgi.common.intent.IntentHandler;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.feature.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultIntentsHandler implements IntentHandler {
    static final Logger LOG = LoggerFactory.getLogger(IntentManagerImpl.class);

    @Override
    public boolean apply(AbstractEndpointFactory factory, String intentName, Object intent) {
        String prefix = "Applying intent: " + intentName + " via ";
        if (intent instanceof DataBinding) {
            DataBinding dataBinding = (DataBinding) intent;
            LOG.info(prefix + "data binding: " + dataBinding);
            factory.setDataBinding(dataBinding);
            return true;
        } else if (intent instanceof BindingConfiguration) {
            BindingConfiguration bindingCfg = (BindingConfiguration)intent;
            LOG.info(prefix + "binding config: " + bindingCfg);
            factory.setBindingConfig(bindingCfg);
            return true;
        } else if (intent instanceof Feature) {
            Feature feature = (Feature)intent;
            LOG.info(prefix + "feature: " + feature);
            factory.getFeatures().add(feature);
            return true;
        } else {
            return false;
        }
    }

}
