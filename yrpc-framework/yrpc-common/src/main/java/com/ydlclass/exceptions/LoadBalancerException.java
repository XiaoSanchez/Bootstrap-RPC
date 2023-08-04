package com.ydlclass.exceptions;

public class LoadBalancerException extends RuntimeException {

    public LoadBalancerException(String message) {
        super(message);
    }

    public LoadBalancerException() {
    }
}
