package com.ydlclass.discovery;

import com.ydlclass.ServiceConfig;

import java.net.InetSocketAddress;
import java.util.List;

public interface Registry {

    void register(ServiceConfig<?> serviceConfig);

    List<InetSocketAddress> lookup(String serviceName, String group);

}
