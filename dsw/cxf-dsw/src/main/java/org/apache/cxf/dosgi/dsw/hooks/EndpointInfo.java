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
package org.apache.cxf.dosgi.dsw.hooks;

import org.apache.cxf.endpoint.Server;
import org.osgi.framework.BundleContext;
import org.osgi.service.discovery.ServiceEndpointDescription;

public class EndpointInfo {
    
    private BundleContext bc;
    private ServiceEndpointDescription sd;
    private Server server;
    private boolean isPublished;
    
    public EndpointInfo(BundleContext bc,
                        ServiceEndpointDescription sd,
                        Server server,
                        boolean isPublished) {
        this.bc = bc;
        this.sd = sd;
        this.server = server;
        this.isPublished = isPublished;
    }
    
    public BundleContext getContext() {
        return bc;
    }
    
    public ServiceEndpointDescription getServiceDescription() {
        return sd;
    }
    
    public Server getServer() {
        return server;
    }
    
    public boolean isPublished() {
        return isPublished;
    }
    
    
}
