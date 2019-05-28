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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

public final class MultiBundleTools {

    private MultiBundleTools() {
    }

    private static Collection<String> getDistroBundles(File distroDir) {
        List<String> bundles = new ArrayList<>();
        File bundlesDir = new File(distroDir, "bundle");
        for (File file : bundlesDir.listFiles()) {
            if (file.getName().toLowerCase().endsWith(".jar")) {
                bundles.add(file.toURI().toASCIIString());
            }
        }
        return bundles;
    }

    private static File getRootDirectory() {
        URL url = MultiBundleTools.class.getResource("/"); // get ${root}/target/test-classes
        File dir = new File(url.getFile()).getAbsoluteFile();
        return dir.getParentFile().getParentFile();
    }

    public static Option getDistro() {
        File rootDir = getRootDirectory();
        File depsDir = new File(rootDir, "target/dependency");
        File distroDir = depsDir.listFiles()[0];
        Collection<String> bundleUris = getDistroBundles(distroDir);
        List<Option> opts = new ArrayList<>();
        for (String uri : bundleUris) {
            Option opt = CoreOptions.bundle(uri);
            // Make sure annotation bundle is loaded first to make sure it is used for resolution
            if (uri.contains("javax.annotation")) {
                opts.add(0, opt);
            } else {
                opts.add(opt);
            }
        }
        return CoreOptions.composite(opts.toArray(new Option[opts.size()]));
    }

}
