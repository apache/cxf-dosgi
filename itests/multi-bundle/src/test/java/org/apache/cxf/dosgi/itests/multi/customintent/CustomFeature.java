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
package org.apache.cxf.dosgi.itests.multi.customintent;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomFeature extends AbstractFeature {
    Logger log = LoggerFactory.getLogger(CustomFeature.class);

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        log.info("Adding interceptor " + ChangeTitleInterceptor.class.getName());
        provider.getOutInterceptors().add(0, new ChangeTitleInterceptor());
        super.initializeProvider(provider, bus);
    }
}
