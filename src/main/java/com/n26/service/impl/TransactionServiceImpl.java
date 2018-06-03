package com.n26.service.impl;

import com.n26.model.Transaction;
import com.n26.repository.TransactionRepository;
import com.n26.service.StaleTransactionException;
import com.n26.service.TransactionService;
import com.n26.vo.TransactionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;

    private final Clock clock;

    @Autowired
    public TransactionServiceImpl(TransactionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void save(TransactionVO transactionVO) throws StaleTransactionException {
        final Instant currentInstant = clock.instant();
        if (currentInstant.toEpochMilli() - transactionVO.getTimestamp() > repository.getTransactionValidityInMilliSeconds()) {
            throw new StaleTransactionException("Transaction is stale");
        } else if (currentInstant.toEpochMilli() - transactionVO.getTimestamp() < 0) {
            throw new StaleTransactionException("Transaction is invalid");
        }

        Transaction transaction = new Transaction(transactionVO.getAmount(), transactionVO.getTimestamp());
        repository.save(transaction);
        repository.removeStaleTransactions(currentInstant);
    }
}
