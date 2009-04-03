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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;

public abstract class AbstractDosgiSystemTest extends AbstractConfigurableBundleCreatorTests {
    private Properties dependencies;

    // -------------------------------------------------------------------
    // These methods are copied from the SMX Kernel Testing component. They
    // are copied from the 
    // org.apache.servicemix.kernel.testing.support.AbstractIntegrationTest 
    // class. SMX Kernel 1.0 doesn't seem to work with Spring-DM 1.2, so 
    // thats why these methods are duplicated here for the moment...
    // -------------------------------------------------------------------
    protected String getBundle(String groupId, String artifactId) {
        return groupId + "," + artifactId + "," + getBundleVersion(groupId, artifactId);
    }

    protected String getBundleVersion(String groupId, String artifactId) {
        if (dependencies == null) {
            try {
                File f = new File(System.getProperty("basedir"), "target/classes/META-INF/maven/dependencies.properties");
                Properties prop = new Properties();
                prop.load(new FileInputStream(f));
                dependencies = prop;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load dependencies informations", e);
            }
        }
        String version = dependencies.getProperty(groupId + "/" + artifactId + "/version");
        if (version == null) {
            throw new IllegalStateException("Unable to find dependency information for: " + groupId + "/" + artifactId + "/version");
        }
        return version;
    }

    protected File localMavenBundle(String groupId, String artifact, String version, String classifier, String type) {
        String defaultHome = new File(new File(System.getProperty("user.home")), ".m2/repository").getAbsolutePath();
        File repositoryHome = new File(System.getProperty("localRepository", defaultHome));

        StringBuffer location = new StringBuffer(groupId.replace('.', '/'));
        location.append('/');
        location.append(artifact);
        location.append('/');
        location.append(getSnapshot(version));
        location.append('/');
        location.append(artifact);
        location.append('-');
        location.append(version);
        if (classifier != null) {
            location.append('-');
            location.append(classifier);
        }
        location.append(".");
        location.append(type);

        return new File(repositoryHome, location.toString());
    }

    protected static boolean isTimestamped(String version) {
        return version.matches(".+-\\d\\d\\d\\d\\d\\d\\d\\d\\.\\d\\d\\d\\d\\d\\d-\\d+");
    }

    protected static String getSnapshot(String version) {
        if (isTimestamped(version)) {
            return version.substring(0, version.lastIndexOf('-', version.lastIndexOf('-') - 1)) + "-SNAPSHOT";
        }
        return version;
    }

    protected void installBundle(String groupId, String artifactId, String classifier, String type) throws Exception {
        String version = getBundleVersion(groupId, artifactId);
        File loc = localMavenBundle(groupId, artifactId, version, classifier, type);
        Bundle bundle = bundleContext.installBundle(loc.toURI().toString());
        bundle.start();
    }    
    // -------------------------------------------------------------------
    // End of SMX Kernel methods.
    // -------------------------------------------------------------------
    

    protected Manifest getManifest() {
        // let the testing framework create/load the manifest
        Manifest mf = super.getManifest();
        String importP = mf.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
        mf.getMainAttributes().putValue(Constants.IMPORT_PACKAGE, importP);
        mf.getMainAttributes().putValue(Constants.DYNAMICIMPORT_PACKAGE, "*");

        return mf;
    }       

    @Override
    protected String[] getTestFrameworkBundlesNames() {
        List<String> names = new ArrayList<String>(Arrays.asList(super.getTestFrameworkBundlesNames()));        
        fixLog4J(names);        
        names.add(getBundle("org.apache.felix", "org.osgi.compendium"));
        return names.toArray(new String [names.size()]);
    }

    private void fixLog4J(List<String> names) {
        for (Iterator<String> it = names.iterator(); it.hasNext(); ) {
            if (it.next().equals("org.springframework.osgi,log4j.osgi,1.2.15-SNAPSHOT")) {
                it.remove();
                break;
            }
        }
        // replace it with a better version
        names.add("org.apache.log4j,com.springsource.org.apache.log4j,1.2.15");
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
