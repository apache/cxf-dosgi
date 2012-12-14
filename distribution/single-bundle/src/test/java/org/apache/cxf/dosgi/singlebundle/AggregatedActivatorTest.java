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
package org.apache.cxf.dosgi.singlebundle;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;

public class AggregatedActivatorTest extends TestCase {
    private Map<Object, Object> savedProps;
    private String oldDefaultPort;

    @Override
    protected void setUp() throws Exception {
        oldDefaultPort = AggregatedActivator.defaultHttpPort;
        // Change the default port to one that we know is available
        ServerSocket s = new ServerSocket(0);
        int availablePort = s.getLocalPort();
        s.close();
        AggregatedActivator.defaultHttpPort = "" + availablePort;        
        
        savedProps = new HashMap<Object, Object>(System.getProperties());
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Properties props = new Properties();
        props.putAll(savedProps);
        System.setProperties(props);
        
        AggregatedActivator.defaultHttpPort = oldDefaultPort;
    }

    public void testReadResourcesFile() throws Exception {        
        String[] expected = {
            "org.apache.felix.cm.impl.ConfigurationManager",
            "org.apache.felix.fileinstall.internal.FileInstall",
            "org.ops4j.pax.web.service.internal.Activator",
            "org.apache.cxf.dosgi.discovery.local.internal.Activator",
            "org.apache.cxf.dosgi.dsw.Activator",
            "org.apache.cxf.dosgi.discovery.zookeeper.Activator",
            "org.apache.cxf.dosgi.topologymanager.Activator",
            "org.springframework.osgi.extender.internal.activator.ContextLoaderListener"};
        
        assertEquals(Arrays.asList(expected), AggregatedActivator.getActivators());
    }
    
    public void testDefaultHttpServicePort() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);
        
        assertNull("Precondition failed", System.getProperty(AggregatedActivator.HTTP_PORT_PROPERTY));
        new AggregatedActivator().setHttpServicePort(bc);
        assertEquals(AggregatedActivator.defaultHttpPort, System.getProperty(AggregatedActivator.HTTP_PORT_PROPERTY));
    }
    
    public void testHttpServicePortFromProperty() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getProperty(AggregatedActivator.HTTP_PORT_PROPERTY)).
            andReturn("1234").anyTimes();
        EasyMock.replay(bc);

        Map<Object, Object> before = new HashMap<Object, Object>(System.getProperties());
        new AggregatedActivator().setHttpServicePort(bc);
        assertEquals("No additional properties should have been set",
                before, new HashMap<Object, Object>(System.getProperties()));
    }
    
    public void testHttpsServicePortFromProperty() {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getProperty(AggregatedActivator.HTTPS_PORT_PROPERTY)).
            andReturn("4321").anyTimes();
        EasyMock.expect(bc.getProperty(AggregatedActivator.HTTPS_ENABLED_PROPERTY)).
            andReturn("true").anyTimes();
        EasyMock.replay(bc);

        Map<Object, Object> before = new HashMap<Object, Object>(System.getProperties());
        new AggregatedActivator().setHttpServicePort(bc);
        assertEquals("No additional properties should have been set",
                before, new HashMap<Object, Object>(System.getProperties()));        
    }
    
    public void testHttpServicePortInUse() throws Exception {
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.replay(bc);

        ServerSocket s = null;
        try {
            try {
                // now lets block the default port
                s = new ServerSocket(Integer.parseInt(
                        AggregatedActivator.defaultHttpPort));
            } catch (Exception e) {
                // if someone else already has it, thats fine too
            }
            
            assertNull("Precondition failed", System.getProperty(AggregatedActivator.HTTP_PORT_PROPERTY));
            new AggregatedActivator().setHttpServicePort(bc);
            assertTrue("The " + AggregatedActivator.HTTP_PORT_PROPERTY 
                    + " property should have been set", 
                    System.getProperty(AggregatedActivator.HTTP_PORT_PROPERTY).length() > 0);
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }
    
    public void testSetHttpPortInActivator() throws Exception {
        final List<String> results = new ArrayList<String>();
        AggregatedActivator aa = new AggregatedActivator() {
            @Override
            void setHttpServicePort(BundleContext bc) {
                results.add("HTTPPort");
            }

            @Override
            void startEmbeddedActivators(BundleContext ctx) throws Exception {
                results.add("start_activators");
            }

            @Override
            void stopEmbeddedActivators(BundleContext ctx) throws Exception {
                results.add("stop_activators");
            }        
        };
        
        assertEquals("Precondition failed", 0, results.size());
        aa.start(null); 
        assertEquals(2, results.size());
        assertTrue(results.contains("HTTPPort"));
        assertTrue(results.contains("start_activators"));
        
        results.clear();
        aa.stop(null);
        assertEquals(1, results.size());
        assertEquals("stop_activators", results.iterator().next());        
    }
}
