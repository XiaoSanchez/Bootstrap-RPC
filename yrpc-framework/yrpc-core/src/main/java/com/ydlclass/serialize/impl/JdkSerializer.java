package com.ydlclass.serialize.impl;

import com.ydlclass.exceptions.SerializeException;
import com.ydlclass.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class JdkSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        if (object == null) {
            return null;
        }

        try (

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream outputStream = new ObjectOutputStream(baos);) {
            outputStream.writeObject(object);

            byte[] result = baos.toByteArray();
            if (log.isDebugEnabled()) {
                log.debug(
                        "Object [{}] has completed the serialization operation, the number of bytes that serialize are [{}]",
                        object, result.length);
            }
            return result;
        } catch (IOException e) {
            log.error("Serialized objects [{}] Putting birth abnormalities.", object);
            throw new SerializeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null) {
            return null;
        }
        try (

                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream objectInputStream = new ObjectInputStream(bais);) {
            Object object = objectInputStream.readObject();
            if (log.isDebugEnabled()) {
                log.debug("Category [{}] has completed the dee -order operation.", clazz);
            }
            return (T) object;
        } catch (IOException | ClassNotFoundException e) {
            log.error("Reverse sequentialization objects [{}] are abnormal when they are.", clazz);
            throw new SerializeException(e);
        }
    }
}
