package com.ydlclass.proxy.handler;

import com.ydlclass.NettyBootstrapInitializer;
import com.ydlclass.YrpcBootstrap;
import com.ydlclass.annotation.TryTimes;
import com.ydlclass.compress.CompressorFactory;
import com.ydlclass.discovery.Registry;
import com.ydlclass.enumeration.RequestType;
import com.ydlclass.exceptions.DiscoveryException;
import com.ydlclass.exceptions.NetworkException;
import com.ydlclass.protection.CircuitBreaker;
import com.ydlclass.serialize.SerializerFactory;
import com.ydlclass.transport.message.RequestPayload;
import com.ydlclass.transport.message.YrpcRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class RpcConsumerInvocationHandler implements InvocationHandler {

    private final Registry registry;
    private final Class<?> interfaceRef;
    private String group;

    public RpcConsumerInvocationHandler(Registry registry, Class<?> interfaceRef, String group) {
        this.registry = registry;
        this.interfaceRef = interfaceRef;
        this.group = group;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        TryTimes tryTimesAnnotation = method.getAnnotation(TryTimes.class);

        int tryTimes = 0;
        int intervalTime = 0;
        if (tryTimesAnnotation != null) {
            tryTimes = tryTimesAnnotation.tryTimes();
            intervalTime = tryTimesAnnotation.intervalTime();
        }

        while (true) {
            RequestPayload requestPayload = RequestPayload.builder()
                    .interfaceName(interfaceRef.getName())
                    .methodName(method.getName())
                    .parametersType(method.getParameterTypes())
                    .parametersValue(args)
                    .returnType(method.getReturnType())
                    .build();

            YrpcRequest yrpcRequest = YrpcRequest.builder()
                    .requestId(YrpcBootstrap.getInstance().getConfiguration().getIdGenerator().getId())
                    .compressType(CompressorFactory
                            .getCompressor(YrpcBootstrap.getInstance().getConfiguration().getCompressType()).getCode())
                    .requestType(RequestType.REQUEST.getId())
                    .serializeType(SerializerFactory
                            .getSerializer(YrpcBootstrap.getInstance().getConfiguration().getSerializeType()).getCode())
                    .timeStamp(System.currentTimeMillis())
                    .requestPayload(requestPayload)
                    .build();

            YrpcBootstrap.REQUEST_THREAD_LOCAL.set(yrpcRequest);

            InetSocketAddress address = YrpcBootstrap.getInstance()
                    .getConfiguration().getLoadBalancer().selectServiceAddress(interfaceRef.getName(), group);
            if (log.isDebugEnabled()) {
                log.debug("The service call party, found the service [{}] available host [{}].",
                        interfaceRef.getName(), address);
            }

            Map<SocketAddress, CircuitBreaker> everyIpCircuitBreaker = YrpcBootstrap.getInstance()
                    .getConfiguration().getEveryIpCircuitBreaker();
            CircuitBreaker circuitBreaker = everyIpCircuitBreaker.get(address);
            if (circuitBreaker == null) {
                circuitBreaker = new CircuitBreaker(10, 0.5F);
                everyIpCircuitBreaker.put(address, circuitBreaker);
            }

            try {

                if (yrpcRequest.getRequestType() != RequestType.HEART_BEAT.getId() && circuitBreaker.isBreak()) {

                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            YrpcBootstrap.getInstance()
                                    .getConfiguration().getEveryIpCircuitBreaker()
                                    .get(address).reset();
                        }
                    }, 5000);

                    throw new RuntimeException(
                            "The current circuit breaker has been opened, and the request cannot be sent");
                }

                Channel channel = getAvailableChannel(address);
                if (log.isDebugEnabled()) {
                    log.debug("Get the connection channel established and [{}], ready to send data.", address);
                }

                CompletableFuture<Object> completableFuture = new CompletableFuture<>();

                YrpcBootstrap.PENDING_REQUEST.put(yrpcRequest.getRequestId(), completableFuture);

                channel.writeAndFlush(yrpcRequest).addListener((ChannelFutureListener) promise -> {

                    if (!promise.isSuccess()) {
                        completableFuture.completeExceptionally(promise.cause());
                    }
                });

                YrpcBootstrap.REQUEST_THREAD_LOCAL.remove();

                Object result = completableFuture.get(10, TimeUnit.SECONDS);

                circuitBreaker.recordRequest();
                return result;
            } catch (Exception e) {

                tryTimes--;

                circuitBreaker.recordErrorRequest();
                try {
                    Thread.sleep(intervalTime);
                } catch (InterruptedException ex) {
                    log.error("An abnormalities occur during retry.", ex);
                }
                if (tryTimes < 0) {
                    log.error(
                            "When the method [{}] is made remotely, the time {} times will be repeated, and it is still not available",
                            method.getName(), tryTimes, e);
                    break;
                }
                log.error("An abnormalities occur during the secondary trial.", 3 - tryTimes, e);
            }
        }
        throw new RuntimeException("Execute the remote method" + method.getName() + "The call failed.");
    }

    private Channel getAvailableChannel(InetSocketAddress address) {

        Channel channel = YrpcBootstrap.CHANNEL_CACHE.get(address);

        if (channel == null) {

            CompletableFuture<Channel> channelFuture = new CompletableFuture<>();
            NettyBootstrapInitializer.getBootstrap().connect(address).addListener(
                    (ChannelFutureListener) promise -> {
                        if (promise.isDone()) {

                            if (log.isDebugEnabled()) {
                                log.debug("The connection has been successfully established with [{}].", address);
                            }
                            channelFuture.complete(promise.channel());
                        } else if (!promise.isSuccess()) {
                            channelFuture.completeExceptionally(promise.cause());
                        }
                    });

            try {
                channel = channelFuture.get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("When obtaining the passage, abnormalities occur.", e);
                throw new DiscoveryException(e);
            }

            YrpcBootstrap.CHANNEL_CACHE.put(address, channel);
        }

        if (channel == null) {
            log.error("Anomalial occurred when obtaining or establishing a channel with [{}].", address);
            throw new NetworkException("An abnormalities occurred when obtaining the channel.");
        }

        return channel;
    }
}
