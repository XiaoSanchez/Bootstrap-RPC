package com.ydlclass.protection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TokenBuketRateLimiter implements RateLimiter {

    private int tokens;

    private final int capacity;

    private final int rate;

    private Long lastTokenTime;

    public TokenBuketRateLimiter(int capacity, int rate) {
        this.capacity = capacity;
        this.rate = rate;
        lastTokenTime = System.currentTimeMillis();
        tokens = capacity;
    }

    public synchronized boolean allowRequest() {

        Long currentTime = System.currentTimeMillis();
        long timeInterval = currentTime - lastTokenTime;

        if (timeInterval >= 1000 / rate) {
            int needAddTokens = (int) (timeInterval * rate / 1000);
            System.out.println("needAddTokens = " + needAddTokens);

            tokens = Math.min(capacity, tokens + needAddTokens);
            System.out.println("tokens = " + tokens);

            this.lastTokenTime = System.currentTimeMillis();
        }

        if (tokens > 0) {
            tokens--;
            System.out.println("Request being released---------------");
            return true;
        } else {
            System.out.println("Request being intercepted---------------");
            return false;
        }

    }

    public static void main(String[] args) {
        TokenBuketRateLimiter rateLimiter = new TokenBuketRateLimiter(10, 10);
        for (int i = 0; i < 1000; i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            boolean allowRequest = rateLimiter.allowRequest();
            System.out.println("allowRequest = " + allowRequest);
        }
    }
}
