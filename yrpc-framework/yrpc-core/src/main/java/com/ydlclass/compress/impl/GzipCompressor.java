package com.ydlclass.compress.impl;

import com.ydlclass.compress.Compressor;
import com.ydlclass.exceptions.CompressException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class GzipCompressor implements Compressor {
    @Override
    public byte[] compress(byte[] bytes) {

        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);) {
            gzipOutputStream.write(bytes);
            gzipOutputStream.finish();
            byte[] result = baos.toByteArray();
            if (log.isDebugEnabled()) {
                log.debug("The compression length of the byte array was compressed from [{}] to [{}].", bytes.length,
                        result.length);
            }
            return result;
        } catch (IOException e) {
            log.error("Anomalial occurs when the byte array is compressed", e);
            throw new CompressException(e);
        }

    }

    @Override
    public byte[] decompress(byte[] bytes) {
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                GZIPInputStream gzipInputStream = new GZIPInputStream(bais);) {
            byte[] result = gzipInputStream.readAllBytes();
            if (log.isDebugEnabled()) {
                log.debug("To understand the compression length from [{}] to [{}] from [{}].", bytes.length,
                        result.length);
            }
            return result;
        } catch (IOException e) {
            log.error("Anomalial occurs when the byte array is compressed", e);
            throw new CompressException(e);
        }
    }
}
