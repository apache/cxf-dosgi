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


import javax.inject.Inject;

import org.apache.cxf.dosgi.systests2.common.AbstractTestImportService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

@RunWith(JUnit4TestRunner.class)
public class TestImportService extends AbstractTestImportService {
    @Inject
    BundleContext bundleContext;

    @Configuration
    public static Option[] configure() throws Exception {
        return new Option[] {
                MultiBundleTools.getDistroWithDiscovery(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                mavenBundle().groupId("org.apache.cxf.dosgi.samples")
                    .artifactId("cxf-dosgi-ri-samples-greeter-interface").versionAsInProject(),
                mavenBundle().groupId("org.apache.servicemix.bundles")
                    .artifactId("org.apache.servicemix.bundles.junit").version("4.9_2"),
                mavenBundle().groupId("org.apache.cxf.dosgi.systests")
                    .artifactId("cxf-dosgi-ri-systests2-common").versionAsInProject(),
                provision(getTestClientBundle()),
                // increase for debugging
                systemProperty("org.apache.cxf.dosgi.test.serviceWaitTimeout").value(
                        System.getProperty("org.apache.cxf.dosgi.test.serviceWaitTimeout", "20")), 
                frameworkStartLevel(100)
        };
    }
    
    protected BundleContext getBundleContext() {
        return bundleContext;
    }

    @Test
    public void testClientConsumer() throws Exception {
//        for( Bundle b : bundleContext.getBundles() ) {
//            System.out.println( "*** Bundle " + b.getBundleId() + " : " + b.getSymbolicName() + "/" + b.getState());
//        }
        baseTestClientConsumer();
    }    
}
