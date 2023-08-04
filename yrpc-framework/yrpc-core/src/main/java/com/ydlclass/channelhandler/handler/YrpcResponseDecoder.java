package com.ydlclass.channelhandler.handler;

import com.ydlclass.compress.Compressor;
import com.ydlclass.compress.CompressorFactory;
import com.ydlclass.serialize.Serializer;
import com.ydlclass.serialize.SerializerFactory;
import com.ydlclass.transport.message.MessageFormatConstant;
import com.ydlclass.transport.message.YrpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YrpcResponseDecoder extends LengthFieldBasedFrameDecoder {
    public YrpcResponseDecoder() {
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

        byte responseCode = byteBuf.readByte();

        byte serializeType = byteBuf.readByte();

        byte compressType = byteBuf.readByte();

        long requestId = byteBuf.readLong();

        long timeStamp = byteBuf.readLong();

        YrpcResponse yrpcResponse = new YrpcResponse();
        yrpcResponse.setCode(responseCode);
        yrpcResponse.setCompressType(compressType);
        yrpcResponse.setSerializeType(serializeType);
        yrpcResponse.setRequestId(requestId);
        yrpcResponse.setTimeStamp(timeStamp);

        int bodyLength = fullLength - headLength;
        byte[] payload = new byte[bodyLength];
        byteBuf.readBytes(payload);

        if (payload.length > 0) {

            Compressor compressor = CompressorFactory.getCompressor(compressType).getImpl();
            payload = compressor.decompress(payload);

            Serializer serializer = SerializerFactory
                    .getSerializer(yrpcResponse.getSerializeType()).getImpl();
            Object body = serializer.deserialize(payload, Object.class);
            yrpcResponse.setBody(body);
        }

        if (log.isDebugEnabled()) {
            log.debug("Response [{}] has completed the decoding work on the call.", yrpcResponse.getRequestId());
        }

        return yrpcResponse;
    }

    public static void main(String[] args) {
        int i = ~(-1 << 3);
        System.out.println(i);
    }
}
