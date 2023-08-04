package com.ydlclass.netty;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class MyWatcher implements Watcher {
    @Override
    public void process(WatchedEvent event) {

        if (event.getType() == Event.EventType.None) {
            if (event.getState() == Event.KeeperState.SyncConnected) {
                System.out.println("Zookeeper connection successfully");
            } else if (event.getState() == Event.KeeperState.AuthFailed) {
                System.out.println("Zookeeper certification failed");
            } else if (event.getState() == Event.KeeperState.Disconnected) {
                System.out.println("zookeeper disconnect connection");
            }

        } else if (event.getType() == Event.EventType.NodeCreated) {
            System.out.println(event.getPath() + "Created");
        } else if (event.getType() == Event.EventType.NodeDeleted) {
            System.out.println(event.getPath() + "Deleted");
        } else if (event.getType() == Event.EventType.NodeDataChanged) {
            System.out.println(event.getPath() + "The data of the node has changed");
        } else if (event.getType() == Event.EventType.NodeChildrenChanged) {
            System.out.println(event.getPath() + "Sub -node changes");
        }

    }
}
