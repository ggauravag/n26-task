package com.n26.service;

public class StaleTransactionException extends Exception {
    public StaleTransactionException(String message) {
        super(message);
    }
}
