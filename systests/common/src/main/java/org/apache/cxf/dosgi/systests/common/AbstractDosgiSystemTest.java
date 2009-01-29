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
package org.apache.cxf.dosgi.systests.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.servicemix.kernel.testing.support.AbstractIntegrationTest;
import org.osgi.framework.Constants;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public abstract class AbstractDosgiSystemTest extends AbstractIntegrationTest {
    protected Manifest getManifest() {
        // let the testing framework create/load the manifest
        Manifest mf = super.getManifest();
        String importP = mf.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
        mf.getMainAttributes().putValue(Constants.IMPORT_PACKAGE, importP);
        mf.getMainAttributes().putValue(Constants.DYNAMICIMPORT_PACKAGE, "*");

        return mf;
    }       
        
    @Override
    protected Resource[] getTestBundles() {
        // Return the bundles for the current distribution, filtering out the 
        // ones that are already installed as part of the testing framework.
        // At the end the test subclass is called to obtain the test bundles.
        
        List<String> frameworkBundleNames = new ArrayList<String>();
        for (Resource r : getTestFrameworkBundles()) {
            frameworkBundleNames.add(r.getFilename());
        }

        
        try {
            List<Resource> resources = new ArrayList<Resource>();
            for (File file : getDistributionBundles()) {
                if (!frameworkBundleNames.contains(file.getName())) {
                    resources.add(new FileSystemResource(file));
                }
            }
            
            resources.addAll(Arrays.asList(super.getTestBundles()));
            return resources.toArray(new Resource[resources.size()]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract File [] getDistributionBundles() throws Exception;
}
