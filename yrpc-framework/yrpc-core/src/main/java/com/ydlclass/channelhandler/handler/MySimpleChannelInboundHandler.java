package com.ydlclass.channelhandler.handler;

import com.ydlclass.YrpcBootstrap;
import com.ydlclass.enumeration.RespCode;
import com.ydlclass.exceptions.ResponseException;
import com.ydlclass.loadbalancer.LoadBalancer;
import com.ydlclass.protection.CircuitBreaker;
import com.ydlclass.transport.message.YrpcRequest;
import com.ydlclass.transport.message.YrpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<YrpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, YrpcResponse yrpcResponse)
            throws Exception {

        CompletableFuture<Object> completableFuture = YrpcBootstrap.PENDING_REQUEST.get(yrpcResponse.getRequestId());

        SocketAddress socketAddress = channelHandlerContext.channel().remoteAddress();
        Map<SocketAddress, CircuitBreaker> everyIpCircuitBreaker = YrpcBootstrap.getInstance()
                .getConfiguration().getEveryIpCircuitBreaker();
        CircuitBreaker circuitBreaker = everyIpCircuitBreaker.get(socketAddress);

        byte code = yrpcResponse.getCode();
        if (code == RespCode.FAIL.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("The current ID is a request [{}], returning the error result, the response code [{}].",
                    yrpcResponse.getRequestId(), yrpcResponse.getCode());
            throw new ResponseException(code, RespCode.FAIL.getDesc());

        } else if (code == RespCode.RATE_LIMIT.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("The current ID is a request with [{}], which is limited, the response code [{}].",
                    yrpcResponse.getRequestId(), yrpcResponse.getCode());
            throw new ResponseException(code, RespCode.RATE_LIMIT.getDesc());

        } else if (code == RespCode.RESOURCE_NOT_FOUND.getCode()) {
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("The current ID is a request [{}], and the target resource is not found, the response code [{}].",
                    yrpcResponse.getRequestId(), yrpcResponse.getCode());
            throw new ResponseException(code, RespCode.RESOURCE_NOT_FOUND.getDesc());

        } else if (code == RespCode.SUCCESS.getCode()) {

            Object returnValue = yrpcResponse.getBody();
            completableFuture.complete(returnValue);
            if (log.isDebugEnabled()) {
                log.debug("To find the CompletableFuture with the number of [{}] to handle the response result.",
                        yrpcResponse.getRequestId());
            }
        } else if (code == RespCode.SUCCESS_HEART_BEAT.getCode()) {
            completableFuture.complete(null);
            if (log.isDebugEnabled()) {
                log.debug(
                        "To find the CompletableFuture with the number of [{}], process heartbeat detection, and handle the response results.",
                        yrpcResponse.getRequestId());
            }
        } else if (code == RespCode.BECOLSING.getCode()) {
            completableFuture.complete(null);
            if (log.isDebugEnabled()) {
                log.debug(
                        "The current ID is a request [{}]. The access is rejected. The target server is being closed. The response code [{}].",
                        yrpcResponse.getRequestId(), yrpcResponse.getCode());
            }

            YrpcBootstrap.CHANNEL_CACHE.remove(socketAddress);

            LoadBalancer loadBalancer = YrpcBootstrap.getInstance()
                    .getConfiguration().getLoadBalancer();

            YrpcRequest yrpcRequest = YrpcBootstrap.REQUEST_THREAD_LOCAL.get();
            loadBalancer.reLoadBalance(yrpcRequest.getRequestPayload().getInterfaceName(),
                    YrpcBootstrap.CHANNEL_CACHE.keySet().stream().toList());

            throw new ResponseException(code, RespCode.BECOLSING.getDesc());
        }
    }
}
