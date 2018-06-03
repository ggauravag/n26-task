package com.n26.repository;

import com.n26.model.Transaction;

import java.time.Instant;

/**
 * Representation of a component which stores transactions and/or related aggregated statistics
 */
public interface TransactionRepository {

    /**
     * Saves the transaction related data in O(1) space-complexity
     */
    void save(Transaction transaction);

    /**
     * Gets the aggregated sum for the transactions happened in last {@link #getTransactionValidityInMilliSeconds} in
     * O(1) time-complexity
     */
    Double getSum(Instant currentTime);

    /**
     * Gets the average amount for the transactions happened in last {@link #getTransactionValidityInMilliSeconds} in
     * O(1) time-complexity
     */
    Double getAverage(Instant currentTime) throws NoRecordedTransactionException;

    /**
     * Gets the minimum amount for the transactions happened in last {@link #getTransactionValidityInMilliSeconds} in
     * O(1) time-complexity
     */
    Double getMinimum(Instant currentTime) throws NoRecordedTransactionException;

    /**
     * Gets the maximum amount for the transactions happened in last {@link #getTransactionValidityInMilliSeconds} in
     * O(1) time-complexity
     */
    Double getMaximum(Instant currentTime) throws NoRecordedTransactionException;

    /**
     * Gets the total count for the transactions happened in last {@link #getTransactionValidityInMilliSeconds} in O(1)
     * time-complexity
     */
    Long getCount(Instant currentTime);

    /**
     * Maintenance operation which removes all stale transactions which are older than {@link
     * #getTransactionValidityInMilliSeconds} sec. and returns the count
     */
    long removeStaleTransactions(Instant currentTime);

    /**
     * Returns the validity of a transaction in milli seconds
     */
    long getTransactionValidityInMilliSeconds();
}
