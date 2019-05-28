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
package org.apache.cxf.dosgi.samples.ssl;

import static javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transport.http.HttpConduitConfig;
import org.apache.cxf.transport.http.HttpConduitFeature;
import org.osgi.service.component.annotations.Component;

/**
 * Configures the client side conduit to trust the server certificate and authenticate by using
 * a client certificate
 */
@Component //
(//
    property = "org.apache.cxf.dosgi.IntentName=ssl" //
)
public class SslIntent implements Callable<List<Object>> {

    private static final String CLIENT_PASSWORD = "password";

    @Override
    public List<Object> call() throws Exception {
        HttpConduitFeature conduitFeature = new HttpConduitFeature();
        HttpConduitConfig conduitConfig = new HttpConduitConfig();
        TLSClientParameters tls = new TLSClientParameters();
        String karafHome = System.getProperty("karaf.home");
        tls.setKeyManagers(keyManager(keystore(karafHome + "/etc/keystores/client.jks", CLIENT_PASSWORD),
                                      CLIENT_PASSWORD));
        tls.setTrustManagers(trustManager(keystore(karafHome + "/etc/keystores/client.jks", CLIENT_PASSWORD)));
        //tls.setTrustManagers(new TrustManager[]{new DefaultTrustManager()});
        HostnameVerifier verifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        tls.setHostnameVerifier(verifier);
        tls.setCertAlias("clientkey");
        tls.setDisableCNCheck(true);
        conduitConfig.setTlsClientParameters(tls);
        conduitFeature.setConduitConfig(conduitConfig);

        return Arrays.asList((Object)conduitFeature);
    }

    private TrustManager[] trustManager(KeyStore ks) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(ks);
        return factory.getTrustManagers();
    }

    private KeyManager[] keyManager(KeyStore ks, String keyPassword) throws Exception {
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(getDefaultAlgorithm());
        kmfactory.init(ks, keyPassword.toCharArray());
        return kmfactory.getKeyManagers();
    }

    private KeyStore keystore(String keystorePath, String storePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keystorePath), storePassword.toCharArray());
        return ks;
    }

}
