package org.apache.cxf.dosgi.discovery.zookeeper;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the EndpointListeners and the scopes they are interested in.
 * For each scope with interested EndpointListeners an InterfaceMonitor is created.
 * The InterfaceMonitor calls back when it detects added or removed external Endpoints.
 * These events are then forwarded to all interested EndpointListeners
 */
public class InterfaceMonitorManager {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMonitorManager.class);
    
    private final ZooKeeper zooKeeper;
    private final Map<ServiceReference, List<String> /* scopes of the epl */> handledEndpointlisteners = new HashMap<ServiceReference, List<String>>();
    private final Map<String /* scope */, Interest> interestingScopes = new HashMap<String, Interest>();
    private final BundleContext bctx;

    protected static class Interest {
        List<ServiceReference> relatedServiceListeners = new ArrayList<ServiceReference>(1);
        InterfaceMonitor im;
    }
    
    public InterfaceMonitorManager(BundleContext bctx, ZooKeeper zooKeeper) {
        this.bctx = bctx;
        this.zooKeeper = zooKeeper;
    }
    
    void addInterest(ServiceReference sref, String scope, String objClass) {
        synchronized (interestingScopes) {
            synchronized (handledEndpointlisteners) {
                Interest interest = interestingScopes.get(scope);
                if (interest == null) {
                    interest = new Interest();
                    interestingScopes.put(scope, interest);
                }
                
                if (!interest.relatedServiceListeners.contains(sref)) {
                    interest.relatedServiceListeners.add(sref);
                }

                if (interest.im != null) {
                    // close old Monitor
                    interest.im.close();
                    interest.im = null;
                }
                
                InterfaceMonitor dm = createInterfaceMonitor(scope, objClass, interest);
                dm.start();
                interest.im = dm;

                List<String> handledScopes = handledEndpointlisteners.get(sref);
                if (handledScopes == null) {
                    handledScopes = new ArrayList<String>(1);
                    handledEndpointlisteners.put(sref, handledScopes);
                }

                if (!handledScopes.contains(scope))
                    handledScopes.add(scope);

            }
        }
    }
    
    /**
     * Only for test case !
     * */
    protected Map<String, Interest> getInterestingScopes() {
        return interestingScopes;
    }

    /**
     * Only for test case !
     * */
    protected Map<ServiceReference, List<String>>  getHandledEndpointlisteners() {
        return handledEndpointlisteners;
    }
    
    protected InterfaceMonitor createInterfaceMonitor(String scope, String objClass, final Interest interest) {
        EndpointListener epListener = new EndpointListener() {
            public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
                notifyListeners(endpoint, false, interest.relatedServiceListeners);
            }
            
            public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
                notifyListeners(endpoint, true, interest.relatedServiceListeners);
            }
        };
        return new InterfaceMonitor(zooKeeper, objClass, epListener, scope, bctx);
    }

    public void removeInterest(ServiceReference sref) {
        List<String> handledScopes = handledEndpointlisteners.get(sref);
        if (handledScopes == null) {
               return;
        }

        for (String scope : handledScopes) {
            Interest i = interestingScopes.get(scope);
            if (i != null) {
                i.relatedServiceListeners.remove(sref);
                if (i.relatedServiceListeners.size() == 0) {
                    i.im.close();
                    interestingScopes.remove(scope);
                }
            }
        }
        handledEndpointlisteners.remove(sref);
    }
    
    private void notifyListeners(EndpointDescription epd, boolean isAdded,
            List<ServiceReference> relatedServiceListeners) {
        for (ServiceReference sref : relatedServiceListeners) {
            Object service = bctx.getService(sref);
            if (service == null || !(service instanceof EndpointListener)) {
                continue;
            }
            EndpointListener epl = (EndpointListener) service;
            String[] scopes = Util.getScopes(sref);
            for (final String currentScope : scopes) {
                LOG.debug("matching {} against {}", epd, currentScope);
                if (matches(currentScope, epd)) {
                    LOG.debug("Matched {} against {}", epd, currentScope);
                    if (isAdded) {
                        LOG.info("calling EndpointListener.endpointAdded: " + epl + "from bundle "
                                + sref.getBundle().getSymbolicName() + " for endpoint: " + epd);
                        epl.endpointAdded(epd, currentScope);
                    } else {
                        LOG.info("calling EndpointListener.endpointRemoved: " + epl + "from bundle "
                                + sref.getBundle().getSymbolicName() + " for endpoint: " + epd);
                        epl.endpointRemoved(epd, currentScope);
                    }
                    break;
                }
            }
        }
    }
    
    private boolean matches(String scope, EndpointDescription epd) {
        try {
            Filter f = FrameworkUtil.createFilter(scope);
            Dictionary<String, Object> dict = mapToDictionary(epd.getProperties());
            return f.match(dict);
        } catch (InvalidSyntaxException e) {
            LOG.error("Currentscope [" + scope + "] resulted in" + " a bad filter!", e);
            return false;
        }
    }

    private Dictionary<String, Object> mapToDictionary(Map<String, Object> map) {
        Dictionary<String, Object> d = new Hashtable<String, Object>();
        Set<Map.Entry<String, Object>> entries = map.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            d.put(entry.getKey(), entry.getValue());
        }
        return d;
    }

    public void close() {
        for (String scope : interestingScopes.keySet()) {
            Interest interest = interestingScopes.get(scope);
            interest.im.close();
        }
    }
}
