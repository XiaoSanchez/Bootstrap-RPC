package com.ydlclass.channelhandler.handler;

import com.ydlclass.ServiceConfig;
import com.ydlclass.YrpcBootstrap;
import com.ydlclass.core.ShutDownHolder;
import com.ydlclass.enumeration.RequestType;
import com.ydlclass.enumeration.RespCode;
import com.ydlclass.protection.RateLimiter;
import com.ydlclass.protection.TokenBuketRateLimiter;
import com.ydlclass.transport.message.RequestPayload;
import com.ydlclass.transport.message.YrpcRequest;
import com.ydlclass.transport.message.YrpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Map;

@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<YrpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, YrpcRequest yrpcRequest) throws Exception {

        YrpcResponse yrpcResponse = new YrpcResponse();
        yrpcResponse.setRequestId(yrpcRequest.getRequestId());
        yrpcResponse.setCompressType(yrpcRequest.getCompressType());
        yrpcResponse.setSerializeType(yrpcRequest.getSerializeType());

        Channel channel = channelHandlerContext.channel();

        if (ShutDownHolder.BAFFLE.get()) {
            yrpcResponse.setCode(RespCode.BECOLSING.getCode());
            channel.writeAndFlush(yrpcResponse);
            return;
        }

        ShutDownHolder.REQUEST_COUNTER.increment();

        SocketAddress socketAddress = channel.remoteAddress();
        Map<SocketAddress, RateLimiter> everyIpRateLimiter = YrpcBootstrap.getInstance().getConfiguration()
                .getEveryIpRateLimiter();

        RateLimiter rateLimiter = everyIpRateLimiter.get(socketAddress);
        if (rateLimiter == null) {
            rateLimiter = new TokenBuketRateLimiter(10, 10);
            everyIpRateLimiter.put(socketAddress, rateLimiter);
        }
        boolean allowRequest = rateLimiter.allowRequest();

        if (!allowRequest) {

            yrpcResponse.setCode(RespCode.RATE_LIMIT.getCode());

        } else if (yrpcRequest.getRequestType() == RequestType.HEART_BEAT.getId()) {

            yrpcResponse.setCode(RespCode.SUCCESS_HEART_BEAT.getCode());

        } else {

            RequestPayload requestPayload = yrpcRequest.getRequestPayload();

            try {
                Object result = callTargetMethod(requestPayload);
                if (log.isDebugEnabled()) {
                    log.debug("Request [{}] has been called on the server.", yrpcRequest.getRequestId());
                }

                yrpcResponse.setCode(RespCode.SUCCESS.getCode());
                yrpcResponse.setBody(result);
            } catch (Exception e) {
                log.error("The request number [{}] is abnormal during the call.", yrpcRequest.getRequestId(), e);
                yrpcResponse.setCode(RespCode.FAIL.getCode());
            }
        }

        channel.writeAndFlush(yrpcResponse);

        ShutDownHolder.REQUEST_COUNTER.decrement();
    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String interfaceName = requestPayload.getInterfaceName();
        String methodName = requestPayload.getMethodName();
        Class<?>[] parametersType = requestPayload.getParametersType();
        Object[] parametersValue = requestPayload.getParametersValue();

        ServiceConfig<?> serviceConfig = YrpcBootstrap.SERVERS_LIST.get(interfaceName);
        Object refImpl = serviceConfig.getRef();

        Object returnValue;
        try {
            Class<?> aClass = refImpl.getClass();
            Method method = aClass.getMethod(methodName, parametersType);
            returnValue = method.invoke(refImpl, parametersValue);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            log.error("A method of calling the [{}] method [{}] occurred.", interfaceName, methodName, e);
            throw new RuntimeException(e);
        }
        return returnValue;
    }
}
