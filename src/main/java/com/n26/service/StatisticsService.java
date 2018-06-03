package com.n26.service;

import com.n26.repository.NoRecordedTransactionException;
import com.n26.vo.StatisticsVO;

public interface StatisticsService {

    StatisticsVO getStatistics() throws NoRecordedTransactionException;

}
