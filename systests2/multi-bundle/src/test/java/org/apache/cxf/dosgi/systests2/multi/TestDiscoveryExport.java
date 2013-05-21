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

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.cxf.dosgi.systests2.common.test2.Test2Service;
import org.apache.cxf.dosgi.systests2.common.test2.client.ClientActivator;
import org.apache.cxf.dosgi.systests2.common.test2.client.Test2ServiceTracker;
import org.apache.cxf.dosgi.systests2.common.test2.server.ServerActivator;
import org.apache.cxf.dosgi.systests2.common.test2.server.Test2ServiceImpl;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;


@RunWith(JUnit4TestRunner.class)
public class TestDiscoveryExport extends AbstractDosgiTest {

    private static final String GREETER_ZOOKEEPER_NODE 
        = "/osgi/service_registry/org/apache/cxf/dosgi/samples/greeter/GreeterService/localhost#9090##greeter";

    @Inject
    BundleContext bundleContext;

    @Inject
    ConfigurationAdmin configAdmin;

    @Configuration
    public static Option[] configure() throws Exception {
        return new Option[] {
                MultiBundleTools.getDistroWithDiscovery(),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                mavenBundle().groupId("org.apache.servicemix.bundles")
                    .artifactId("org.apache.servicemix.bundles.junit").version("4.9_2"),
                mavenBundle().groupId("org.apache.cxf.dosgi.samples")
                    .artifactId("cxf-dosgi-ri-samples-greeter-interface").versionAsInProject(),
                mavenBundle().groupId("org.apache.cxf.dosgi.samples").
                    artifactId("cxf-dosgi-ri-samples-greeter-impl").versionAsInProject(),
                mavenBundle().groupId("org.apache.cxf.dosgi.systests")
                    .artifactId("cxf-dosgi-ri-systests2-common").versionAsInProject(),
                frameworkStartLevel(100)
        };
    }

    @Test
    public void testDiscoveryExport() throws Exception {
        final int zkPort = getFreePort();
        configureZookeeper(configAdmin, zkPort);
        ZooKeeper zk = new ZooKeeper("localhost:" + zkPort, 1000, null);
        assertNodeExists(zk, GREETER_ZOOKEEPER_NODE, 4000);
        zk.close();
    }

    private void assertNodeExists(ZooKeeper zk, String zNode, int timeout) {
        long endTime = System.currentTimeMillis() + timeout;
        Stat stat = null;
        while (stat == null  && System.currentTimeMillis() < endTime) {
            try {
                stat = zk.exists(zNode, null);
                Thread.sleep(200);
            } catch (Exception e) {
                // Ignore
            }
        }
        Assert.assertNotNull("Zookeeper node " + zNode + " was not found", stat);
    }

    protected static InputStream getClientBundle() {
        return TinyBundles.newBundle()
            .add(ClientActivator.class)
            .add(Test2Service.class)
            .add(Test2ServiceTracker.class)
            .set(Constants.BUNDLE_SYMBOLICNAME, "test2ClientBundle")
            .set(Constants.BUNDLE_ACTIVATOR, ClientActivator.class.getName())
            .build(TinyBundles.withBnd());
    }

    protected static InputStream getServerBundle() {
        return TinyBundles.newBundle()
            .add(ServerActivator.class)
            .add(Test2Service.class)
            .add(Test2ServiceImpl.class)
            .set(Constants.BUNDLE_SYMBOLICNAME, "test2ServerBundle")
            .set(Constants.BUNDLE_ACTIVATOR, ServerActivator.class.getName())
            .build(TinyBundles.withBnd());
    }

    protected void configureZookeeper(ConfigurationAdmin ca, int zkPort) throws IOException {
        System.out.println("*** Port for Zookeeper Server: " + zkPort);
        updateZkServerConfig(zkPort, ca);                            
        updateZkClientConfig(zkPort, ca);
    }
    
    protected void updateZkClientConfig(final int zkPort, ConfigurationAdmin cadmin) throws IOException {
        Dictionary<String, Object> cliProps = new Hashtable<String, Object>();
        cliProps.put("zookeeper.host", "127.0.0.1");
        cliProps.put("zookeeper.port", "" + zkPort);
        cadmin.getConfiguration("org.apache.cxf.dosgi.discovery.zookeeper", null).update(cliProps);
    }

    protected void updateZkServerConfig(final int zkPort, ConfigurationAdmin cadmin) throws IOException {
        Dictionary<String, Object> svrProps = new Hashtable<String, Object>();
        svrProps.put("clientPort", zkPort);
        cadmin.getConfiguration("org.apache.cxf.dosgi.discovery.zookeeper.server", null).update(svrProps);
    }
    
}
