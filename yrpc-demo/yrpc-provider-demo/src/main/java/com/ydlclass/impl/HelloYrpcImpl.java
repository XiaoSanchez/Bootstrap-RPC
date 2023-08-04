package com.ydlclass.impl;

import com.ydlclass.HelloYrpc;
import com.ydlclass.annotation.YrpcApi;

@YrpcApi(group = "primary")
public class HelloYrpcImpl implements HelloYrpc {
    @Override
    public String sayHi(String msg) {
        return "hi consumer:" + msg;
    }
}
