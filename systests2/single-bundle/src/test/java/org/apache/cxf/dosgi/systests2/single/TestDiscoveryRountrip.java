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

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import org.apache.cxf.dosgi.systests2.common.AbstractTestDiscoveryRoundtrip;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.BundleContext;

@RunWith( JUnit4TestRunner.class )
public class TestDiscoveryRountrip extends AbstractTestDiscoveryRoundtrip {
    @Inject
    BundleContext bundleContext = null;

    @Configuration
    public static Option[] configure() {
        return CoreOptions.options(
//                // this just adds all what you write here to java vm argumenents of the (new) osgi process.
//                vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006" ),
//                // this is necessary to let junit runner not timout the remote process before attaching debugger
//                // setting timeout to 0 means wait as long as the remote service comes available.
//                waitForFrameworkStartup(),
                
                CoreOptions.frameworks(CoreOptions.felix()),
                mavenBundle().groupId("org.apache.cxf.dosgi").artifactId("cxf-dosgi-ri-singlebundle-distribution").versionAsInProject(),
                wrappedBundle(new MavenArtifactUrlReference().groupId("log4j").artifactId("log4j").versionAsInProject()),
                mavenBundle().groupId("org.apache.zookeeper").artifactId("zookeeper").versionAsInProject(),
                mavenBundle().groupId("org.apache.cxf.dosgi").artifactId("cxf-dosgi-ri-discovery-distributed-zookeeper-server").versionAsInProject(),
                
                // This bundle contains the common system testing code
                mavenBundle().groupId("org.apache.cxf.dosgi.systests").artifactId("cxf-dosgi-ri-systests2-common").versionAsInProject(),

                provision(getClientBundle()),
                provision(getServerBundle())
        );
    }
    
    protected BundleContext getBundleContext() {
        return bundleContext;
    }

    @Test
    public void testDiscoveryRoundtrip() throws Exception {
         // Disabled temporarily
         // baseTestDiscoveryRoundtrip();
    }
}
