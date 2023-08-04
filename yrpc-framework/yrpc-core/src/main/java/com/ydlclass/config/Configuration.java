package com.ydlclass.config;

import com.ydlclass.IdGenerator;
import com.ydlclass.discovery.RegistryConfig;
import com.ydlclass.loadbalancer.LoadBalancer;
import com.ydlclass.loadbalancer.impl.RoundRobinLoadBalancer;
import com.ydlclass.protection.CircuitBreaker;
import com.ydlclass.protection.RateLimiter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Slf4j
public class Configuration {

    private int port = 8094;

    private String appName = "default";

    private String group = "default";

    private RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");

    private String serializeType = "jdk";

    private String compressType = "gzip";

    public IdGenerator idGenerator = new IdGenerator(1, 2);

    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

    private final Map<SocketAddress, RateLimiter> everyIpRateLimiter = new ConcurrentHashMap<>(16);

    private final Map<SocketAddress, CircuitBreaker> everyIpCircuitBreaker = new ConcurrentHashMap<>(16);

    public Configuration() {

        SpiResolver spiResolver = new SpiResolver();
        spiResolver.loadFromSpi(this);

        XmlResolver xmlResolver = new XmlResolver();
        xmlResolver.loadFromXml(this);

    }

    public static void main(String[] args) {
        Configuration configuration = new Configuration();
    }

}
