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
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

public final class MultiBundleTools {

    private MultiBundleTools() {
    }

    private static Collection<String> getDistroBundles(File distroDir) throws Exception {
        List<String> bundles = new ArrayList<>();
        File bundlesDir = new File(distroDir, "bundle");
        File[] files = bundlesDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
            }
        );
        for (File file : files) {
            bundles.add(file.toURI().toASCIIString());
        }
        return bundles;
    }

    private static File getRootDirectory() {
        String resourceName = "/" + MultiBundleTools.class.getName().replace('.', '/') + ".class";
        URL curURL = MultiBundleTools.class.getResource(resourceName);
        File curFile = new File(curURL.getFile());
        String curString = curFile.getAbsolutePath();
        File curBase = new File(curString.substring(0, curString.length() - resourceName.length()));
        return curBase.getParentFile().getParentFile();
    }

    public static Option getDistro() throws Exception {
        File root = getRootDirectory();
        File depRoot = new File(root, "target/dependency");
        File distroDir = depRoot.listFiles()[0];
        Collection<String> bundles = getDistroBundles(distroDir);
        List<Option> opts = new ArrayList<Option>();
        for (String bundleUri : bundles) {
            opts.add(CoreOptions.bundle(bundleUri));
        }
        return CoreOptions.composite(opts.toArray(new Option[opts.size()]));
    }

}
