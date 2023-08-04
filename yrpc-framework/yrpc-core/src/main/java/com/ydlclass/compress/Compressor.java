package com.ydlclass.compress;

public interface Compressor {
    byte[] compress(byte[] bytes);

    byte[] decompress(byte[] bytes);
}
