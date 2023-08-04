package com.ydlclass;

import com.ydlclass.discovery.RegistryConfig;

public class ProviderApplication {

    public static void main(String[] args) {

        YrpcBootstrap.getInstance()
                .application("first-yrpc-provider")

                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .serialize("jdk")

                .scan("com.ydlclass")

                .start();
    }

}
