package com.ydlclass.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public class ShutDownHolder {

    public static AtomicBoolean BAFFLE = new AtomicBoolean(false);

    public static LongAdder REQUEST_COUNTER = new LongAdder();
}
