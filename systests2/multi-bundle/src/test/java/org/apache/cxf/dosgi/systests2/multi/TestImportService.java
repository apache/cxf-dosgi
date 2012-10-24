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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.dosgi.systests2.common.AbstractTestImportService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.def.PaxRunnerOptions;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

@RunWith( JUnit4TestRunner.class )
public class TestImportService extends AbstractTestImportService {
    @Inject
    BundleContext bundleContext = null;

    @Configuration
    public static Option[] configure() throws Exception {
        Map<Integer, String> bundles = new TreeMap<Integer, String>();
        int startLevel = MultiBundleTools.getDistroBundles(bundles, false);
        
        List<Option> opts = new ArrayList<Option>();
        
        // Run this test under Felix. 
        opts.add(CoreOptions.frameworks(CoreOptions.felix()));

        for(Map.Entry<Integer, String> entry : bundles.entrySet()) {
            opts.add(CoreOptions.bundle(entry.getValue()).startLevel(entry.getKey()));
        }
        opts.add(CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi.samples").artifactId("cxf-dosgi-ri-samples-greeter-interface").versionAsInProject().startLevel(++startLevel));

        // This bundle contains the common system testing code
        opts.add(CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi.systests").artifactId("cxf-dosgi-ri-systests2-common").versionAsInProject().startLevel(++startLevel));
        opts.add(CoreOptions.provision(getTestClientBundle()));
        opts.add(CoreOptions.systemProperty("org.osgi.framework.startlevel.beginning").value("" + startLevel));

        String loggingConfigFile = System.getProperty("java.util.logging.config.file");
        if (loggingConfigFile != null) {
            // When running from eclipse junit the loggingConfigFile will not be set
            opts.add(CoreOptions.systemProperty("java.util.logging.config.file").value(loggingConfigFile));
        }

        // For debugging...
        final String debugPort = System.getProperty("org.apache.cxf.dosgi.test.debug.port");
        if(debugPort != null) {
            opts.add(PaxRunnerOptions.vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + debugPort ));
            opts.add(CoreOptions.waitForFrameworkStartup());
        }
        // end debugging section.

        // service wait timeout (this should also be increased for debugging)...
        opts.add(CoreOptions.systemProperty("org.apache.cxf.dosgi.test.serviceWaitTimeout").value(System.getProperty("org.apache.cxf.dosgi.test.serviceWaitTimeout", "20")));
        
        return CoreOptions.options(opts.toArray(new Option[opts.size()]));
    }
    
    protected BundleContext getBundleContext() {
        return bundleContext;
    }

    @Test
    public void testClientConsumer() throws Exception {
        baseTestClientConsumer();
    }    
}
