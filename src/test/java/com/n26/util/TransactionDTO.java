package com.n26.util;

public class TransactionDTO {
    public Double amount;

    public Long timestamp;

    public TransactionDTO(Double amount, Long timestamp) {
        this.amount = amount;
        this.timestamp = timestamp;
    }
}
