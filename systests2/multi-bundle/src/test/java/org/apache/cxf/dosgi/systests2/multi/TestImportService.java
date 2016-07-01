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
package org.apache.cxf.dosgi.systests2.multi;

import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.apache.cxf.dosgi.systests2.multi.importservice.SimpleGreeter;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
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

@RunWith(PaxExam.class)
public class TestImportService extends AbstractDosgiTest {
    @Inject
    GreeterService greeterService;
    private Server server;

    @Configuration
    public static Option[] configure() throws Exception {
        return new Option[] //
        {//
         basicTestOptions(), //
         greeterInterface(), //
         provision(createServiceConsumerBundle()), //
         // increase for debugging
         systemProperty("org.apache.cxf.dosgi.test.serviceWaitTimeout")
             .value(System.getProperty("org.apache.cxf.dosgi.test.serviceWaitTimeout", "200")),
        };
    }

    protected static InputStream createServiceConsumerBundle() {
        return TinyBundles.bundle() //
            .add("OSGI-INF/remote-service/remote-services.xml",
                 TestImportService.class.getResource("/rs-test1.xml")) //
            .set(Constants.BUNDLE_SYMBOLICNAME, "importConfig") //
            .build(TinyBundles.withBnd());
    }

    @Before
    public void createCXFService() {
        server = publishTestGreeter();
    }

    @Test
    public void testClientConsumer() throws Exception {
        Map<GreetingPhrase, String> result = greeterService.greetMe("OSGi");
        GreetingPhrase phrase = result.keySet().iterator().next();
        Assert.assertEquals("Hi", phrase.getPhrase());
    }

    @After
    public void stopCXFService() {
        server.stop();
    }

    private Server publishTestGreeter() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ServerFactoryBean.class.getClassLoader());
            ServerFactoryBean factory = new ServerFactoryBean();
            factory.setServiceClass(GreeterService.class);
            factory.setAddress("http://localhost:9191/grrr");
            factory.getServiceFactory().setDataBinding(new AegisDatabinding());
            factory.setServiceBean(new SimpleGreeter());
            return factory.create();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}
