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
package org.apache.cxf.dosgi.itests.multi;

import java.net.URL;
import java.util.concurrent.Callable;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.cxf.dosgi.samples.soap.Task;
import org.apache.cxf.dosgi.samples.soap.TaskService;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Deploys the sample SOAP service and zookeeper discovery.
 * Then checks the service can be called via plain CXF and is announced in zookeeper
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestExportService extends AbstractDosgiTest {

    private static final String SERVICE_URI = HTTP_BASE_URI + "/cxf/taskservice";
    private static final String REST_SERVICE_URI = HTTP_BASE_URI + "/cxf/tasks";

    private static final String GREETER_ZOOKEEPER_NODE = //
        "/osgi/service_registry/http:##localhost:8181#cxf#taskservice";

    @Configuration
    public static Option[] configure() {
        return new Option[] //
        {//
         basicTestOptions(), //
         configZKServer(), //
         configZKConsumer(), //
         taskServiceAPI(), //
         taskServiceImpl(), //
         taskRESTAPI(), //
         taskRESTImpl(), //
         //debug(),
        };
    }

    @Test
    public void testSOAPCall() throws Exception {
        checkWsdl(new URL(SERVICE_URI + "?wsdl"));
        TaskService taskService = TaskServiceProxyFactory.create(SERVICE_URI);
        Task task = taskService.get(1);
        Assert.assertEquals("Buy some coffee", task.getTitle());
    }

    @Test
    public void testRESTCall() throws Exception {
        waitWebPage(REST_SERVICE_URI);
        final WebClient client = WebClient.create(REST_SERVICE_URI + "/1");
        client.accept(MediaType.APPLICATION_XML_TYPE);
        org.apache.cxf.dosgi.samples.rest.Task task = tryTo("Call REST Resource", 
                                                            new Callable<org.apache.cxf.dosgi.samples.rest.Task>() {
            @Override
            public org.apache.cxf.dosgi.samples.rest.Task call() {
                return client.get(org.apache.cxf.dosgi.samples.rest.Task.class);
            }
        }
        );
        Assert.assertEquals("Buy some coffee", task.getTitle());
        final WebClient swaggerClient = WebClient.create(REST_SERVICE_URI + "/swagger.json");
        String swaggerJson = swaggerClient.get(String.class);
        Assert.assertEquals("{\"swagger\":\"2.0\"", swaggerJson.substring(0, 16));
    }

    @Test
    public void testDiscoveryExport() throws Exception {
        ZooKeeper zk = createZookeeperClient();
        assertNodeExists(zk, GREETER_ZOOKEEPER_NODE, 5000);
        zk.close();
    }

    private void checkWsdl(final URL wsdlURL) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        final DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = tryTo("Parse WSDL", new Callable<Document>() {
            @Override
            public Document call() throws Exception {
                return db.parse(wsdlURL.openStream());
            }
        });

        Element el = doc.getDocumentElement();
        Assert.assertEquals("definitions", el.getLocalName());
        Assert.assertEquals("http://schemas.xmlsoap.org/wsdl/", el.getNamespaceURI());
        Assert.assertEquals("TaskServiceService", el.getAttribute("name"));
    }

}
