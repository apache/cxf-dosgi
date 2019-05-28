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
package org.apache.cxf.dosgi.itests.multi;

import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.InputStream;

import javax.inject.Inject;

import org.apache.cxf.dosgi.samples.soap.Task;
import org.apache.cxf.dosgi.samples.soap.TaskService;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;

/**
 * Creates a service outside OSGi, announces the service via the xml based discovery. Checks that the service
 * proxy is created by CXF DOSGi and can be called.
 */
@RunWith(PaxExam.class)
public class TestImportService extends AbstractDosgiTest {
    @Inject
    TaskService taskService;

    private Server server;

    @Configuration
    public static Option[] configure() {
        return new Option[] //
        {//
         basicTestOptions(), //
         taskServiceAPI(), //
         provision(importConfigBundle()), //
         // increase for debugging
         systemProperty("org.apache.cxf.dosgi.test.serviceWaitTimeout")
             .value(System.getProperty("org.apache.cxf.dosgi.test.serviceWaitTimeout", "200")),
        };
    }

    protected static InputStream importConfigBundle() {
        return TinyBundles.bundle() //
            .add("OSGI-INF/remote-service/remote-services.xml",
                 TestImportService.class.getResource("/rs-test1.xml")) //
            .set(Constants.BUNDLE_SYMBOLICNAME, "importConfig") //
            .build(TinyBundles.withBnd());
    }

    @Before
    public void createCXFService() {
        server = publishService();
    }

    @Test
    public void testClientConsumer() {
        Task task = taskService.get(1);
        Assert.assertEquals("test", task.getTitle());
    }

    @After
    public void stopCXFService() {
        server.stop();
    }

    private Server publishService() {
        System.out.println("Publishing service");
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(TaskService.class);
        factory.setAddress("/taskservice");
        factory.setServiceBean(new DummyTaskServiceImpl());
        return factory.create();

    }
}
