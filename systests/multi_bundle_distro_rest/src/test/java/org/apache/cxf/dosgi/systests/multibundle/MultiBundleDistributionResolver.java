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
package org.apache.cxf.dosgi.systests.multibundle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class MultiBundleDistributionResolver {
    private MultiBundleDistributionResolver() {}

    // Read the distribution from the distribution/multi-bundle module
    static File [] getDistribution() throws Exception {
        File distroRoot = new File(System.getProperty("basedir") + "/../../distribution/multi-bundle"); 
        File distroFile = new File(distroRoot, "target/classes/distro_bundles.xml");
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(distroFile);
        
        List<File> files = new ArrayList<File>();        
        NodeList nodes = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if ("bundle".equals(n.getNodeName())) {
                String location = n.getTextContent();
                File bundleFile = new File(distroRoot, "target/" + location);
                files.add(bundleFile.getCanonicalFile());
            }
        }
        
        return files.toArray(new File[files.size()]);
    }
}
