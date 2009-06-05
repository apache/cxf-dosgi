package org.apache.cxf.dosgi.discovery.zookeeper;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class Activator implements BundleActivator, ManagedService {
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());
    
    private BundleContext bundleContext;
    private DiscoveryDriver driver;
    ServiceRegistration cmReg;

    public void start(BundleContext bc) throws Exception {
        bundleContext = bc;        
        cmReg = bc.registerService(ManagedService.class.getName(), this, getCMDefaults());
    }

    private Dictionary getCMDefaults() {
        Dictionary props = new Hashtable();
        props.put("zookeeper.timeout", "3000");
        props.put("zookeeper.port", "2181");
        props.put(Constants.SERVICE_PID, "org.apache.cxf.dosgi.discovery.zookeeper");
        return props;    
    }

    public synchronized void stop(BundleContext bc) throws Exception {
        cmReg.unregister();
        
        if (driver != null) {
            driver.destroy();
        }
    }

    public void updated(Dictionary configuration) throws ConfigurationException {
        if (configuration == null) {
            return;
        }
        
        Dictionary effective = getCMDefaults();
        // apply all values on top of the defaults
        for (Enumeration e = configuration.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            if (key != null) {
                Object val = configuration.get(key);
                effective.put(key, val);
            }
        }
        
        cmReg.setProperties(effective);
        synchronized (this) {
            try {
                if (driver == null) {
                    driver = createDriver(effective);
                } else {
                    driver.updateConfiguration(effective);
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Could now create the ZooKeeper client", e);
            }
        }
    }

    // Isolated for testing
    DiscoveryDriver createDriver(Dictionary configuration)
            throws IOException, ConfigurationException {
        return new DiscoveryDriver(bundleContext, configuration);
    }
}
