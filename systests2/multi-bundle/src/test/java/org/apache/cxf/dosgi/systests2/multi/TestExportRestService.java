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

import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.InputStream;

import org.apache.cxf.dosgi.systests2.multi.rest.RestTranslate;
import org.apache.cxf.dosgi.systests2.multi.rest.RestTranslateImpl;
import org.apache.cxf.dosgi.systests2.multi.rest.TranslateActivator;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;

@RunWith(PaxExam.class)
public class TestExportRestService extends AbstractDosgiTest {
    String webPort = "9091";

    @Configuration
    public Option[] configure() throws Exception {
        return new Option[] //
        {//
         basicTestOptions(), //
         systemProperty("org.osgi.service.http.port").value(webPort), //
         provision(getServiceBundle()),
         //debug()
        };
    }

    @Test
    public void testCallService() throws Exception {
        waitWebPage("http://localhost:" + webPort + "/cxf/translate");
        try {
            WebClient client = WebClient.create("http://localhost:" + webPort + "/cxf/translate/hello");
            String result = client.get(String.class);
            Assert.assertEquals("hallo", result);
        } catch (Exception e) {
            // Avoid serialization problems when just letting the exception fly
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private InputStream getServiceBundle() {
        return TinyBundles.bundle() //
            .add(RestTranslate.class) //
            .add(RestTranslateImpl.class) //
            .add(TranslateActivator.class) //
            .set(Constants.BUNDLE_SYMBOLICNAME, "RestTranslate") //
            .set(Constants.BUNDLE_ACTIVATOR, TranslateActivator.class.getName()) //
            .build(TinyBundles.withBnd());
    }
}
