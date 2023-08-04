package com.ydlclass.channelhandler.handler;

import com.ydlclass.compress.Compressor;
import com.ydlclass.compress.CompressorFactory;
import com.ydlclass.serialize.Serializer;
import com.ydlclass.serialize.SerializerFactory;
import com.ydlclass.transport.message.MessageFormatConstant;
import com.ydlclass.transport.message.YrpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YrpcRequestEncoder extends MessageToByteEncoder<YrpcRequest> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, YrpcRequest yrpcRequest, ByteBuf byteBuf)
            throws Exception {

        byteBuf.writeBytes(MessageFormatConstant.MAGIC);

        byteBuf.writeByte(MessageFormatConstant.VERSION);

        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);

        byteBuf.writerIndex(byteBuf.writerIndex() + MessageFormatConstant.FULL_FIELD_LENGTH);

        byteBuf.writeByte(yrpcRequest.getRequestType());
        byteBuf.writeByte(yrpcRequest.getSerializeType());
        byteBuf.writeByte(yrpcRequest.getCompressType());

        byteBuf.writeLong(yrpcRequest.getRequestId());
        byteBuf.writeLong(yrpcRequest.getTimeStamp());

        byte[] body = null;
        if (yrpcRequest.getRequestPayload() != null) {
            Serializer serializer = SerializerFactory.getSerializer(yrpcRequest.getSerializeType()).getImpl();
            body = serializer.serialize(yrpcRequest.getRequestPayload());

            Compressor compressor = CompressorFactory.getCompressor(yrpcRequest.getCompressType()).getImpl();
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
            log.debug("Request [{}] has completed the encoding of the message.", yrpcRequest.getRequestId());
        }

    }

}
