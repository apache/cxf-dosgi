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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MultiBundleTools {
    private MultiBundleTools() {}
    
    private static int getDistroBundles(Map<Integer, String> bundles, boolean discovery) throws Exception {
        File root = getRootDirectory();        
        File mdRoot = new File(root, "distribution/multi-bundle");
        String pomVersion = getPomVersion(mdRoot);
        
        return getDistroBundles(mdRoot, pomVersion, bundles, discovery);
    }
    
    private static int getDistroBundles(File mdRoot, String pomVersion, Map<Integer, String> bundles, boolean discovery) throws Exception {
        File distroDir = new File(mdRoot, "target/cxf-dosgi-ri-multibundle-distribution-" + pomVersion + "-dir");
        Properties p = new Properties();
        File confDir = new File(distroDir, "apache-cxf-dosgi-ri-" + pomVersion + "/conf");
        p.load(new FileInputStream(new File(confDir, "felix.config.properties.append")));
        if (discovery) {
            p.load(new FileInputStream(new File(confDir, "felix.discovery.config.properties.append")));
        }
        
        int startLevel = Integer.parseInt(p.getProperty("org.osgi.framework.startlevel.beginning"));
        for (int i = 0; i <= startLevel; i++) {
            String val = p.getProperty("felix.auto.start." + i);
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
        File root = curBase.getParentFile().getParentFile().getParentFile().getParentFile();
        return root;
    }

    private static String getPomVersion(File mdRoot) throws Exception {
        File mdPom = new File(mdRoot, "pom.xml");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(mdPom);
        Element el = doc.getDocumentElement();
        String pomVersion = null;
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("version".equals(child.getLocalName())) {
                pomVersion = child.getTextContent().trim();
                break;
            }
        }
        if (pomVersion == null) {
            throw new RuntimeException("Failed to retrieved version from pom file " + mdPom);
        }
        return pomVersion;
    }

    private static Option[] getDistroBundleOptions(boolean b) throws Exception {
        Map<Integer, String> bundles = new TreeMap<Integer, String>();
        MultiBundleTools.getDistroBundles(bundles, true);
        List<Option> opts = new ArrayList<Option>();
        for (Map.Entry<Integer, String> entry : bundles.entrySet()) {
            String bundleUri = entry.getValue();
            if (!bundleUri.contains("pax-logging")) {
                opts.add(CoreOptions.bundle(bundleUri));
            }
        }
        return opts.toArray(new Option[opts.size()]);
    }

    public static Option getDistroWithDiscovery() throws Exception {
        return CoreOptions.composite(getDistroBundleOptions(true));
    }
    
    public static Option getDistro() throws Exception {
        return CoreOptions.composite(getDistroBundleOptions(false));
    }
}
