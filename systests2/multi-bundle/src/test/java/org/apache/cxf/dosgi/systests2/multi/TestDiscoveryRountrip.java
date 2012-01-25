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

import org.apache.cxf.dosgi.systests2.common.AbstractTestDiscoveryRoundtrip;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

@RunWith( JUnit4TestRunner.class )
public class TestDiscoveryRountrip extends AbstractTestDiscoveryRoundtrip {
    @Inject
    BundleContext bundleContext = null;

    @Configuration
    public static Option[] configure() throws Exception {
        Map<Integer, String> bundles = new TreeMap<Integer, String>();
        int startLevel = MultiBundleTools.getDistroBundles(bundles, true);
        
        List<Option> opts = new ArrayList<Option>();
        
        // Run this test under Equinox. 
        opts.add(CoreOptions.frameworks(CoreOptions.equinox()));
        
        // This property sets the start level for felix
        opts.add(CoreOptions.systemProperty("org.osgi.framework.startlevel.beginning").value("" + startLevel));
        
        // This property sets the start level for equinox
        opts.add(CoreOptions.systemProperty("osgi.startLevel").value("" + startLevel));

        for(Map.Entry<Integer, String> entry : bundles.entrySet()) {
            opts.add(CoreOptions.bundle(entry.getValue()).startLevel(entry.getKey()));
        }
      
        opts.add(CoreOptions.mavenBundle().groupId("org.apache.log4j").artifactId("com.springsource.org.apache.log4j").versionAsInProject());
        opts.add(CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi").artifactId("cxf-dosgi-ri-discovery-distributed-zookeeper-server").versionAsInProject());

        // This bundle contains the common system testing code
        opts.add(CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi.systests").artifactId("cxf-dosgi-ri-systests2-common").versionAsInProject());

        opts.add(CoreOptions.provision(getClientBundle()));
        opts.add(CoreOptions.provision(getServerBundle()));
        
        return CoreOptions.options(opts.toArray(new Option[opts.size()]));                
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
