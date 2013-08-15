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
package org.apache.cxf.dosgi.dsw.decorator;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;
import org.apache.cxf.xmlns.service_decoration._1_0.ServiceDecorationType;
import org.apache.cxf.xmlns.service_decoration._1_0.ServiceDecorationsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DecorationParser {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDecoratorImpl.class);
    private JAXBContext jaxbContext;
    private Schema schema;

    DecorationParser() {
        try {
            jaxbContext = JAXBContext.newInstance(ServiceDecorationsType.class.getPackage().getName(),
                                                  this.getClass().getClassLoader());
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL resource = getClass().getResource("/service-decoration.xsd");
            schema = schemaFactory.newSchema(resource);
        } catch (JAXBException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (SAXException e) {
            throw new RuntimeException("Error loading decorations schema", e);
        }

    }

    List<ServiceDecorationType> getDecorations(URL resourceURL) {
        if (resourceURL == null) {
            return new ArrayList<ServiceDecorationType>();
        }
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            InputStream is = resourceURL.openStream();
            Source source = new StreamSource(is);
            JAXBElement<ServiceDecorationsType> jaxb = unmarshaller.unmarshal(source, ServiceDecorationsType.class);
            ServiceDecorationsType decorations = jaxb.getValue();
            return decorations.getServiceDecoration();
        } catch (Exception ex) {
            LOG.warn("Problem parsing: " + resourceURL, ex);
            return new ArrayList<ServiceDecorationType>();
        }
    }
}
