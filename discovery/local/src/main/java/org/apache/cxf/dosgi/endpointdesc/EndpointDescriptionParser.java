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
package org.apache.cxf.dosgi.endpointdesc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.osgi.xmlns.rsa.v1_0.EndpointDescriptionType;
import org.osgi.xmlns.rsa.v1_0.EndpointDescriptionsType;

public class EndpointDescriptionParser {
    private JAXBContext jaxbContext;

    public EndpointDescriptionParser() {
        try {
            jaxbContext = JAXBContext.newInstance(EndpointDescriptionsType.class.getPackage().getName(),
                                                  this.getClass().getClassLoader());
        } catch (JAXBException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<EndpointDescriptionType> getEndpointDescriptions(InputStream is) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Source source = new StreamSource(is);
            JAXBElement<EndpointDescriptionsType> jaxb = unmarshaller.unmarshal(source, EndpointDescriptionsType.class);
            EndpointDescriptionsType decorations = jaxb.getValue();
            return decorations.getEndpointDescription();
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    public void writeTo(EndpointDescriptionsType endpointDescriptions, OutputStream os) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            QName name = new QName("http://www.osgi.org/xmlns/rsa/v1.0.0", "endpoint-descriptions");
            JAXBElement<EndpointDescriptionsType> el = 
                new JAXBElement<EndpointDescriptionsType>(name, EndpointDescriptionsType.class, 
                    endpointDescriptions);
            marshaller.marshal(el, os);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    public byte[] getData(EndpointDescriptionType endpointDescription) {
        EndpointDescriptionsType endpointDescriptions = new EndpointDescriptionsType();
        endpointDescriptions.getEndpointDescription().add(endpointDescription);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeTo(endpointDescriptions, bos);
        return bos.toByteArray();
    }
}
