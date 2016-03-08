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
package org.apache.aries.rsa.provider.tcp;

import java.io.IOException;
import java.util.Map;

import org.apache.cxf.dosgi.dsw.api.Endpoint;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class TcpEndpoint implements Endpoint {
    private EndpointDescription epd;
    private TCPServer tcpServer;
    
    public TcpEndpoint(Object service, Map<String, Object> effectiveProperties) {
        Integer port = getInt(effectiveProperties, "port", 0);
        String localip = LocalHostUtil.getLocalIp();
        int numThreads = getInt(effectiveProperties, "numThreads", 10);
        tcpServer = new TCPServer(service, localip, port, numThreads);
        effectiveProperties.put(RemoteConstants.ENDPOINT_ID, "tcp://" + localip + ":" + tcpServer.getPort());
        effectiveProperties.put(RemoteConstants.SERVICE_EXPORTED_CONFIGS, "");
        this.epd = new EndpointDescription(effectiveProperties);
    }
    

    private Integer getInt(Map<String, Object> effectiveProperties, String key, int defaultValue) {
        String value = (String)effectiveProperties.get(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    @Override
    public EndpointDescription description() {
        return this.epd;
    }


    @Override
    public void close() throws IOException {
        tcpServer.close();
    }
}
