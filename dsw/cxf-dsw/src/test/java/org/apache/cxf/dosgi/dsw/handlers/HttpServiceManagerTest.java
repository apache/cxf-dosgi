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

import java.util.Dictionary;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.cxf.Bus;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;


public class HttpServiceManagerTest extends TestCase {
    
    public void testGetAbsoluteAddress() {
        IMocksControl c = EasyMock.createControl();
        BundleContext bundleContext = c.createMock(BundleContext.class);
        c.replay();
        HttpServiceManager manager = new HttpServiceManager(bundleContext, null, null, null);
        String localIp = LocalHostUtil.getLocalIp();

        String address1 = manager.getAbsoluteAddress(bundleContext, null, "/myservice");
        assertEquals("http://" + localIp  + ":8181/cxf/myservice", address1);
        
        String address2 = manager.getAbsoluteAddress(bundleContext, "/mycontext", "/myservice");
        assertEquals("http://" + localIp  + ":8181/mycontext/myservice", address2);

        c.verify();
    }
    
    public void testRegisterAndUnregisterServlet() throws Exception {
        IMocksControl c = EasyMock.createControl();
        BundleContext dswContext = c.createMock(BundleContext.class);
        Filter filter = c.createMock(Filter.class);
        expect(dswContext.createFilter(EasyMock.eq("(service.id=12345)"))).andReturn(filter).once();
        Capture<ServiceListener> captured = new Capture<ServiceListener>();
        dswContext.addServiceListener(EasyMock.capture(captured), EasyMock.<String>anyObject());
        expectLastCall().atLeastOnce();
        expect(dswContext.getProperty("org.apache.cxf.httpservice.requirefilter")).andReturn(null).atLeastOnce();
        ServletConfig config = c.createMock(ServletConfig.class);
        expect(config.getInitParameter(EasyMock.<String>anyObject())).andReturn(null).atLeastOnce();
        ServletContext servletContext = c.createMock(ServletContext.class);
        expect(config.getServletContext()).andReturn(servletContext);
        final HttpService httpService = new DummyHttpService(config);
        ServiceReference sr = c.createMock(ServiceReference.class);
        expect(sr.getProperty(EasyMock.eq("service.id"))).andReturn(12345L).atLeastOnce();
        c.replay();
        
        HttpServiceManager h = new HttpServiceManager(dswContext, null, null, null) {

            @Override
            protected HttpService getHttpService() {
                return httpService;
            }
            
        };
        Bus bus = h.registerServletAndGetBus("/myService", dswContext, sr);
        Assert.assertNotNull(bus);
        
        ServiceEvent event = new ServiceEvent(ServiceEvent.UNREGISTERING, sr);
        captured.getValue().serviceChanged(event);
        c.verify();
    } 

    static class DummyHttpService implements HttpService {
        
        private ServletConfig config;

        public DummyHttpService(ServletConfig config) {
            this.config = config;
        }

        @SuppressWarnings("rawtypes")
        public void registerServlet(String alias, Servlet servlet, Dictionary initparams, HttpContext context)
            throws ServletException, NamespaceException {
            Assert.assertEquals("/myService", alias);
            servlet.init(config);
        }

        public void registerResources(String alias, String name, HttpContext context) throws NamespaceException {
            throw new RuntimeException("This method should not be called");
        }

        public void unregister(String alias) {

        }

        public HttpContext createDefaultHttpContext() {
            return EasyMock.createNiceMock(HttpContext.class);
        }
        
    }
}
