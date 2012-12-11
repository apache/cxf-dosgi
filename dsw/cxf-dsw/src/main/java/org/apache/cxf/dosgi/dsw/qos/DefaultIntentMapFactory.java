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
package org.apache.cxf.dosgi.dsw.qos;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.feature.LoggingFeature;

public class DefaultIntentMapFactory {
    public Map<String, Object> create() {
        Map<String, Object> intentMap = new HashMap<String, Object>();
        intentMap.put("logging", getLoggingFeature());
        Object soap11 = getSoapBinding(Soap11.getInstance());
        intentMap.put("SOAP", soap11);
        intentMap.put("SOAP.1_1", soap11);
        intentMap.put("SOAP.1_2", getSoapBinding(Soap12.getInstance()));
        intentMap.put("HTTP", "PROVIDED");
        return intentMap;
    }

    private Object getLoggingFeature() {
        return new LoggingFeature();
    }

    private Object getSoapBinding(SoapVersion soapVersion) {
        SoapBindingConfiguration soapBindingConfig = new SoapBindingConfiguration();
        soapBindingConfig.setVersion(soapVersion);
        return soapBindingConfig;
    }

}
