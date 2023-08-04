package com.ydlclass.discovery;

import com.ydlclass.Constant;
import com.ydlclass.discovery.impl.NacosRegistry;
import com.ydlclass.discovery.impl.ZookeeperRegistry;
import com.ydlclass.exceptions.DiscoveryException;

public class RegistryConfig {

    private final String connectString;

    public RegistryConfig(String connectString) {
        this.connectString = connectString;
    }

    public Registry getRegistry() {

        String registryType = getRegistryType(connectString, true).toLowerCase().trim();

        if (registryType.equals("zookeeper")) {
            String host = getRegistryType(connectString, false);
            return new ZookeeperRegistry(host, Constant.TIME_OUT);
        } else if (registryType.equals("nacos")) {
            String host = getRegistryType(connectString, false);
            return new NacosRegistry(host, Constant.TIME_OUT);
        }
        throw new DiscoveryException("No appropriate registration center was found.");
    }

    private String getRegistryType(String connectString, boolean ifType) {
        String[] typeAndHost = connectString.split("://");
        if (typeAndHost.length != 2) {
            throw new RuntimeException("The connection of a given registration center is illegal");
        }
        if (ifType) {
            return typeAndHost[0];
        } else {
            return typeAndHost[1];
        }
    }

}
