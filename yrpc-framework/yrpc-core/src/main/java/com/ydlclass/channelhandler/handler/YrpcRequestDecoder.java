package com.ydlclass.channelhandler.handler;

import com.ydlclass.compress.Compressor;
import com.ydlclass.compress.CompressorFactory;
import com.ydlclass.enumeration.RequestType;
import com.ydlclass.serialize.Serializer;
import com.ydlclass.serialize.SerializerFactory;
import com.ydlclass.transport.message.MessageFormatConstant;
import com.ydlclass.transport.message.RequestPayload;
import com.ydlclass.transport.message.YrpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class YrpcRequestDecoder extends LengthFieldBasedFrameDecoder {
    public YrpcRequestDecoder() {
        super(

                MessageFormatConstant.MAX_FRAME_LENGTH,

                MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH,

                MessageFormatConstant.FULL_FIELD_LENGTH,

                -(MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH + MessageFormatConstant.FULL_FIELD_LENGTH),
                0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

        Thread.sleep(new Random().nextInt(50));

        Object decode = super.decode(ctx, in);
        if (decode instanceof ByteBuf byteBuf) {
            return decodeFrame(byteBuf);
        }
        return null;
    }

    private Object decodeFrame(ByteBuf byteBuf) {

        byte[] magic = new byte[MessageFormatConstant.MAGIC.length];
        byteBuf.readBytes(magic);

        for (int i = 0; i < magic.length; i++) {
            if (magic[i] != MessageFormatConstant.MAGIC[i]) {
                throw new RuntimeException("The request obtained is not legitimate。");
            }
        }

        byte version = byteBuf.readByte();
        if (version > MessageFormatConstant.VERSION) {
            throw new RuntimeException("The request version is not supported.");
        }

        short headLength = byteBuf.readShort();

        int fullLength = byteBuf.readInt();

        byte requestType = byteBuf.readByte();

        byte serializeType = byteBuf.readByte();

        byte compressType = byteBuf.readByte();

        long requestId = byteBuf.readLong();

        long timeStamp = byteBuf.readLong();

        YrpcRequest yrpcRequest = new YrpcRequest();
        yrpcRequest.setRequestType(requestType);
        yrpcRequest.setCompressType(compressType);
        yrpcRequest.setSerializeType(serializeType);
        yrpcRequest.setRequestId(requestId);
        yrpcRequest.setTimeStamp(timeStamp);

        if (requestType == RequestType.HEART_BEAT.getId()) {
            return yrpcRequest;
        }

        int payloadLength = fullLength - headLength;
        byte[] payload = new byte[payloadLength];
        byteBuf.readBytes(payload);

        if (payload != null && payload.length != 0) {
            Compressor compressor = CompressorFactory.getCompressor(compressType).getImpl();
            payload = compressor.decompress(payload);

            Serializer serializer = SerializerFactory.getSerializer(serializeType).getImpl();
            RequestPayload requestPayload = serializer.deserialize(payload, RequestPayload.class);
            yrpcRequest.setRequestPayload(requestPayload);
        }

        if (log.isDebugEnabled()) {
            log.debug("Request [{}] has completed decoding work on the server.", yrpcRequest.getRequestId());
        }

        return yrpcRequest;
    }
}
