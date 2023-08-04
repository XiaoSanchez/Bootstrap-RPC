package com.ydlclass;

import com.ydlclass.annotation.YrpcApi;
import com.ydlclass.channelhandler.handler.MethodCallHandler;
import com.ydlclass.channelhandler.handler.YrpcRequestDecoder;
import com.ydlclass.channelhandler.handler.YrpcResponseEncoder;
import com.ydlclass.config.Configuration;
import com.ydlclass.core.HeartbeatDetector;
import com.ydlclass.core.YrpcShutdownHook;
import com.ydlclass.discovery.RegistryConfig;
import com.ydlclass.loadbalancer.LoadBalancer;
import com.ydlclass.transport.message.YrpcRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class YrpcBootstrap {

    private static final YrpcBootstrap yrpcBootstrap = new YrpcBootstrap();

    private final Configuration configuration;

    public static final ThreadLocal<YrpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    public final static TreeMap<Long, Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();

    public final static Map<String, ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

    public final static Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);

    private YrpcBootstrap() {

        configuration = new Configuration();
    }

    public static YrpcBootstrap getInstance() {
        return yrpcBootstrap;
    }

    public YrpcBootstrap application(String appName) {
        configuration.setAppName(appName);
        return this;
    }

    public YrpcBootstrap registry(RegistryConfig registryConfig) {

        configuration.setRegistryConfig(registryConfig);
        return this;
    }

    public YrpcBootstrap loadBalancer(LoadBalancer loadBalancer) {
        configuration.setLoadBalancer(loadBalancer);
        return this;
    }

    public YrpcBootstrap publish(ServiceConfig<?> service) {

        configuration.getRegistryConfig().getRegistry().register(service);

        SERVERS_LIST.put(service.getInterface().getName(), service);
        return this;
    }

    public YrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service : services) {
            this.publish(service);
        }
        return this;
    }

    public void start() {

        Runtime.getRuntime().addShutdownHook(new YrpcShutdownHook());

        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup(10);
        try {

            ServerBootstrap serverBootstrap = new ServerBootstrap();

            serverBootstrap = serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {

                            socketChannel.pipeline().addLast(new LoggingHandler())
                                    .addLast(new YrpcRequestDecoder())

                                    .addLast(new MethodCallHandler())
                                    .addLast(new YrpcResponseEncoder());
                        }
                    });

            ChannelFuture channelFuture = serverBootstrap.bind(configuration.getPort()).sync();

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                boss.shutdownGracefully().sync();
                worker.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public YrpcBootstrap reference(ReferenceConfig<?> reference) {

        HeartbeatDetector.detectHeartbeat(reference.getInterface().getName());

        reference.setRegistry(configuration.getRegistryConfig().getRegistry());
        reference.setGroup(this.getConfiguration().getGroup());
        return this;
    }

    public YrpcBootstrap serialize(String serializeType) {
        configuration.setSerializeType(serializeType);
        if (log.isDebugEnabled()) {
            log.debug("We configure the serialization method used as [{}】.", serializeType);
        }
        return this;
    }

    public YrpcBootstrap compress(String compressType) {
        configuration.setCompressType(compressType);
        if (log.isDebugEnabled()) {
            log.debug("We configure the compression algorithm used as [{}】.", compressType);
        }
        return this;
    }

    public YrpcBootstrap scan(String packageName) {

        List<String> classNames = getAllClassNames(packageName);

        List<Class<?>> classes = classNames.stream()
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(clazz -> clazz.getAnnotation(YrpcApi.class) != null)
                .collect(Collectors.toList());

        for (Class<?> clazz : classes) {

            Class<?>[] interfaces = clazz.getInterfaces();
            Object instance = null;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            YrpcApi yrpcApi = clazz.getAnnotation(YrpcApi.class);
            String group = yrpcApi.group();

            for (Class<?> anInterface : interfaces) {
                ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterface(anInterface);
                serviceConfig.setRef(instance);
                serviceConfig.setGroup(group);
                if (log.isDebugEnabled()) {
                    log.debug("---->Published by scanning through bags, publishing the service [{}].", anInterface);
                }

                publish(serviceConfig);
            }

        }
        return this;
    }

    private List<String> getAllClassNames(String packageName) {

        String basePath = packageName.replaceAll("\\.", "/");
        URL url = ClassLoader.getSystemClassLoader().getResource(basePath);
        if (url == null) {
            throw new RuntimeException("When scanning, I find that the path does not exist.");
        }
        String absolutePath = url.getPath();

        List<String> classNames = new ArrayList<>();
        classNames = recursionFile(absolutePath, classNames, basePath);

        return classNames;
    }

    private List<String> recursionFile(String absolutePath, List<String> classNames, String basePath) {

        File file = new File(absolutePath);

        if (file.isDirectory()) {

            File[] children = file
                    .listFiles(pathname -> pathname.isDirectory() || pathname.getPath().contains(".class"));
            if (children == null || children.length == 0) {
                return classNames;
            }
            for (File child : children) {
                if (child.isDirectory()) {

                    recursionFile(child.getAbsolutePath(), classNames, basePath);
                } else {

                    String className = getClassNameByAbsolutePath(child.getAbsolutePath(), basePath);
                    classNames.add(className);
                }
            }

        } else {

            String className = getClassNameByAbsolutePath(absolutePath, basePath);
            classNames.add(className);
        }
        return classNames;
    }

    private String getClassNameByAbsolutePath(String absolutePath, String basePath) {

        String fileName = absolutePath
                .substring(absolutePath.indexOf(basePath.replaceAll("/", "\\\\")))
                .replaceAll("\\\\", ".");

        fileName = fileName.substring(0, fileName.indexOf(".class"));
        return fileName;
    }

    public static void main(String[] args) {
        List<String> allClassNames = YrpcBootstrap.getInstance().getAllClassNames("com.ydlclass");
        System.out.println(allClassNames);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public YrpcBootstrap group(String group) {
        this.getConfiguration().setGroup(group);
        return this;
    }
}
