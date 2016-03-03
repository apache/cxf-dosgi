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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

public final class MultiBundleTools {

    private MultiBundleTools() {
    }
    
    private static Properties getProps(File distroDir) throws FileNotFoundException, IOException {
        Properties p = new Properties();
        File confFile = new File(distroDir, "conf/felix.config.properties.append");
        p.load(new FileInputStream(confFile));
        return p;
    }

    private static int getDistroBundles(File distroDir,
                                        Properties props, 
                                        Map<Integer, String> bundles) throws Exception {
        int startLevel = Integer.parseInt(props.getProperty("org.osgi.framework.startlevel.beginning"));
        for (int i = 0; i <= startLevel; i++) {
            String val = props.getProperty("felix.auto.start." + i);
            if (val != null) {
                if (val.startsWith("file:")) {
                    File fullDir = new File(distroDir, val.substring("file:".length()));
                    bundles.put(i, fullDir.toURI().toASCIIString());
                } else {
                    if (!val.contains("org.osgi.compendium")) {
                        // We're skipping that one as it's pulled in explicitly in the test
                        bundles.put(i, val);
                    }
                }
            }
        }
        return startLevel + 1; // Add 1 to start level to be on the safe side
    }

    private static File getRootDirectory() {
        String resourceName = "/" + MultiBundleTools.class.getName().replace('.', '/') + ".class";
        URL curURL = MultiBundleTools.class.getResource(resourceName);
        File curFile = new File(curURL.getFile());
        String curString = curFile.getAbsolutePath();
        File curBase = new File(curString.substring(0, curString.length() - resourceName.length()));
        return curBase.getParentFile().getParentFile();
    }

    private static Option[] getDistroBundleOptions() throws Exception {
        Map<Integer, String> bundles = new TreeMap<Integer, String>();
        File root = getRootDirectory();
        File depRoot = new File(root, "target/dependency");
        File distroDir = depRoot.listFiles()[0];
        Properties props = getProps(distroDir);
        getDistroBundles(distroDir, props, bundles);
        List<Option> opts = new ArrayList<Option>();
        
        /*
        String sysPackagesValue = props.getProperty("org.osgi.framework.system.packages");
        opts.add(CoreOptions.frameworkProperty("org.osgi.framework.system.packages")
                 .value(sysPackagesValue));
        */

        for (Map.Entry<Integer, String> entry : bundles.entrySet()) {
            String bundleUri = entry.getValue();
            opts.add(CoreOptions.bundle(bundleUri));
        }
        return opts.toArray(new Option[opts.size()]);
    }

    public static Option getDistroWithDiscovery() throws Exception {
        return getDistro();
    }

    public static Option getDistro() throws Exception {
        return CoreOptions.composite(getDistroBundleOptions());
    }
}
