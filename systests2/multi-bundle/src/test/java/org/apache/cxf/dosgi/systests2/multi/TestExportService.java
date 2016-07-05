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
import java.net.URL;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.cxf.dosgi.samples.greeter.GreeterData;
import org.apache.cxf.dosgi.samples.greeter.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@RunWith(PaxExam.class)
public class TestExportService extends AbstractDosgiTest {

    @Configuration
    public static Option[] configure() throws Exception {
        return new Option[] //
        {//
         basicTestOptions(), //
         //debug(),
         greeterInterface(), //
         greeterImpl(),
        };
    }

    @Test
    public void testAccessEndpoint() throws Exception {
        waitPort(9090);
        checkWsdl(new URL("http://localhost:9090/greeter?wsdl"));
        checkServiceCall("http://localhost:9090/greeter");
    }

    private void checkServiceCall(String serviceUri) {
        GreeterService client = GreeterServiceProxyFactory.createGreeterServiceProxy(serviceUri);

        Map<GreetingPhrase, String> greetings = client.greetMe("Fred");
        Assert.assertEquals("Fred", greetings.get(new GreetingPhrase("Hello")));
        System.out.println("Invocation result: " + greetings);

        try {
            GreeterData gd = new GreeterDataImpl("Stranger", 11, true);
            client.greetMe(gd);
            Assert.fail("GreeterException has to be thrown");
        } catch (GreeterException ex) {
            Assert.assertEquals("Wrong exception message", "GreeterService can not greet Stranger",
                                ex.toString());
        }
    }

    private void checkWsdl(URL wsdlURL) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(wsdlURL.openStream());
        Element el = doc.getDocumentElement();
        Assert.assertEquals("definitions", el.getLocalName());
        Assert.assertEquals("http://schemas.xmlsoap.org/wsdl/", el.getNamespaceURI());
        Assert.assertEquals("GreeterService", el.getAttribute("name"));
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
