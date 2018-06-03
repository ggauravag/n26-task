package com.n26.controller;

import com.n26.util.TransactionDTO;
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
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Clock;
import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionControllerIntegrationTest {

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
    public void shouldReturn201WhenSavingValidTransactionWithin60Seconds() {
        // having
        final Instant now = Instant.now();
        final TransactionDTO transactionToSave = new TransactionDTO(15.0, now.toEpochMilli());

        final Instant justAt60Seconds = now.plusSeconds(60);
        when(IntegrationTestConfiguration.clock.instant()).thenReturn(justAt60Seconds);

        // when
        final ResponseEntity<Object> response = restTemplate.postForEntity("/transactions", transactionToSave, Object.class);

        // then
        assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
    }

    @Test
    public void shouldReturn204WhenSavingStaleTransactionAfter60Seconds() {
        // having
        final Instant now = Instant.now();
        final TransactionDTO transactionToSave = new TransactionDTO(25.0, now.toEpochMilli());

        final Instant after60Seconds = now.plusSeconds(61);
        when(IntegrationTestConfiguration.clock.instant()).thenReturn(after60Seconds);

        // when
        final ResponseEntity<Object> response = restTemplate.postForEntity("/transactions", transactionToSave, Object.class);

        // then
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
    }
}
