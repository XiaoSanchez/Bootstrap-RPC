package com.ydlclass.transport.message;

import lombok.*;

import java.io.Serializable;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestPayload implements Serializable {

    private String interfaceName;

    private String methodName;

    private Class<?>[] parametersType;
    private Object[] parametersValue;

    private Class<?> returnType;

}
