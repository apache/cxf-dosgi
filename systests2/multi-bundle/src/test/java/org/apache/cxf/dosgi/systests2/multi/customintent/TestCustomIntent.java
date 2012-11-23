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
package org.apache.cxf.dosgi.systests2.multi.customintent;

import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.apache.cxf.dosgi.systests2.common.AbstractTestExportService;
import org.apache.cxf.dosgi.systests2.multi.MultiBundleTools;
import org.apache.cxf.dosgi.systests2.multi.customintent.service.EmptyGreeterService;
import org.apache.cxf.dosgi.systests2.multi.customintent.service.GreeterServiceWithCustomIntentActivator;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@RunWith(JUnit4TestRunner.class)
public class TestCustomIntent extends AbstractTestExportService {
    @Inject
    BundleContext bundleContext;

    protected static InputStream getCustomIntentBundle() {
        return TinyBundles.newBundle()
                .add(CustomIntentActivator.class)
                .add(CustomFeature.class)
                .add(AddGreetingPhraseInterceptor.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "CustomIntent")
                .set(Constants.BUNDLE_ACTIVATOR, CustomIntentActivator.class.getName()).build(TinyBundles.withBnd());
    }

    protected static InputStream getServiceBundle() {
        return TinyBundles.newBundle()
                .add(GreeterServiceWithCustomIntentActivator.class)
                .add(EmptyGreeterService.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "EmptyGreeterService")
                .set(Constants.BUNDLE_ACTIVATOR, GreeterServiceWithCustomIntentActivator.class.getName())
                .build(TinyBundles.withBnd());
    }

    @Configuration
    public static Option[] configure() throws Exception {
        return new Option[] {
                MultiBundleTools.getDistro(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),
                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.junit").version("4.9_2"),
                mavenBundle().groupId("org.apache.cxf.dosgi.samples").artifactId("cxf-dosgi-ri-samples-greeter-interface").versionAsInProject(),
                mavenBundle().groupId("org.apache.cxf.dosgi.systests").artifactId("cxf-dosgi-ri-systests2-common").versionAsInProject(), 
                streamBundle(getCustomIntentBundle()).noStart(), 
                provision(getServiceBundle()),
                frameworkStartLevel(100) };
    }

    @Test
    public void testCustomIntent() throws Exception {
        // There should be warnings of unsatisfied intent myIntent in the log at debug level
        Thread.sleep(2000);
        getBundleByName(bundleContext, "CustomIntent").start();
        waitPort(9090);
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setServiceClass(GreeterService.class);
        factory.setAddress("http://localhost:9090/greeter");
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());
        factory.setServiceClass(GreeterService.class);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ClientProxyFactoryBean.class.getClassLoader());
        try {
            GreeterService greeterService = (GreeterService) factory.create();
            Map<GreetingPhrase, String> result = greeterService.greetMe("Chris");
            Assert.assertEquals(1, result.size());
            GreetingPhrase phrase = result.keySet().iterator().next();
            Assert.assertEquals("Hi from custom intent", phrase.getPhrase());
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
    
    private Bundle getBundleByName(BundleContext bc, String sn) {
        for (Bundle bundle : bc.getBundles()) {
            if (bundle.getSymbolicName().equals(sn)) {
                return bundle;
            }
        }
        return null;
    }

    private void waitPort(int port) throws Exception {
        for (int i = 0; i < 20; i++) {
            Socket s = null;
            try {
                s = new Socket((String) null, port);
                // yep, its available
                return;
            } catch (IOException e) {
                // wait 
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {}
                }
            }
            System.out.println("Waiting for server to appear on port: " + port);
            Thread.sleep(10000);            
        }
        throw new TimeoutException();
    }
}
