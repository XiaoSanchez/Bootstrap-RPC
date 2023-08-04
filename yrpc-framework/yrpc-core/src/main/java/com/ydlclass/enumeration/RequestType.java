package com.ydlclass.enumeration;

public enum RequestType {

    REQUEST((byte) 1, "Ordinary request"), HEART_BEAT((byte) 2, "Heartbeat test request");

    private byte id;
    private String type;

    RequestType(byte id, String type) {
        this.id = id;
        this.type = type;
    }

    public byte getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
