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

package org.apache.cxf.dosgi.dsw.handlers;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.osgi.framework.BundleContext;

public class JaxRSUtils {

	private static final Logger LOG = Logger.getLogger(JaxRSUtils.class.getName());
	
	public final static String MODEL_FOLDER = "/OSGI-INF/cxf/jaxrs/";
	public final static String DEFAULT_MODEL = "/OSGI-INF/cxf/jaxrs/model.xml";
	
	public static List<UserResource> getModel(BundleContext callingContext, Class<?> iClass) {
		String classModel = MODEL_FOLDER + iClass.getSimpleName() + "-model.xml";
		List<UserResource> list = getModel(callingContext, iClass, classModel);
		return list != null ? list : getModel(callingContext, iClass, DEFAULT_MODEL);
    }
	
	public static List<UserResource> getModel(BundleContext callingContext, Class<?> iClass, String name) {
		InputStream r = iClass.getClassLoader().getResourceAsStream(name);
    	if (r == null) {
    		URL u = callingContext.getBundle().getResource(name);
    		if (u != null) {
    			try {
    			    r = u.openStream();
    			} catch (Exception ex) {
    				LOG.info("Problems opening a user model resource at " + u.toString());
    			}
    		}
    	}
    	if (r != null) {
    		try {
    		    return ResourceUtils.getUserResources(r);
    		} catch (Exception ex) {
    			LOG.info("Problems reading a user model, it will be ignored");
    		}
    	}
    	return null;
    }
	
}
