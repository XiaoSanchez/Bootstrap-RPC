package com.ydlclass;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ZookeeperTest {

    ZooKeeper zooKeeper;
    CountDownLatch countDownLatch = new CountDownLatch(1);

    @Before
    public void createZk() {

        String connectString = "127.0.0.1:2181";

        int timeout = 10000;

        try {

            zooKeeper = new ZooKeeper(connectString, timeout, event -> {

                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    System.out.println("The client has been successfully connected.");
                    countDownLatch.countDown();
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCreatePNode() {
        try {

            countDownLatch.await();
            zooKeeper.setData("/ydlclass", "hi".getBytes(), -1);
            String result = zooKeeper.create("/ydlclass", "hello".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            System.out.println("result = " + result);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zooKeeper != null) {
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testDeletePNode() {
        try {

            zooKeeper.delete("/ydlclass", -1);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zooKeeper != null) {
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testExistsPNode() {
        try {

            Stat stat = zooKeeper.exists("/ydlclass", null);

            zooKeeper.setData("/ydlclass", "hi".getBytes(), -1);

            int version = stat.getVersion();
            System.out.println("version = " + version);

            int aversion = stat.getAversion();
            System.out.println("aversion = " + aversion);

            int cversion = stat.getCversion();
            System.out.println("cversion = " + cversion);

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zooKeeper != null) {
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testWatcher() {
        try {

            zooKeeper.exists("/ydlclass", true);

            while (true) {
                Thread.sleep(1000);
            }

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zooKeeper != null) {
                    zooKeeper.close();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
