package org.apache.cxf.dosgi.dsw.qos;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.ws.policy.WSPolicyFeature;
import org.apache.neethi.Policy;

public class DefaultIntentMapFactory {
    public IntentMap create() {
        Map<String, Object> intentMap = new HashMap<String, Object>();
        intentMap.put("addressing", getNonDecoupledAddressing());
        intentMap.put("logging", getLoggingFeature());
        Object soap11 = getSoapBinding(Soap11.getInstance());
        intentMap.put("SOAP", soap11);
        intentMap.put("SOAP.1_1", soap11);
        intentMap.put("SOAP.1_2", getSoapBinding(Soap12.getInstance()));
        intentMap.put("HTTP", "PROVIDED");

        IntentMap intentMap2 = new IntentMap();
        intentMap2.setIntents(intentMap );
        return intentMap2;
    }

    private Object getNonDecoupledAddressing() {
        Policy wsAddressing = new Policy();
        WSPolicyFeature wsPolicy = new WSPolicyFeature(wsAddressing );
        return wsPolicy;
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
