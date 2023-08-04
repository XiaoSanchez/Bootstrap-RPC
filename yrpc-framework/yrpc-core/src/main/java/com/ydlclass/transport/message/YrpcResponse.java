package com.ydlclass.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class YrpcResponse {

    private long requestId;

    private byte compressType;
    private byte serializeType;

    private long timeStamp;

    private byte code;

    private Object body;
}
