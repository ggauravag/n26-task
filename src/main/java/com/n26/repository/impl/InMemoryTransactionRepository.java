package com.n26.repository.impl;

import com.n26.model.Transaction;
import com.n26.repository.NoRecordedTransactionException;
import com.n26.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class InMemoryTransactionRepository implements TransactionRepository {

    private final ConcurrentSkipListMap<Long, AggregatedStats> statisticsByTime = new ConcurrentSkipListMap<>();

    private static final int SECOND_LEVEL_PRECISION = 1000;
    private static final long DEFAULT_TRANSACTION_VALIDITY_IN_MS = 60000;

    private final int precisionInMs;
    private final long transactionValidityInMilliSeconds;

    public InMemoryTransactionRepository() {
        this(SECOND_LEVEL_PRECISION, DEFAULT_TRANSACTION_VALIDITY_IN_MS);
    }

    public InMemoryTransactionRepository(int precisionInMs, long transactionValidityInMilliSeconds) {
        this.precisionInMs = precisionInMs;
        this.transactionValidityInMilliSeconds = transactionValidityInMilliSeconds;
    }

    @Override
    public void save(Transaction transaction) {
        final Long ceilingSecond = roundToCeilingSecond(transaction.getTimestamp());
        final AggregatedStats existingValue = statisticsByTime.putIfAbsent(ceilingSecond,
                AggregatedStats.of(transaction.getAmount()));
        if (existingValue != null) {
            existingValue.aggregateIncrementally(transaction.getAmount());
        }
    }

    @Override
    public Double getAverage(Instant currentTime) throws NoRecordedTransactionException {
        final Long totalCount = getCount(currentTime);
        if (totalCount == 0) {
            throw new NoRecordedTransactionException();
        }
        return getSum(currentTime) / totalCount;
    }

    @Override
    public Double getSum(Instant currentTime) {
        return getRequiredStats(currentTime)
                .mapToDouble(AggregatedStats::getSum)
                .sum();
    }

    @Override
    public Double getMinimum(Instant currentTime) throws NoRecordedTransactionException {
        return getRequiredStats(currentTime)
                .mapToDouble(AggregatedStats::getMin)
                .min().orElseThrow(NoRecordedTransactionException::new);
    }

    @Override
    public Double getMaximum(Instant currentTime) throws NoRecordedTransactionException {
        return getRequiredStats(currentTime)
                .mapToDouble(AggregatedStats::getMax)
                .max().orElseThrow(NoRecordedTransactionException::new);
    }

    @Override
    public Long getCount(Instant currentTime) {
        return getRequiredStats(currentTime)
                .mapToLong(AggregatedStats::getCount)
                .sum();
    }

    @Override
    public long removeStaleTransactions(Instant currentTime) {
        final Long afterThis = roundToCeilingSecond(currentTime.toEpochMilli() - transactionValidityInMilliSeconds);
        final Long untilThis = statisticsByTime.floorKey(afterThis);
        final Long oldestKey = statisticsByTime.firstKey();

        if (untilThis == null) {
            return 0;
        }

        final Set<Long> keysToRemove = new HashSet<>();
        final AtomicLong totalCount = new AtomicLong();
        statisticsByTime.subMap(oldestKey, true, untilThis, true)
                .forEach((key, value) -> {
                    totalCount.addAndGet(value.getCount());
                    keysToRemove.add(key);
                });

        statisticsByTime.keySet().removeAll(keysToRemove);
        return totalCount.get();
    }

    @Override
    public long getTransactionValidityInMilliSeconds() {
        return transactionValidityInMilliSeconds;
    }

    private static class AggregatedStats {

        private Double sum = 0.0;
        private Double max = null;
        private Double min = null;
        private Integer count = 0;

        static AggregatedStats of(Double amount) {
            final AggregatedStats aggregatedStats = new AggregatedStats();
            aggregatedStats.aggregateIncrementally(amount);
            return aggregatedStats;
        }

        synchronized void aggregateIncrementally(Double amount) {
            sum += amount;
            max = (max != null && max > amount) ? max : amount;
            min = (min != null && min < amount) ? min : amount;
            count++;
        }

        Double getSum() {
            return sum;
        }

        Double getMax() {
            return max;
        }

        Double getMin() {
            return min;
        }

        Integer getCount() {
            return count;
        }

    }

    private Long roundToCeilingSecond(Long timeInMs) {
        return (long) Math.floor(timeInMs.doubleValue() / precisionInMs);
    }

    private Stream<AggregatedStats> getRequiredStats(Instant currentTime) {
        final Long untilNow = roundToCeilingSecond(currentTime.toEpochMilli());
        final Long afterThis = roundToCeilingSecond(currentTime.toEpochMilli() - transactionValidityInMilliSeconds);
        return statisticsByTime.subMap(afterThis, true, untilNow, true)
                .values()
                .stream();
    }
}
