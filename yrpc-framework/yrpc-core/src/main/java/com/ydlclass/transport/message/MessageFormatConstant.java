package com.ydlclass.transport.message;

public class MessageFormatConstant {

    public final static byte[] MAGIC = "yrpc".getBytes();
    public final static byte VERSION = 1;

    public final static short HEADER_LENGTH = (byte) (MAGIC.length + 1 + 2 + 4 + 1 + 1 + 1 + 8 + 8);

    public static final int HEADER_FIELD_LENGTH = 2;

    public final static int MAX_FRAME_LENGTH = 1024 * 1024;

    public static final int VERSION_LENGTH = 1;

    public static final int FULL_FIELD_LENGTH = 4;
}
