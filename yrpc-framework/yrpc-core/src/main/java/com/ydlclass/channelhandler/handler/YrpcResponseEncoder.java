package com.ydlclass.channelhandler.handler;

import com.ydlclass.compress.Compressor;
import com.ydlclass.compress.CompressorFactory;
import com.ydlclass.serialize.Serializer;
import com.ydlclass.serialize.SerializerFactory;
import com.ydlclass.transport.message.MessageFormatConstant;
import com.ydlclass.transport.message.YrpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YrpcResponseEncoder extends MessageToByteEncoder<YrpcResponse> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, YrpcResponse yrpcResponse, ByteBuf byteBuf)
            throws Exception {

        byteBuf.writeBytes(MessageFormatConstant.MAGIC);

        byteBuf.writeByte(MessageFormatConstant.VERSION);

        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);

        byteBuf.writerIndex(byteBuf.writerIndex() + MessageFormatConstant.FULL_FIELD_LENGTH);

        byteBuf.writeByte(yrpcResponse.getCode());
        byteBuf.writeByte(yrpcResponse.getSerializeType());
        byteBuf.writeByte(yrpcResponse.getCompressType());

        byteBuf.writeLong(yrpcResponse.getRequestId());
        byteBuf.writeLong(yrpcResponse.getTimeStamp());

        byte[] body = null;
        if (yrpcResponse.getBody() != null) {
            Serializer serializer = SerializerFactory
                    .getSerializer(yrpcResponse.getSerializeType()).getImpl();
            body = serializer.serialize(yrpcResponse.getBody());

            Compressor compressor = CompressorFactory.getCompressor(
                    yrpcResponse.getCompressType()).getImpl();
            body = compressor.compress(body);
        }

        if (body != null) {
            byteBuf.writeBytes(body);
        }
        int bodyLength = body == null ? 0 : body.length;

        int writerIndex = byteBuf.writerIndex();

        byteBuf.writerIndex(MessageFormatConstant.MAGIC.length
                + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH);
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + bodyLength);

        byteBuf.writerIndex(writerIndex);

        if (log.isDebugEnabled()) {
            log.debug("Response [{}] has completed the encoding work on the server.", yrpcResponse.getRequestId());
        }

    }

}
