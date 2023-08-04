package com.ydlclass.netty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MyCompletableFuture {

    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {

        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();

        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            int i = 8;

            completableFuture.complete(i);

        }).start();

        Integer integer = completableFuture.get(1, TimeUnit.SECONDS);
        System.out.println(integer);

    }

}
