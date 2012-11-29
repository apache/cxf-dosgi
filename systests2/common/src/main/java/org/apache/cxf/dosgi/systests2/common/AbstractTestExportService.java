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

public abstract class AbstractTestExportService {
    // Make sure to explicitly invoke the test from the actual test class
    // Annotating this method with @Test will not properly report any failures.
    protected void baseTestAccessEndpoint() throws Exception {
        waitPort(9090);
        URL wsdlURL = new URL("http://localhost:9090/greeter?wsdl");
        
        // Do some basic checking on the WSDL
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(wsdlURL.openStream());
        Element el = doc.getDocumentElement();
        Assert.assertEquals("definitions", el.getLocalName());
        Assert.assertEquals("http://schemas.xmlsoap.org/wsdl/", el.getNamespaceURI());
        Assert.assertEquals("GreeterService", el.getAttribute("name"));
        
        // Make an actual invocation on the remote service.
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ClientProxyFactoryBean.class.getClassLoader());        
        try {
            ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
            factory.setServiceClass(GreeterService.class);
            factory.setAddress("http://localhost:9090/greeter");
            factory.getServiceFactory().setDataBinding(new AegisDatabinding());
            GreeterService client = (GreeterService)factory.create();

            Map<GreetingPhrase, String> greetings = client.greetMe("Fred");
            Assert.assertEquals("Fred", greetings.get(new GreetingPhrase("Hello")));
            System.out.println("Invocation result: " + greetings);

            try {
                GreeterData gd = new GreeterDataImpl("Stranger", 11, true);
                client.greetMe(gd);
                Assert.fail("GreeterException has to be thrown");
            } catch (GreeterException ex) {
                Assert.assertEquals("Wrong exception message", 
                             "GreeterService can not greet Stranger", 
                             ex.toString());
            }

        } finally {
            Thread.currentThread().setContextClassLoader(cl);            
        } 
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
            Thread.sleep(1000);            
        }
        throw new TimeoutException();
    }
    
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
}
