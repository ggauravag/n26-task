package com.n26.controller;

import com.n26.util.StatisticsDTO;
import com.n26.vo.TransactionVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Clock;
import java.time.Instant;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StatisticsControllerIntegrationTest {

    @TestConfiguration
    static class IntegrationTestConfiguration {

        static Clock clock = Mockito.mock(Clock.class);

        @Bean
        public Clock clock() {
            return clock;
        }

    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldReturn404WhenNoValidTransactions() {
        // having
        when(IntegrationTestConfiguration.clock.instant()).thenReturn(Instant.now());

        // when
        final ResponseEntity<StatisticsDTO> response = restTemplate.getForEntity("/statistics", StatisticsDTO.class);

        // then
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    @Test
    @DirtiesContext
    public void shouldGetCorrectStatisticsForLast60SecondsWhenConcurrentRequests() throws InterruptedException {
        // having
        final Instant now = Instant.now();

        // Save stale transactions
        when(IntegrationTestConfiguration.clock.instant()).thenReturn(now);
        for (int i = 0; i < 5; i++) {
            restTemplate.postForEntity("/transactions", new TransactionVO(20.0, now.toEpochMilli()), Object.class);
        }

        // Save valid transactions
        final Instant after30Seconds = now.plusSeconds(30);
        double expectedSum = 0.0;
        double expectedAvg = 0.0;
        double expectedMin = Double.MAX_VALUE;
        double expectedMax = Double.MIN_VALUE;

        when(IntegrationTestConfiguration.clock.instant()).thenReturn(after30Seconds);
        for (int i = 0; i < 3; i++) {
            final double amount = 1000 * Math.random();
            expectedAvg = (expectedAvg * i + amount) / (i + 1);
            expectedMax = expectedMax > amount ? expectedMax : amount;
            expectedMin = expectedMin < amount ? expectedMin : amount;
            expectedSum += amount;
            restTemplate.postForEntity("/transactions", new TransactionVO(amount, after30Seconds.toEpochMilli()), Object.class);
        }

        // when
        when(IntegrationTestConfiguration.clock.instant()).thenReturn(now.plusSeconds(61));
        final ResponseEntity<StatisticsDTO> response = restTemplate.getForEntity("/statistics", StatisticsDTO.class);

        // then
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        final StatisticsDTO stats = response.getBody();
        assertThat(stats.count, is(3L));
        assertThat(stats.sum, closeTo(expectedSum, 0.001));
        assertThat(stats.avg, closeTo(expectedAvg, 0.001));
        assertThat(stats.max, closeTo(expectedMax, 0.001));
        assertThat(stats.min, closeTo(expectedMin, 0.001));
    }
}
