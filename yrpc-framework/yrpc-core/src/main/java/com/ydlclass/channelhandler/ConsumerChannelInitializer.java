package com.ydlclass.channelhandler;

import com.ydlclass.channelhandler.handler.MySimpleChannelInboundHandler;
import com.ydlclass.channelhandler.handler.YrpcRequestEncoder;
import com.ydlclass.channelhandler.handler.YrpcResponseDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()

                .addLast(new LoggingHandler(LogLevel.DEBUG))

                .addLast(new YrpcRequestEncoder())

                .addLast(new YrpcResponseDecoder())

                .addLast(new MySimpleChannelInboundHandler());

    }
}
