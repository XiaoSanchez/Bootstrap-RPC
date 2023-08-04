package com.ydlclass.compress;

import com.ydlclass.compress.impl.GzipCompressor;
import com.ydlclass.config.ObjectWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CompressorFactory {

    private final static Map<String, ObjectWrapper<Compressor>> COMPRESSOR_CACHE = new ConcurrentHashMap<>(8);
    private final static Map<Byte, ObjectWrapper<Compressor>> COMPRESSOR_CACHE_CODE = new ConcurrentHashMap<>(8);

    static {
        ObjectWrapper<Compressor> gzip = new ObjectWrapper<>((byte) 1, "gzip", new GzipCompressor());
        COMPRESSOR_CACHE.put("gzip", gzip);
        COMPRESSOR_CACHE_CODE.put((byte) 1, gzip);
    }

    public static ObjectWrapper<Compressor> getCompressor(String compressorType) {
        ObjectWrapper<Compressor> compressorObjectWrapper = COMPRESSOR_CACHE.get(compressorType);
        if (compressorObjectWrapper == null) {
            log.error(
                    "The [{}] compression algorithm that is not configured is not found, and the GZIP algorithm is selected by default.",
                    compressorType);
            return COMPRESSOR_CACHE.get("gzip");
        }
        return compressorObjectWrapper;
    }

    public static ObjectWrapper<Compressor> getCompressor(byte serializeCode) {
        ObjectWrapper<Compressor> compressorObjectWrapper = COMPRESSOR_CACHE.get(serializeCode);
        if ((compressorObjectWrapper == null)) {
            log.error(
                    "The compression algorithm of [{}] that you configured is not found, and the GZIP algorithm is selected by default.",
                    serializeCode);
            return COMPRESSOR_CACHE.get("gzip");
        }
        return compressorObjectWrapper;
    }

    public static void addCompressor(ObjectWrapper<Compressor> compressorObjectWrapper) {
        COMPRESSOR_CACHE.put(compressorObjectWrapper.getName(), compressorObjectWrapper);
        COMPRESSOR_CACHE_CODE.put(compressorObjectWrapper.getCode(), compressorObjectWrapper);
    }
}
