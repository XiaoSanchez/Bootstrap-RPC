package com.ydlclass;

import com.ydlclass.annotation.YrpcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @YrpcService
    private HelloYrpc helloYrpc;

    @GetMapping("hello")
    public String hello() {
        return helloYrpc.sayHi("provider");
    }

}
