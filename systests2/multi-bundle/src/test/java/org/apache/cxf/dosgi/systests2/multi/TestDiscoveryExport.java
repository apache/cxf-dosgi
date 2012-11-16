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

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.cxf.dosgi.systests2.common.AbstractTestDiscoveryRoundtrip;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(JUnit4TestRunner.class)
public class TestDiscoveryExport extends AbstractTestDiscoveryRoundtrip {

    @Inject
    BundleContext bundleContext = null;

    @Inject
    ConfigurationAdmin configAdmin;

    @Configuration
    public static Option[] configure() throws Exception {
        Map<Integer, String> bundles = new TreeMap<Integer, String>();
        MultiBundleTools.getDistroBundles(bundles, true);

        List<Option> opts = new ArrayList<Option>();

        for (Map.Entry<Integer, String> entry : bundles.entrySet()) {
            String bundleUri = entry.getValue();
            opts.add(CoreOptions.bundle(bundleUri));
        }
        opts.add(CoreOptions.mavenBundle().groupId("org.apache.servicemix.bundles")
                .artifactId("org.apache.servicemix.bundles.junit").version("4.9_2"));
        opts.add(CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi.samples")
                .artifactId("cxf-dosgi-ri-samples-greeter-interface").versionAsInProject());
        opts.add(CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi.samples")
                .artifactId("cxf-dosgi-ri-samples-greeter-impl").versionAsInProject());
        opts.add(mavenBundle().groupId("org.apache.cxf.dosgi.systests").artifactId("cxf-dosgi-ri-systests2-common")
                .versionAsInProject());

        return CoreOptions.options(opts.toArray(new Option[opts.size()]));
    }

    @Test
    public void testDiscoveryExport() throws Exception {
        final int zkPort = getFreePort();
        configureZookeeper(configAdmin, zkPort);
        Thread.sleep(3000);
        ZooKeeper zk = new ZooKeeper("localhost:" + zkPort, 1000, null);
        Stat stat = zk.exists(
                "/osgi/service_registry/org/apache/cxf/dosgi/samples/greeter/GreeterService/localhost#9090##greeter",
                null);
        Assert.assertNotNull("Node for GreeterService endpoint not found in zookeeper", stat);
        zk.close();
    }

    private int getFreePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        try {
            return socket.getLocalPort();
        } finally {
            socket.close();
        }
    }
}
