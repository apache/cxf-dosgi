package org.apache.cxf.dosgi.systests2.basic.test1;

import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.dosgi.samples.greeter.GreeterService;
import org.apache.cxf.dosgi.samples.greeter.GreetingPhrase;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class MyServiceTracker extends ServiceTracker {
    private static StringBuffer invocationResult = new StringBuffer();

    public MyServiceTracker(BundleContext context) {
        super(context, GreeterService.class.getName(), null);
    }

    public Object addingService(ServiceReference reference) {
        Object svc = super.addingService(reference);
        if (svc instanceof GreeterService) {
            invokeGreeter((GreeterService) svc);
        }
        return svc;
    }
    
    public static String getResult() {
        return invocationResult.toString();
    }

    private void invokeGreeter(GreeterService svc) {
        Map<GreetingPhrase, String> result = svc.greetMe("OSGi");
        for (Map.Entry<GreetingPhrase, String> e : result.entrySet()) {
            GreetingPhrase key = e.getKey();
            invocationResult.append(key.getPhrase());
            invocationResult.append(e.getValue());
        }
        
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("result", invocationResult.toString());
        props.put("testResult", "test1");
        context.registerService(String.class.getName(), "test1", props);
    }    
}