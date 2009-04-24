package org.apache.cxf.dosgi.discovery.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.data.Stat;

public class ChildMonitor implements StatCallback, Watcher {
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        System.out.println("~~~ Got a callback! " + path + "@@@" + rc );
    }

    public void process(WatchedEvent event) {
        System.out.println("??? Got a watcher callback! " + event );
    }

}
