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
package org.apache.cxf.dosgi.dsw;

public final class ClassUtils {
    private ClassUtils() {}
    
    public static Class<?> getInterfaceClass(Object service, String interfaceName) {
        return getInterfaceClass(service.getClass(), interfaceName);
    }
    
    private static Class<?> getInterfaceClass(Class<?> serviceClass, String interfaceName) {
        for (Class<?> iClass : serviceClass.getInterfaces()) {
            if (iClass.getName().equals(interfaceName)) {
                return iClass;
            }
            Class<?> intf = getInterfaceClass(iClass, interfaceName);
            if (intf != null) {
                return intf;
            }
        }
        
        if (serviceClass.getName().equals(interfaceName)) {
            return serviceClass;
        }
        
        Class<?> interfaceOnProxiedClass = getInterfaceClassOnSuperClasses(serviceClass, interfaceName);
        if (interfaceOnProxiedClass != null){
        	return interfaceOnProxiedClass;
        }
        
        return null;
    }    
    
    /**
     * <pre>
     * 
     * The following method tries to deal specifically with classes that might have been proxied 
     * eg. CGLIB proxies of which there might be a chain of proxies as different osgi frameworks
     * might be proxying the original service class that has been registered and then proxying the proxy.
     * 
     * </pre>
     * 
     * @param serviceClass
     * @param interfaceName
     * @return
     */
    private static Class<?> getInterfaceClassOnSuperClasses(Class<?> serviceClass, String interfaceName){
        Class<?> superClass = serviceClass.getSuperclass();
		if (superClass != null){
		    for (Class<?> iClass : superClass.getInterfaces()) {
	            if (iClass.getName().equals(interfaceName)) {
	                return iClass;
	            }
	            Class<?> intf = getInterfaceClass(iClass, interfaceName);
	            if (intf != null) {
	                return intf;
	            }
	        }
		    Class<?> foundOnSuperclass = getInterfaceClassOnSuperClasses(superClass, interfaceName);
		    if (foundOnSuperclass != null){
		    	return foundOnSuperclass;
		    }
		}
    	return null;
    }
}
