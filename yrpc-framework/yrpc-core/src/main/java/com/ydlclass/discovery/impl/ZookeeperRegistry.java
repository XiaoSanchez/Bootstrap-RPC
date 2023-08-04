package com.ydlclass.discovery.impl;

import com.ydlclass.Constant;
import com.ydlclass.ServiceConfig;
import com.ydlclass.YrpcBootstrap;
import com.ydlclass.discovery.AbstractRegistry;
import com.ydlclass.exceptions.DiscoveryException;
import com.ydlclass.utils.NetUtils;
import com.ydlclass.utils.zookeeper.ZookeeperNode;
import com.ydlclass.utils.zookeeper.ZookeeperUtils;
import com.ydlclass.watch.UpAndDownWatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZookeeperRegistry extends AbstractRegistry {

    private ZooKeeper zooKeeper;

    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    public ZookeeperRegistry(String connectString, int timeout) {
        this.zooKeeper = ZookeeperUtils.createZookeeper(connectString, timeout);
    }

    @Override
    public void register(ServiceConfig<?> service) {

        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterface().getName();

        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        parentNode = parentNode + "/" + service.getGroup();
        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }

        String node = parentNode + "/" + NetUtils.getIp() + ":"
                + YrpcBootstrap.getInstance().getConfiguration().getPort();
        if (!ZookeeperUtils.exists(zooKeeper, node, null)) {
            ZookeeperNode zookeeperNode = new ZookeeperNode(node, null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.EPHEMERAL);
        }

        if (log.isDebugEnabled()) {
            log.debug("Service {}, has been registered", service.getInterface().getName());
        }
    }

    @Override
    public List<InetSocketAddress> lookup(String serviceName, String group) {

        String serviceNode = Constant.BASE_PROVIDERS_PATH + "/" + serviceName + "/" + group;

        List<String> children = ZookeeperUtils.getChildren(zooKeeper, serviceNode, new UpAndDownWatcher());

        List<InetSocketAddress> inetSocketAddresses = children.stream().map(ipString -> {
            String[] ipAndPort = ipString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.parseInt(ipAndPort[1]);
            return new InetSocketAddress(ip, port);
        }).toList();

        if (inetSocketAddresses.size() == 0) {
            throw new DiscoveryException("No available service consoles are not found.");
        }

        return inetSocketAddresses;
    }
}
