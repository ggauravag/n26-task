package com.n26.repository.impl;

import com.n26.model.Transaction;
import com.n26.repository.NoRecordedTransactionException;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class InMemoryTransactionRepositoryTest {

    private InMemoryTransactionRepository repository;

    @Before
    public void setUp() {
        repository = new InMemoryTransactionRepository();
    }

    @Test
    public void shouldGetTransactionWhenHappenedInLastOneMinute() {
        // having
        final long currentTimeMillis = System.currentTimeMillis();
        final Transaction transaction = new Transaction(15.0, currentTimeMillis);
        final Instant instant = Instant.ofEpochMilli(currentTimeMillis).plusMillis(30000);

        // when
        repository.save(transaction);

        // then
        assertThat(repository.getCount(instant), is(1L));
    }

    @Test
    public void shouldNotGetTransactionsWhenLastTransactionWasMoreThanMinuteAgo() {
        // having
        final long currentTimeMillis = System.currentTimeMillis();
        final Transaction transaction = new Transaction(15.0, currentTimeMillis);
        final Instant instant = Instant.ofEpochMilli(currentTimeMillis).plusMillis(61000);

        // when
        repository.save(transaction);

        // then
        assertThat(repository.getCount(instant), is(0L));
    }

    @Test
    public void shouldGetCorrectStatisticsWhenConcurrentTransactions() throws InterruptedException, NoRecordedTransactionException {
        // having
        final long currentTimeMillis = System.currentTimeMillis();
        final ExecutorService executorService = Executors.newCachedThreadPool();
        final Instant instant = Instant.ofEpochMilli(currentTimeMillis).plusMillis(60000);

        final List<Callable<Double>> multipleRequests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            multipleRequests.add(transactionWithTime(currentTimeMillis));
        }

        // when
        final List<Future<Double>> futures = executorService.invokeAll(multipleRequests);
        executorService.shutdown();

        // then
        assertThat(repository.getCount(instant), is(5L));
        assertThat(repository.getSum(instant), closeTo(futures.stream().mapToDouble(this::fetchValue).sum(), 0.001));
        assertThat(repository.getMaximum(instant), closeTo(futures.stream().mapToDouble(this::fetchValue).max().getAsDouble(), 0.001));
        assertThat(repository.getMinimum(instant), closeTo(futures.stream().mapToDouble(this::fetchValue).min().getAsDouble(), 0.001));
        assertThat(repository.getAverage(instant), closeTo(futures.stream().mapToDouble(this::fetchValue).average().getAsDouble(), 0.001));
    }

    @Test
    public void shouldGetCorrectStatisticsWhenStaleAndValidTransactions() throws InterruptedException, NoRecordedTransactionException {
        // having
        final long currentTimeMillis = System.currentTimeMillis();
        final ExecutorService executorService = Executors.newCachedThreadPool();
        final Instant instant = Instant.ofEpochMilli(currentTimeMillis).plusMillis(60000);

        final List<Callable<Double>> staleTransactions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            staleTransactions.add(transactionWithTime(currentTimeMillis - 20000));
        }

        final List<Callable<Double>> validTransactions = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            validTransactions.add(transactionWithTime(currentTimeMillis));
        }

        // when
        final List<Future<Double>> futures = executorService.invokeAll(validTransactions);
        executorService.invokeAll(staleTransactions);
        executorService.shutdown();

        // then
        assertThat(repository.getCount(instant), is(2L));
        assertThat(repository.getSum(instant), closeTo(futures.stream().mapToDouble(this::fetchValue).sum(), 0.001));
        assertThat(repository.getMaximum(instant), closeTo(futures.stream().mapToDouble(this::fetchValue).max().getAsDouble(), 0.001));
        assertThat(repository.getMinimum(instant), closeTo(futures.stream().mapToDouble(this::fetchValue).min().getAsDouble(), 0.001));
        assertThat(repository.getAverage(instant), closeTo(futures.stream().mapToDouble(this::fetchValue).average().getAsDouble(), 0.001));
    }

    @Test
    public void shouldRemoveStaleTransactions() throws InterruptedException {
        // having
        final long currentTimeMillis = System.currentTimeMillis();
        final Instant instant = Instant.ofEpochMilli(currentTimeMillis).plusMillis(61000);

        final ExecutorService executorService = Executors.newCachedThreadPool();
        final List<Callable<Double>> multipleRequests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            multipleRequests.add(transactionWithTime(currentTimeMillis));
        }
        executorService.invokeAll(multipleRequests);

        // when
        final long removedTransactions = repository.removeStaleTransactions(instant);

        // then
        assertThat(removedTransactions, is(5L));
    }

    @Test
    public void shouldGetZeroSumWhenNoTransactions() {
        final Instant current = Instant.now();

        final Double actualSum = repository.getSum(current);

        assertThat(actualSum, is(0.0));
    }

    @Test(expected = NoRecordedTransactionException.class)
    public void shouldThrowWhileGettingAverageWhenNoTransactions() throws NoRecordedTransactionException {
        final Instant current = Instant.now();

        repository.getAverage(current);
    }

    @Test(expected = NoRecordedTransactionException.class)
    public void shouldThrowWhileGettingMaximumWhenNoTransactions() throws NoRecordedTransactionException {
        final Instant current = Instant.now();

        repository.getMaximum(current);
    }

    @Test(expected = NoRecordedTransactionException.class)
    public void shouldThrowWhileGettingMinimumWhenNoTransactions() throws NoRecordedTransactionException {
        final Instant current = Instant.now();

        repository.getMinimum(current);
    }

    @Test
    public void shouldGetZeroCountWhenNoTransactions() {
        final Instant current = Instant.now();

        assertThat(repository.getCount(current), is(0L));
    }

    private <T> T fetchValue(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Callable<Double> transactionWithTime(final long timeInMillis) {
        return () -> {
            final double transactionAmount = 1000 * Math.random();
            final Transaction transaction = new Transaction(transactionAmount, timeInMillis);
            repository.save(transaction);
            return transactionAmount;
        };
    }
}