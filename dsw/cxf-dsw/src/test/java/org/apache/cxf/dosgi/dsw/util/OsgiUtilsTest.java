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
package org.apache.cxf.dosgi.dsw.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class OsgiUtilsTest extends TestCase {

    public void testMultiValuePropertyAsString() {
        assertEquals(Collections.singleton("hi"), 
            OsgiUtils.getMultiValueProperty("hi"));            
    }
    
    public void testMultiValuePropertyAsArray() {
        assertEquals(Arrays.asList("a", "b"), 
                OsgiUtils.getMultiValueProperty(new String [] {"a", "b"}));
    }
    
    public void testMultiValuePropertyAsCollection() {
        List<String> list = new ArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");
        assertEquals(list, OsgiUtils.getMultiValueProperty(list)); 
    }
    
    public void testMultiValuePropertyNull() {
        assertNull(OsgiUtils.getMultiValueProperty(null));            
    }
    
    
    public void testGetUUID(){
        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getProperty(EasyMock.eq("org.osgi.framework.uuid"))).andReturn(null).atLeastOnce();
        EasyMock.replay(bc);
        String uuid = OsgiUtils.getUUID(bc);
        assertNotNull(uuid);
        
        assertEquals(System.getProperty("org.osgi.framework.uuid"),uuid );
        
        EasyMock.verify(bc);
    }
    
    
    
    public void testGetVersion(){
        IMocksControl c = EasyMock.createNiceControl();
        BundleContext bc = c.createMock(BundleContext.class);
        ServiceReference sref = c.createMock(ServiceReference.class);
        PackageAdmin pa = c.createMock(PackageAdmin.class);
        Bundle b = c.createMock(Bundle.class);
        
        EasyMock.expect(bc.getServiceReference(EasyMock.eq(PackageAdmin.class.getName()))).andReturn(sref);
        EasyMock.expect(bc.getService(EasyMock.eq(sref))).andReturn(pa);
        
        Class<?> iClass = CharSequence.class;
            
        c.replay();
        // version 0.0.0 because of missing bundle
        
        assertEquals("0.0.0", OsgiUtils.getVersion(iClass, bc));
        
        c.verify();
        c.reset();
        // version 1.2.3 ... 
        
        EasyMock.expect(bc.getServiceReference(EasyMock.eq(PackageAdmin.class.getName()))).andReturn(sref);
        EasyMock.expect(bc.getService(EasyMock.eq(sref))).andReturn(pa);
        EasyMock.expect(pa.getBundle(EasyMock.eq(iClass))).andReturn(b);
        
        ExportedPackage[] exP = new ExportedPackage[] { new MyExportedPackage(iClass.getPackage(),"1.2.3"), new MyExportedPackage(String.class.getPackage(),"4.5.6") }; 
        
        EasyMock.expect(pa.getExportedPackages(EasyMock.eq(b))).andReturn(exP).atLeastOnce();
        
        c.replay();
        assertEquals("1.2.3", OsgiUtils.getVersion(iClass, bc));
        c.verify();
    }
    
    
    private static class MyExportedPackage implements ExportedPackage{

        Package package1;
        String version;
        
        public MyExportedPackage(Package package1, String version) {
            this.package1 = package1;
            this.version = version;
        }

        public Bundle getExportingBundle() {
            return null;
        }

        public Bundle[] getImportingBundles() {
            return null;
        }

        public String getName() {
            return package1.getName();
        }

        public String getSpecificationVersion() {
            return null;
        }

        public Version getVersion() {
            return new Version(version);
        }

        public boolean isRemovalPending() {
            return false;
        }
        
    }
    
    
    public void testGetProperty(){
        
        Map<String, Object> p = new HashMap<String, Object>();
        p.put(RemoteConstants.ENDPOINT_ID, "http://google.de");
        p.put("notAString",new Object());
        p.put(org.osgi.framework.Constants.OBJECTCLASS, new String[]{"my.class"});
        p.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, new String[]{"my.config"});
        
        EndpointDescription ep = new EndpointDescription(p);
        
        assertNull(OsgiUtils.getProperty(ep, "unkownProp"));
        assertEquals(p.get(RemoteConstants.ENDPOINT_ID),OsgiUtils.getProperty(ep, RemoteConstants.ENDPOINT_ID));
        assertEquals(null, OsgiUtils.getProperty(ep, "notAString"));
    }
}
