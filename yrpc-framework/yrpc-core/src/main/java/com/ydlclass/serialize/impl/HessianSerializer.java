package com.ydlclass.serialize.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.ydlclass.exceptions.SerializeException;
import com.ydlclass.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class HessianSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        if (object == null) {
            return null;
        }
        try (

                ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
            Hessian2Output hessian2Output = new Hessian2Output(baos);
            hessian2Output.writeObject(object);
            hessian2Output.flush();
            byte[] result = baos.toByteArray();
            if (log.isDebugEnabled()) {
                log.debug(
                        "Object [{}] has completed the serialization operation, the number of bytes that serialize are [{}]",
                        object, result.length);
            }
            return result;
        } catch (IOException e) {
            log.error("Use hessian to perform serialized objects [{}].", object);
            throw new SerializeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null) {
            return null;
        }
        try (

                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);) {

            Hessian2Input hessian2Input = new Hessian2Input(bais);
            T t = (T) hessian2Input.readObject();
            if (log.isDebugEnabled()) {
                log.debug("Category [{}] has been used with hessian to complete the back -sequentialization operation.",
                        clazz);
            }
            return t;
        } catch (IOException e) {
            log.error("Anomalial occurs occur with hessian for a back -sequentialized object [{}].", clazz);
            throw new SerializeException(e);
        }
    }
}
