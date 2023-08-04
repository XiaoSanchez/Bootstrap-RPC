package com.ydlclass.core;

public class YrpcShutdownHook extends Thread {

    @Override
    public void run() {

        ShutDownHolder.BAFFLE.set(true);

        long start = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (ShutDownHolder.REQUEST_COUNTER.sum() == 0L
                    || System.currentTimeMillis() - start > 10000) {
                break;
            }
        }

    }
}
