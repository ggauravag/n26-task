package com.n26.service.impl;

import com.n26.repository.NoRecordedTransactionException;
import com.n26.repository.TransactionRepository;
import com.n26.service.StatisticsService;
import com.n26.vo.StatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final TransactionRepository transactionRepository;

    private final Clock clock;

    @Autowired
    public StatisticsServiceImpl(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Override
    public StatisticsVO getStatistics() throws NoRecordedTransactionException {
        final Instant currentTime = clock.instant();
        StatisticsVO statisticsVO = new StatisticsVO();
        statisticsVO.setAvg(transactionRepository.getAverage(currentTime));
        statisticsVO.setSum(transactionRepository.getSum(currentTime));
        statisticsVO.setMin(transactionRepository.getMinimum(currentTime));
        statisticsVO.setMax(transactionRepository.getMaximum(currentTime));
        statisticsVO.setCount(transactionRepository.getCount(currentTime));
        return statisticsVO;
    }
}
