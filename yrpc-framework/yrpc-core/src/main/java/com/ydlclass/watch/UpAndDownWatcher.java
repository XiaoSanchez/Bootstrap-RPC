package com.ydlclass.watch;

import com.ydlclass.NettyBootstrapInitializer;
import com.ydlclass.YrpcBootstrap;
import com.ydlclass.discovery.Registry;
import com.ydlclass.loadbalancer.LoadBalancer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

@Slf4j
public class UpAndDownWatcher implements Watcher {
    @Override
    public void process(WatchedEvent event) {

        if (event.getType() == Event.EventType.NodeChildrenChanged) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "The service is detected [{}] under the node up/offline, the list of service will be removed again ...",
                        event.getPath());
            }
            String serviceName = getServiceName(event.getPath());
            Registry registry = YrpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry();
            ;
            List<InetSocketAddress> addresses = registry.lookup(serviceName,
                    YrpcBootstrap.getInstance().getConfiguration().getGroup());

            for (InetSocketAddress address : addresses) {

                if (!YrpcBootstrap.CHANNEL_CACHE.containsKey(address)) {

                    Channel channel = null;
                    try {
                        channel = NettyBootstrapInitializer.getBootstrap()
                                .connect(address).sync().channel();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    YrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }
            }

            for (Map.Entry<InetSocketAddress, Channel> entry : YrpcBootstrap.CHANNEL_CACHE.entrySet()) {
                if (!addresses.contains(entry.getKey())) {
                    YrpcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                }
            }

            LoadBalancer loadBalancer = YrpcBootstrap.getInstance().getConfiguration().getLoadBalancer();
            loadBalancer.reLoadBalance(serviceName, addresses);

        }
    }

    private String getServiceName(String path) {
        String[] split = path.split("/");
        return split[split.length - 1];
    }
}
