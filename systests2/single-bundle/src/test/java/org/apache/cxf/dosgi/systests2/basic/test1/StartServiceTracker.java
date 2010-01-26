package org.apache.cxf.dosgi.systests2.basic.test1;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class StartServiceTracker extends ServiceTracker {
    private ServiceTracker tracker;

    public StartServiceTracker(BundleContext context, Filter filter, ServiceTracker tracker) {
        super(context, filter, null);
        this.tracker = tracker;
    }

    @Override
    public Object addingService(ServiceReference reference) {
        tracker.open();
        return super.addingService(reference);
    }
}
