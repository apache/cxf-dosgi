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
package org.apache.cxf.dosgi.systests2.single;

import org.apache.cxf.dosgi.systests2.common.AbstractTestImportService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
public class TestImportService extends AbstractTestImportService {
    @Inject
    BundleContext bundleContext;

    @Configuration
    public static Option[] configure() {
        return CoreOptions.options(
                // Run this one in Equinox
                CoreOptions.frameworks(CoreOptions.equinox()),
                
                CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi")
                    .artifactId("cxf-dosgi-ri-singlebundle-distribution").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi.samples")
                    .artifactId("cxf-dosgi-ri-samples-greeter-interface").versionAsInProject(),

                // This bundle contains the common system testing code
                CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi.systests")
                    .artifactId("cxf-dosgi-ri-systests2-common").versionAsInProject(),

                CoreOptions.provision(getTestClientBundle())
        );
    }

    protected BundleContext getBundleContext() {
        return bundleContext;
    }

    @Test
    public void testClientConsumer() throws Exception {
        baseTestClientConsumer();
    }    
}
