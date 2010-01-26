package org.apache.cxf.dosgi.systests2.basic;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.dosgi.samples.greeter.GreeterData;
import org.apache.cxf.dosgi.samples.greeter.GreeterException;
import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.apache.cxf.dosgi.systests2.basic.test1.MyActivator;
import org.apache.cxf.dosgi.systests2.basic.test1.MyServiceTracker;
import org.apache.cxf.dosgi.systests2.basic.test1.StartServiceTracker;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

@RunWith( JUnit4TestRunner.class )
public class TestImportService {
    @Inject
    BundleContext bundleContext = null;

    @Configuration
    public static Option[] configure() {
        InputStream testClientBundle = TinyBundles.newBundle()
            .add(MyActivator.class)
            .add(MyServiceTracker.class)
            .add(StartServiceTracker.class)
            .add("OSGI-INF/remote-service/remote-services.xml", TestImportService.class.getResource("/rs-test1.xml"))
            .set(Constants.BUNDLE_SYMBOLICNAME, "testClientBundle")
            .set(Constants.EXPORT_PACKAGE, "org.apache.cxf.dosgi.systests2.common.test1")
            .set(Constants.BUNDLE_ACTIVATOR, MyActivator.class.getName())
            .build(TinyBundles.withBnd());
        
        return CoreOptions.options(
                CoreOptions.mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi").artifactId("cxf-dosgi-ri-singlebundle-distribution").versionAsInProject(),
                CoreOptions.mavenBundle().groupId("org.apache.cxf.dosgi.samples").artifactId("cxf-dosgi-ri-samples-greeter-interface").versionAsInProject(),
                CoreOptions.provision(testClientBundle)
        );
    }

    @Test
    public void testClientConsumer() throws Exception {
        // Set up a Server in the test
        ServerFactoryBean factory = new ServerFactoryBean();
        factory.setServiceClass(GreeterService.class);
        factory.setAddress("http://localhost:9191/grrr");
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());
        factory.setServiceBean(new TestGreeter());
        
        Server server = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ServerFactoryBean.class.getClassLoader());
            server = factory.create();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
        
//        JOptionPane.showMessageDialog(null, factory.getAddress());
        
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("testName", "test1");
        bundleContext.registerService(Object.class.getName(), new Object(), props);

        // Wait for the service tracker in the test bundle to register a service with the test result
        ServiceReference ref = waitService(String.class.getName(), "(testResult=test1)");
        Assert.assertEquals("HiOSGi", ref.getProperty("result"));
        server.stop(); // in finally !!!
    }
    
    private ServiceReference waitService(String cls, String filter) throws Exception {        
        ServiceReference[] refs = null;
        for (int i=0; i < 20; i++) {
            refs = bundleContext.getServiceReferences(cls, filter);
            if (refs != null && refs.length > 0) {
                return refs[0];
            }
            System.out.println("Waiting for service: " + cls + filter);
            Thread.sleep(1000);
        }
        throw new Exception("Service not found: " + cls + filter);
    }
    
    public static class TestGreeter implements GreeterService {
        public Map<GreetingPhrase, String> greetMe(String name) {
            Map<GreetingPhrase, String> m = new HashMap<GreetingPhrase, String>();
            GreetingPhrase gp = new GreetingPhrase("Hi");
            m.put(gp, name);
            return m;
        }

        public GreetingPhrase[] greetMe(GreeterData gd) throws GreeterException {
            throw new GreeterException("TestGreeter");
        }      
    }
}
