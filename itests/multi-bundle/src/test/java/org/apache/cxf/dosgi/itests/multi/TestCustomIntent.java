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

import static org.ops4j.pax.exam.CoreOptions.streamBundle;

import java.io.InputStream;
import java.util.concurrent.Callable;

import org.apache.cxf.dosgi.itests.multi.customintent.ChangeTitleInterceptor;
import org.apache.cxf.dosgi.itests.multi.customintent.CustomFeature;
import org.apache.cxf.dosgi.itests.multi.customintent.CustomIntentActivator;
import org.apache.cxf.dosgi.samples.soap.Task;
import org.apache.cxf.dosgi.samples.soap.TaskService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;

@RunWith(PaxExam.class)
public class TestCustomIntent extends AbstractDosgiTest {

    @Configuration
    public static Option[] configure() throws Exception {
        return new Option[] //
        {
         basicTestOptions(), //
         taskServiceAPI(), //
         streamBundle(getCustomIntentBundle()), //
         //debug()
        };
    }

    @Test
    public void testCustomIntent() throws Exception {
        String serviceUri = HTTP_BASE_URI + "/cxf/taskservice";
        final TaskService greeterService = TaskServiceProxyFactory.create(serviceUri);
        Task task = tryTo("Call TaskService", new Callable<Task>() {
            public Task call() throws Exception {
                return greeterService.get(1);
            }
        });
        
        Assert.assertEquals("changed", task.getTitle());
    }

    private static InputStream getCustomIntentBundle() {
        return TinyBundles.bundle() //
            .add(CustomIntentActivator.class) //
            .add(CustomFeature.class) //
            .add(ChangeTitleInterceptor.class) //
            .add(DummyTaskServiceImpl.class) //
            .set(Constants.BUNDLE_SYMBOLICNAME, "CustomIntent") //
            .set(Constants.BUNDLE_ACTIVATOR, CustomIntentActivator.class.getName())
            .build(TinyBundles.withBnd());
    }

}
