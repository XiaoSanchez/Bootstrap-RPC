package com.ydlclass.enumeration;

public enum RespCode {

    SUCCESS((byte) 20, "success"),
    SUCCESS_HEART_BEAT((byte) 21, "Heartbeat detection successfully returns"),
    RATE_LIMIT((byte) 31, "The service is restricted"),
    RESOURCE_NOT_FOUND((byte) 44, "The request resource does not exist"),
    FAIL((byte) 50, "The call method is abnormal"),
    BECOLSING((byte) 51, "The call method is abnormal");

    private byte code;
    private String desc;

    RespCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public byte getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
