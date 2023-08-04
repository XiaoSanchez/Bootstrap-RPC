package com.ydlclass.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class YrpcRequest {

    private long requestId;

    private byte requestType;
    private byte compressType;
    private byte serializeType;

    private long timeStamp;

    private RequestPayload requestPayload;

}
