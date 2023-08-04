package com.ydlclass.loadbalancer;

import java.net.InetSocketAddress;
import java.util.List;

public interface LoadBalancer {
    InetSocketAddress selectServiceAddress(String serviceName, String group);

    void reLoadBalance(String serviceName, List<InetSocketAddress> addresses);
}
