package com.ydlclass.protection;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreaker {

    private volatile boolean isOpen = false;

    private AtomicInteger requestCount = new AtomicInteger(0);

    private AtomicInteger errorRequest = new AtomicInteger(0);

    private int maxErrorRequest;
    private float maxErrorRate;

    public CircuitBreaker(int maxErrorRequest, float maxErrorRate) {
        this.maxErrorRequest = maxErrorRequest;
        this.maxErrorRate = maxErrorRate;
    }

    public boolean isBreak() {

        if (isOpen) {
            return true;
        }

        if (errorRequest.get() > maxErrorRequest) {
            this.isOpen = true;
            return true;
        }

        if (errorRequest.get() > 0 && requestCount.get() > 0 &&
                errorRequest.get() / (float) requestCount.get() > maxErrorRate) {
            this.isOpen = true;
            return true;
        }

        return false;
    }

    public void recordRequest() {
        this.requestCount.getAndIncrement();
    }

    public void recordErrorRequest() {
        this.errorRequest.getAndIncrement();
    }

    public void reset() {
        this.isOpen = false;
        this.requestCount.set(0);
        this.errorRequest.set(0);
    }

    public static void main(String[] args) {

        CircuitBreaker circuitBreaker = new CircuitBreaker(3, 1.1F);

        new Thread(() -> {
            for (int i = 0; i < 1000; i++) {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                circuitBreaker.recordRequest();
                int num = new Random().nextInt(100);
                if (num > 70) {
                    circuitBreaker.recordErrorRequest();
                }

                boolean aBreak = circuitBreaker.isBreak();

                String result = aBreak ? "The circuit breaker blocks the request"
                        : "The circuit breaker releases the request";

                System.out.println(result);

            }
        }).start();

        new Thread(() -> {
            for (;;) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("-----------------------------------------");
                circuitBreaker.reset();
            }
        }).start();

        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
