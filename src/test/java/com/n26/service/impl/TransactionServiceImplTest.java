package com.n26.service.impl;

import com.n26.repository.TransactionRepository;
import com.n26.service.StaleTransactionException;
import com.n26.vo.TransactionVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

    @InjectMocks
    private TransactionServiceImpl service;

    @Mock
    private TransactionRepository repository;

    @Mock
    private Clock clock;

    @Before
    public void setUp() throws Exception {
        when(repository.getTransactionValidityInMilliSeconds()).thenReturn(60000L);
    }

    @Test
    public void shouldNotThrowWhenSavingValidTransaction() throws StaleTransactionException {
        // having
        final Instant now = Instant.now();
        final TransactionVO transaction = new TransactionVO(15.0, now.toEpochMilli());
        when(clock.instant()).thenReturn(now.plusSeconds(60));

        // when
        service.save(transaction);

        // then
        verify(repository).save(argThat(tx -> tx.getAmount() == 15.0 && tx.getTimestamp() == now.toEpochMilli()));
    }

    @Test(expected = StaleTransactionException.class)
    public void shouldThrowWhenSavingStaleTransaction() throws StaleTransactionException {
        // having
        final Instant now = Instant.now();
        final TransactionVO transaction = new TransactionVO(15.0, now.toEpochMilli());
        when(clock.instant()).thenReturn(now.plusSeconds(61));

        // when
        service.save(transaction);
    }

    @Test(expected = StaleTransactionException.class)
    public void shouldThrowWhenSavingInvalidTransaction() throws StaleTransactionException {
        // having
        final Instant now = Instant.now();
        final TransactionVO transaction = new TransactionVO(15.0, now.plusSeconds(60).toEpochMilli());
        when(clock.instant()).thenReturn(now);

        // when
        service.save(transaction);
    }
}