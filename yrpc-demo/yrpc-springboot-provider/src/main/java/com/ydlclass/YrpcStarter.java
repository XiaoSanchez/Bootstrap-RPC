package com.ydlclass;

import com.ydlclass.discovery.RegistryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class YrpcStarter implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        Thread.sleep(5000);
        log.info("yrpc Starting...");
        YrpcBootstrap.getInstance()
                .application("first-yrpc-provider")

                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .serialize("jdk")
                .scan("com.ydlclass.impl")

                .start();
    }
}
