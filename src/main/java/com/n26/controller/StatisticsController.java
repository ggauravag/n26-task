package com.n26.controller;

import com.n26.repository.NoRecordedTransactionException;
import com.n26.service.StatisticsService;
import com.n26.vo.StatisticsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Autowired
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping(value = "/statistics")
    public StatisticsVO getStatistics() throws NoRecordedTransactionException {
        return statisticsService.getStatistics();
    }

}
