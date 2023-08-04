package com.ydlclass;

import com.ydlclass.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsumerApplication {

    public static void main(String[] args) {

        ReferenceConfig<HelloYrpc> reference = new ReferenceConfig<>();
        reference.setInterface(HelloYrpc.class);

        YrpcBootstrap.getInstance()
                .application("first-yrpc-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .serialize("hessian")
                .compress("gzip")
                .group("primary")
                .reference(reference);

        System.out.println("++------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        HelloYrpc helloYrpc = reference.get();

        while (true) {

            for (int i = 0; i < 50; i++) {
                String sayHi = helloYrpc.sayHi("你好yrpc");
                log.info("sayHi-->{}", sayHi);
            }
        }

    }
}
