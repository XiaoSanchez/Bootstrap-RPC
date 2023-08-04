package com.ydlclass.utils.zookeeper;

import com.ydlclass.Constant;
import com.ydlclass.exceptions.ZookeeperException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ZookeeperUtils {

    public static ZooKeeper createZookeeper() {
        String connectString = Constant.DEFAULT_ZK_CONNECT;
        int timeout = Constant.TIME_OUT;
        return createZookeeper(connectString, timeout);
    }

    public static ZooKeeper createZookeeper(String connectString, int timeout) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            final ZooKeeper zooKeeper = new ZooKeeper(connectString, timeout, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    log.debug("The client has been successfully connected.");
                    countDownLatch.countDown();
                }
            });

            countDownLatch.await();
            return zooKeeper;
        } catch (IOException | InterruptedException e) {
            log.error("Abnormal occurred when creating a zookeeper instance:", e);
            throw new ZookeeperException();
        }
    }

    public static Boolean createNode(ZooKeeper zooKeeper, ZookeeperNode node, Watcher watcher, CreateMode createMode) {
        try {
            if (zooKeeper.exists(node.getNodePath(), watcher) == null) {
                String result = zooKeeper.create(node.getNodePath(), node.getData(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
                log.info("Node [{}], successfully created.", result);
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.info("Nodes [{}] already exist and do not need to be created.", node.getNodePath());
                }
                return false;
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("An abnormalities occur when creating the basic directory:", e);
            throw new ZookeeperException();
        }
    }

    public static boolean exists(ZooKeeper zk, String node, Watcher watcher) {
        try {
            return zk.exists(node, watcher) != null;
        } catch (KeeperException | InterruptedException e) {
            log.error("Determine the node [{}] Whether the existence is abnormal", node, e);
            throw new ZookeeperException(e);
        }
    }

    public static void close(ZooKeeper zooKeeper) {
        try {
            zooKeeper.close();
        } catch (InterruptedException e) {
            log.error("There is a problem with the problem when turning off the zookeeper:", e);
            throw new ZookeeperException();
        }
    }

    public static List<String> getChildren(ZooKeeper zooKeeper, String serviceNode, Watcher watcher) {
        try {
            return zooKeeper.getChildren(serviceNode, watcher);
        } catch (KeeperException | InterruptedException e) {
            log.error("Abnormal occurrence of sub -elements in obtaining nodes [{}] occur.", serviceNode, e);
            throw new ZookeeperException(e);
        }
    }
}
