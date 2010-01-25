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
package org.apache.cxf.dosgi.systests2.common;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.samples.greeter.GreeterData;
import org.apache.cxf.dosgi.samples.greeter.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@RunWith( JUnit4TestRunner.class )
public class TestExportService {
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
                
                CoreOptions.mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi").artifactId("cxf-dosgi-ri-singlebundle-distribution").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi.samples").artifactId("cxf-dosgi-ri-samples-greeter-interface").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi.samples").artifactId("cxf-dosgi-ri-samples-greeter-impl").versionAsInProject()
        );
    }
    
    @Test
    public void testCreateEndpoint() throws Exception {
        for( Bundle b : bundleContext.getBundles() )
        {
            System.out.println( "Bundle " + b.getBundleId() + " : " + b.getSymbolicName() );
        }        

        waitPort(9090);
        URL wsdlURL = new URL("http://localhost:9090/greeter?wsdl");
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(wsdlURL.openStream());
        Element el = doc.getDocumentElement();
        Assert.assertEquals("definitions", el.getLocalName());
        Assert.assertEquals("http://schemas.xmlsoap.org/wsdl/", el.getNamespaceURI());
        Assert.assertEquals("GreeterService", el.getAttribute("name"));
        
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ClientProxyFactoryBean.class.getClassLoader());        
        try {
            ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
            factory.setServiceClass(GreeterService.class);
            factory.setAddress("http://localhost:9090/greeter");
            factory.getServiceFactory().setDataBinding(new AegisDatabinding());
            GreeterService client = (GreeterService)factory.create();
            Map<GreetingPhrase, String> greetings = client.greetMe("Fred");
            Assert.assertEquals("Fred", greetings.get(new GreetingPhrase("Hello")));
            
            try {
                class GreeterDataImpl implements GreeterData {
                    private String name;
                    private int age;
                    private boolean exception;

                    GreeterDataImpl(String n, int a, boolean ex) {
                        name = n;
                        age = a;
                        exception = ex;
                    }
                    
                    public String getName() {
                        return name;
                    }

                    public int getAge() {
                        return age;
                    }

                    public boolean isException() {
                        return exception;
                    }                
                }
                
                GreeterData gd = new GreeterDataImpl("Stranger", 11, true);
                client.greetMe(gd);
                Assert.fail("GreeterException has to be thrown");
            } catch (GreeterException ex) {
                Assert.assertEquals("Wrong exception message", 
                             "GreeterService can not greet Stranger", 
                             ex.toString());
            } 
        } finally {
            Thread.currentThread().setContextClassLoader(contextLoader);            
        }
    }

    private void waitPort(int port) throws Exception {
        for (int i = 0; i < 10; i++) {
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
            Thread.sleep(1000);            
        }
        throw new TimeoutException();
    }
}
