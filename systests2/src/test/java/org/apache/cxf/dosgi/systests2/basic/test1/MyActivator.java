package org.apache.cxf.dosgi.systests2.basic.test1;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;

public class MyActivator implements BundleActivator {    
    private ServiceTracker startTracker, tracker;

    public void start(final BundleContext bc) throws Exception {
        Filter filter = bc.createFilter("(&(objectClass=java.lang.Object)(testName=test1))");
        tracker = new MyServiceTracker(bc);
        
        // The start tracker waits until a service from the test class is set before the 
        // 'MyServiceTracker' is activated.
        startTracker = new StartServiceTracker(bc, filter, tracker);
        startTracker.open();
    }
    
    public void stop(BundleContext bc) throws Exception {
        startTracker.close();
        tracker.close();
    }
}
