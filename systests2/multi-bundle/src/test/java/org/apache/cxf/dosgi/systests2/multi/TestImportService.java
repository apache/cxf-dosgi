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

import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.samples.greeter.GreeterData;
import org.apache.cxf.dosgi.samples.greeter.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.apache.cxf.dosgi.systests2.common.test1.GreeterDataImpl;
import org.apache.cxf.dosgi.systests2.common.test1.MyActivator;
import org.apache.cxf.dosgi.systests2.common.test1.MyServiceTracker;
import org.apache.cxf.dosgi.systests2.common.test1.StartServiceTracker;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

@RunWith(PaxExam.class)
public class TestImportService extends AbstractDosgiTest {

    @Inject
    BundleContext bundleContext;

    @Configuration
    public static Option[] configure() throws Exception {
        return new Option[] {
                MultiBundleTools.getDistro(),
                CoreOptions.junitBundles(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                mavenBundle().groupId("org.apache.cxf.dosgi.samples")
                    .artifactId("cxf-dosgi-ri-samples-greeter-interface").versionAsInProject(),
                provision(createServiceConsumerBundle()),
                // increase for debugging
                systemProperty("org.apache.cxf.dosgi.test.serviceWaitTimeout").value(
                        System.getProperty("org.apache.cxf.dosgi.test.serviceWaitTimeout", "200")),
                frameworkStartLevel(100),
                //CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
        };
    }

    protected static InputStream createServiceConsumerBundle() {
        return TinyBundles.bundle()
            .add(MyActivator.class)
            .add(MyServiceTracker.class)
            .add(StartServiceTracker.class)
            .add(GreeterDataImpl.class)
            .add("OSGI-INF/remote-service/remote-services.xml", TestImportService.class.getResource("/rs-test1.xml"))
            .set(Constants.BUNDLE_SYMBOLICNAME, "testClientBundle")
            .set(Constants.EXPORT_PACKAGE, "org.apache.cxf.dosgi.systests2.common.test1")
            .set(Constants.BUNDLE_ACTIVATOR, MyActivator.class.getName())
            .build(TinyBundles.withBnd());
    }

    @Test
    public void testClientConsumer() throws Exception {
        // This test tests the consumer side of Distributed OSGi. It works as follows:
        // 1. It creates a little test bundle on the fly and starts that in the framework
        //    (this happens in the configure() method above). The test bundle waits until its
        //    instructed to start doing stuff. It's give this instruction via a service that is
        //    registered by this test (the service is of type java.lang.Object and has testName=test1).
        // 2. The test manually creates a CXF server of the appropriate type (using ServerFactoryBean)
        // 3. It signals the client bundle by registering a service to start doing its work.
        //    This registers a ServiceTracker in the client bundle for the remote service that is created
        //    by the test in step 2. The client bundle knows about the address through the
        //    remote-services.xml file.
        // 4. The client bundle will invoke the remote service and record the results in a service that it
        //    registers in the Service Registry.
        // 5. The test waits for this service to appear and then checks the results which are available as
        //    a service property.

        // Set up a Server in the test
        Server server = null;
        try {
            server = publishTestGreeter();

            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("testName", "test1");
            bundleContext.registerService(Object.class.getName(), new Object(), props);

            // Wait for the service tracker in the test bundle to register a service with the test result
            @SuppressWarnings("rawtypes")
            ServiceReference ref = waitService(bundleContext, String.class, "(testResult=test1)", 20);
            Assert.assertEquals("HiOSGi;exception", ref.getProperty("result"));
        } finally {
            if (server != null) {
                server.stop();
            }
            
        }
    }

    private Server publishTestGreeter() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ServerFactoryBean.class.getClassLoader());
            ServerFactoryBean factory = new ServerFactoryBean();
            factory.setServiceClass(GreeterService.class);
            factory.setAddress("http://localhost:9191/grrr");
            factory.getServiceFactory().setDataBinding(new AegisDatabinding());
            factory.setServiceBean(new TestGreeter());
            return factory.create();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public static class TestGreeter implements GreeterService {

        public Map<GreetingPhrase, String> greetMe(String name) {
            Map<GreetingPhrase, String> m = new HashMap<GreetingPhrase, String>();
            GreetingPhrase gp = new GreetingPhrase("Hi");
            m.put(gp, name);
            return m;
        }

        public GreetingPhrase[] greetMe(GreeterData gd) throws GreeterException {
            throw new GreeterException("TestGreeter");
        }
    }
}
