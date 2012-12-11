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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceException;

public class ServiceInvocationHandler implements InvocationHandler {
    private static final String REMOTE_EXCEPTION_TYPE = "REMOTE";
    private static final Collection<Method> OBJECT_METHODS = 
        Arrays.asList(Object.class.getMethods());

    private Map<Method, List<Class<?>>> exceptionsMap
        = new HashMap<Method, List<Class<?>>>();
    private Object serviceObject;
    
    public ServiceInvocationHandler(Object serviceObject, Class<?> iType) {
        this.serviceObject = serviceObject;
        introspectType(iType);
    }
    
    public Object invoke(Object proxy, final Method m, Object[] params) throws Throwable {
        if (OBJECT_METHODS.contains(m)) {
            if (m.getName().equals("equals")) {
                params = new Object[] {Proxy.getInvocationHandler(params[0])};
            }
            return m.invoke(this, params);
        }

        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {            
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            final Object[] paramsFinal = params;
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    return m.invoke(serviceObject, paramsFinal);
                }
            }); 
        } catch (Throwable ex) {
            Throwable theCause = ex.getCause() == null ? ex : ex.getCause();
            Throwable theCauseCause = theCause.getCause() == null ? theCause : theCause.getCause();
            List<Class<?>> excTypes = exceptionsMap.get(m);
            if (excTypes != null) {
                for (Class<?> type : excTypes) {
                    if (type.isAssignableFrom(theCause.getClass())) {
                        throw theCause;
                    }
                    if (type.isAssignableFrom(theCauseCause.getClass())) {
                        throw theCauseCause;
                    }
                }
                
            }
                        
            throw new ServiceException(REMOTE_EXCEPTION_TYPE, theCause);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    private void introspectType(Class<?> iType) {
        for (Method m : iType.getDeclaredMethods()) {
            for (Class<?> excType : m.getExceptionTypes()) {
                if (Exception.class.isAssignableFrom(excType)) {
                    List<Class<?>> types = exceptionsMap.get(m);
                    if (types == null) {
                        types = new ArrayList<Class<?>>();
                        exceptionsMap.put(m, types);
                    }
                    types.add(excType);
                }
            }
        }
    }
}
