package com.ydlclass.loadbalancer.impl;

import com.ydlclass.YrpcBootstrap;
import com.ydlclass.loadbalancer.AbstractLoadBalancer;
import com.ydlclass.loadbalancer.Selector;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class MinimumResponseTimeLoadBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new MinimumResponseTimeSelector(serviceList);
    }

    private static class MinimumResponseTimeSelector implements Selector {

        public MinimumResponseTimeSelector(List<InetSocketAddress> serviceList) {

        }

        @Override
        public InetSocketAddress getNext() {
            Map.Entry<Long, Channel> entry = YrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry();
            if (entry != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Select the service node of the response time to [{} ms].", entry.getKey());
                }
                return (InetSocketAddress) entry.getValue().remoteAddress();
            }

            System.out.println("----->" + Arrays.toString(YrpcBootstrap.CHANNEL_CACHE.values().toArray()));
            Channel channel = (Channel) YrpcBootstrap.CHANNEL_CACHE.values().toArray()[0];
            return (InetSocketAddress) channel.remoteAddress();
        }

    }
}
