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
package org.apache.cxf.dosgi.dsw.handlers.rest.provider;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.aries.rsa.spi.Endpoint;
import org.apache.cxf.dosgi.common.httpservice.HttpServiceManager;
import org.apache.cxf.dosgi.common.intent.impl.IntentManagerImpl;
import org.apache.cxf.dosgi.dsw.handlers.rest.RsConstants;
import org.apache.cxf.dosgi.dsw.handlers.rest.RsProvider;
import org.apache.cxf.jaxrs.client.WebClient;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class RsProviderCustomTest {

    @Test
    public void testCustomProvider() throws Exception {
        RsProvider rsProvider = new RsProvider();
        HttpServiceManager httpServiceManager = new HttpServiceManager();
        Dictionary<String, Object> config = new Hashtable<>();
        httpServiceManager.initFromConfig(config);
        rsProvider.setHttpServiceManager(httpServiceManager);
        IntentManagerImpl intentManager = new IntentManagerImpl();
        addIntent(intentManager, "my", new MyWriter(), new MyReader());

        rsProvider.setIntentManager(intentManager);
        TaskServiceImpl taskService = new TaskServiceImpl();
        BundleContext callingContext = EasyMock.createMock(BundleContext.class);
        
        Map<String, Object> props = new HashMap<>();
        props.put(Constants.OBJECTCLASS, new String[]{TaskService.class.getName()});
        String serviceAddress = "http://localhost:9181/";
        props.put(RsConstants.RS_ADDRESS_PROPERTY, serviceAddress);
        props.put(RemoteConstants.SERVICE_EXPORTED_INTENTS, "my");
        Class<?>[] ifaces = new Class[]{TaskService.class};
        try (Endpoint endpoint = rsProvider.exportService(taskService, callingContext, props, ifaces)) {
            Assert.assertEquals(serviceAddress, endpoint.description().getId());
            Assert.assertEquals("my", endpoint.description().getIntents().iterator().next());

            List<Object> providers = Arrays.asList((Object)MyReader.class);
            Task task1 = WebClient.create(serviceAddress, providers).path("/task").get(Task.class);
            Assert.assertEquals("test", task1.getName());

            TaskService proxy = (TaskService)rsProvider.importEndpoint(TaskService.class.getClassLoader(),
                                                                       callingContext, ifaces,
                                                                       endpoint.description());
            Task task = proxy.getTask();
            Assert.assertEquals("test", task.getName());
        }
    }

    private void addIntent(IntentManagerImpl intentManager, String name, Object... intents) {
        Callable<List<Object>> provider = intentProvider(intents);
        intentManager.addIntent(provider, name);
    }

    private Callable<List<Object>> intentProvider(final Object... intents) {
        return new Callable<List<Object>>() {
            @Override
            public List<Object> call() throws Exception {
                return Arrays.asList(intents);
            }
        };
    }

}
