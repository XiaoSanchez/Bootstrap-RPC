package com.ydlclass.discovery.impl;

import com.ydlclass.Constant;
import com.ydlclass.ServiceConfig;
import com.ydlclass.discovery.AbstractRegistry;
import com.ydlclass.utils.NetUtils;
import com.ydlclass.utils.zookeeper.ZookeeperNode;
import com.ydlclass.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class NacosRegistry extends AbstractRegistry {

    private ZooKeeper zooKeeper;

    public NacosRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    public NacosRegistry(String connectString, int timeout) {
        this.zooKeeper = ZookeeperUtils.createZookeeper(connectString, timeout);
    }

    @Override
    public void register(ServiceConfig<?> service) {

        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterface().getName();

        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        String node = parentNode + "/" + NetUtils.getIp() + ":" + 8088;
        if (!ZookeeperUtils.exists(zooKeeper, node, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(node, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.EPHEMERAL);
        }

        if (log.isDebugEnabled()) {
            log.debug("Service {}, has been registered", service.getInterface().getName());
        }
    }

    @Override
    public List<InetSocketAddress> lookup(String name, String group) {
        return null;
    }
}
