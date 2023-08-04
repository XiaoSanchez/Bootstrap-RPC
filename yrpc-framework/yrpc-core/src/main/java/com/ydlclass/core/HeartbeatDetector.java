package com.ydlclass.core;

import com.ydlclass.NettyBootstrapInitializer;
import com.ydlclass.YrpcBootstrap;
import com.ydlclass.compress.CompressorFactory;
import com.ydlclass.discovery.Registry;
import com.ydlclass.enumeration.RequestType;
import com.ydlclass.serialize.SerializerFactory;
import com.ydlclass.transport.message.YrpcRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class HeartbeatDetector {

    public static void detectHeartbeat(String ServiceName) {

        Registry registry = YrpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry();
        List<InetSocketAddress> addresses = registry.lookup(ServiceName,
                YrpcBootstrap.getInstance().getConfiguration().getGroup());

        for (InetSocketAddress address : addresses) {
            try {
                if (!YrpcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                    Channel channel = NettyBootstrapInitializer.getBootstrap().connect(address).sync().channel();
                    YrpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Thread thread = new Thread(() -> new Timer().scheduleAtFixedRate(new MyTimerTask(), 0, 2000),
                "yrpc-HeartbeatDetector-thread");
        thread.setDaemon(true);
        thread.start();

    }

    private static class MyTimerTask extends TimerTask {

        @Override
        public void run() {

            YrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.clear();

            Map<InetSocketAddress, Channel> cache = YrpcBootstrap.CHANNEL_CACHE;
            for (Map.Entry<InetSocketAddress, Channel> entry : cache.entrySet()) {

                int tryTimes = 3;
                while (tryTimes > 0) {

                    Channel channel = entry.getValue();

                    long start = System.currentTimeMillis();

                    YrpcRequest yrpcRequest = YrpcRequest.builder()
                            .requestId(YrpcBootstrap.getInstance().getConfiguration().getIdGenerator().getId())
                            .compressType(CompressorFactory.getCompressor(YrpcBootstrap.getInstance()
                                    .getConfiguration().getCompressType()).getCode())
                            .requestType(RequestType.HEART_BEAT.getId())
                            .serializeType(SerializerFactory.getSerializer(YrpcBootstrap.getInstance()
                                    .getConfiguration().getSerializeType()).getCode())
                            .timeStamp(start)
                            .build();

                    CompletableFuture<Object> completableFuture = new CompletableFuture<>();

                    YrpcBootstrap.PENDING_REQUEST.put(yrpcRequest.getRequestId(), completableFuture);

                    channel.writeAndFlush(yrpcRequest).addListener((ChannelFutureListener) promise -> {
                        if (!promise.isSuccess()) {
                            completableFuture.completeExceptionally(promise.cause());
                        }
                    });

                    Long endTime = 0L;
                    try {

                        completableFuture.get(1, TimeUnit.SECONDS);
                        endTime = System.currentTimeMillis();
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {

                        tryTimes--;
                        log.error("The host connection of [{}] is abnormal. It is undergoing [{}] to retry ...",
                                channel.remoteAddress(), 3 - tryTimes);

                        if (tryTimes == 0) {
                            YrpcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                        }

                        try {
                            Thread.sleep(10 * (new Random().nextInt(5)));
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }

                        continue;
                    }
                    Long time = endTime - start;

                    YrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.put(time, channel);
                    log.debug("The response time of [{}] The server is [{}].", entry.getKey(), time);
                    break;
                }
            }

            log.info("-----------------------Treemap in response time----------------------");
            for (Map.Entry<Long, Channel> entry : YrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.entrySet()) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}]--->channelId:[{}]", entry.getKey(), entry.getValue().id());
                }
            }
        }
    }

}
