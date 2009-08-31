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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.dosgi.dsw.OsgiUtils;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.provider.AegisElementProvider;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.discovery.ServiceEndpointDescription;

public class JaxRSUtils {

	private static final Logger LOG = Logger.getLogger(JaxRSUtils.class.getName());
	
	public final static String MODEL_FOLDER = "/OSGI-INF/cxf/jaxrs/";
	public final static String DEFAULT_MODEL = "/OSGI-INF/cxf/jaxrs/model.xml";
	public final static String PROVIDERS_FILTER = 
		"(|" 
		+ "(" + Constants.OBJECTCLASS + "=javax.ws.rs.ext.MessageBodyReader" + ")"
		+ "(" + Constants.OBJECTCLASS + "=javax.ws.rs.ext.MessageBodyWriter" + ")"
		+ "(" + Constants.OBJECTCLASS + "=javax.ws.rs.ext.ExceptionMapper" + ")"
		+ "(" + Constants.OBJECTCLASS + "=org.apache.cxf.jaxrs.ext.RequestHandler" + ")"
		+ "(" + Constants.OBJECTCLASS + "=org.apache.cxf.jaxrs.ext.ResponseHandler" + ")"
		+ "(" + Constants.OBJECTCLASS + "=org.apache.cxf.jaxrs.ext.ParameterHandler" + ")"
		+ ")";
	
	public static List<Object> getProviders(BundleContext callingContext, 
			                                BundleContext dswBC,
			                                ServiceEndpointDescription sd) {
		
		List<Object> providers = new ArrayList<Object>();
		if ("aegis".equals(sd.getProperty(org.apache.cxf.dosgi.dsw.Constants.RS_DATABINDING_PROP_KEY))) {
	        providers.add(new AegisElementProvider());
        }
        Object serviceProviders = 
        	sd.getProperty(org.apache.cxf.dosgi.dsw.Constants.RS_PROVIDER_PROP_KEY);
        if (serviceProviders != null) {
        	if (serviceProviders.getClass().isArray()) {
        		if (serviceProviders.getClass().getComponentType() == String.class) {
        			loadProviders(callingContext, providers, (String[])serviceProviders);	
        		} else {
        	        providers.addAll(Arrays.asList((Object[])serviceProviders));
        		}
        	} else {
        		String[] classNames = serviceProviders.toString().split(",");
        		loadProviders(callingContext, providers, classNames);
        	}
        }
		
		Object globalQueryProp = 
			sd.getProperty(org.apache.cxf.dosgi.dsw.Constants.RS_PROVIDER_GLOBAL_PROP_KEY);
		boolean globalQueryRequired = globalQueryProp == null || OsgiUtils.toBoolean(globalQueryProp);
        if (!globalQueryRequired) {
        	return providers;
        }
		
		boolean cxfProvidersOnly = OsgiUtils.getBooleanProperty(sd, 
				org.apache.cxf.dosgi.dsw.Constants.RS_PROVIDER_EXPECTED_PROP_KEY);
		
		try {
			ServiceReference[] refs = callingContext.getServiceReferences(null, PROVIDERS_FILTER);			
			if (refs != null) {
				for (ServiceReference ref : refs) {
					if (!cxfProvidersOnly || cxfProvidersOnly 
						&& OsgiUtils.toBoolean(
							ref.getProperty(org.apache.cxf.dosgi.dsw.Constants.RS_PROVIDER_PROP_KEY))) {
				        providers.add(callingContext.getService(ref));
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			LOG.fine("Problems finding JAXRS providers " + ex.getMessage());
		}
		return providers;
    }
	
	private static void loadProviders(BundleContext callingContext, 
			                          List<Object> providers,
			                          String[] classNames) {
		for (String className : classNames) {
			try {
				String realName = className.trim();
				if (realName.length() > 0) {
				    Class<?> pClass = callingContext.getBundle().loadClass(realName);
				    providers.add(pClass.newInstance());
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				LOG.warning("JAXRS Provider " + className.trim() + " can not be loaded or created");
			}
		}
	}
	
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
