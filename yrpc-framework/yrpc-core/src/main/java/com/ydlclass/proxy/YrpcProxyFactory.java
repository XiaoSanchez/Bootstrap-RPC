package com.ydlclass.proxy;

import com.ydlclass.ReferenceConfig;
import com.ydlclass.YrpcBootstrap;
import com.ydlclass.discovery.RegistryConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class YrpcProxyFactory {

    private static Map<Class<?>, Object> cache = new ConcurrentHashMap<>(32);

    public static <T> T getProxy(Class<T> clazz) {

        Object bean = cache.get(clazz);
        if (bean != null) {
            return (T) bean;
        }

        ReferenceConfig<T> reference = new ReferenceConfig<>();
        reference.setInterface(clazz);

        YrpcBootstrap.getInstance()
                .application("first-yrpc-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .serialize("hessian")
                .compress("gzip")
                .group("primary")
                .reference(reference);
        T t = reference.get();
        cache.put(clazz, t);
        return t;
    }
}
