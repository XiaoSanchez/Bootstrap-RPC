package com.ydlclass.serialize;

public interface Serializer {

    byte[] serialize(Object object);

    <T> T deserialize(byte[] bytes, Class<T> clazz);

}
