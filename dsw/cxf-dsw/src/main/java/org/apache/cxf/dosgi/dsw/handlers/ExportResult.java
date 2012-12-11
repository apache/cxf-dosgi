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
package org.apache.cxf.dosgi.dsw.handlers;

import java.util.Map;

import org.apache.cxf.endpoint.Server;

public class ExportResult {
    private final Map<String, Object> endpointProps;
    private final Server server;
    private final Exception exception;

    public ExportResult(Map<String, Object> endpointProps, Server server) {
        super();
        this.endpointProps = endpointProps;
        this.server = server;
        this.exception = null;
    }
    
    public ExportResult(Map<String, Object> endpointProps, Exception ex) {
        super();
        this.endpointProps = endpointProps;
        this.server = null;
        this.exception = ex;
    }

    public Map<String, Object> getEndpointProps() {
        return endpointProps;
    }

    public Server getServer() {
        return server;
    }

    public Exception getException() {
        return exception;
    }
    
    
}
