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

import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class TestDiscoveryExport extends AbstractDosgiTest {

    private static final String GREETER_ZOOKEEPER_NODE = //
        "/osgi/service_registry/org/apache/cxf/dosgi/samples/greeter/GreeterService/localhost#9090##greeter";

    @Configuration
    public static Option[] configure() throws Exception {
        return new Option[] //
        {
         basicTestOptions(), //
         configZKServer(), //
         configZKConsumer(), //
         greeterInterface(), //
         greeterImpl(),
        };
    }

    @Test
    public void testDiscoveryExport() throws Exception {
        ZooKeeper zk = createZookeeperClient();
        assertNodeExists(zk, GREETER_ZOOKEEPER_NODE, 5000);
        zk.close();
    }

}
