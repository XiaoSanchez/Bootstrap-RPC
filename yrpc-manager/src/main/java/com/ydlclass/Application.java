package com.ydlclass;

import com.ydlclass.utils.zookeeper.ZookeeperNode;
import com.ydlclass.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;

@Slf4j
public class Application {

    public static void main(String[] args) throws InterruptedException {

        ZooKeeper zooKeeper = ZookeeperUtils.createZookeeper();

        String basePath = "/yrpc-metadata";
        String providerPath = basePath + "/providers";
        String consumersPath = basePath + "/consumers";
        ZookeeperNode baseNode = new ZookeeperNode(basePath, null);
        ZookeeperNode providersNode = new ZookeeperNode(providerPath, null);
        ZookeeperNode consumersNode = new ZookeeperNode(consumersPath, null);

        List.of(baseNode, providersNode, consumersNode).forEach(node -> {
            ZookeeperUtils.createNode(zooKeeper, node, null, CreateMode.PERSISTENT);
        });

        ZookeeperUtils.close(zooKeeper);

    }

}
