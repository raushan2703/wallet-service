package com.example.wallet.controller;

import com.example.wallet.dto.WalletBalanceResponse;
import com.example.wallet.dto.WalletOperationRequest;
import com.example.wallet.entity.OperationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class WalletControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deposit_shouldCreateWalletAndReturnOk() {
        UUID walletId = UUID.randomUUID();
        var request = new WalletOperationRequest(walletId, OperationType.DEPOSIT, 1000L);

        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/wallet", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<WalletBalanceResponse> balanceResponse =
                restTemplate.getForEntity("/api/v1/wallets/" + walletId, WalletBalanceResponse.class);
        assertThat(balanceResponse.getBody().balance()).isEqualTo(1000L);
    }

    @Test
    void deposit_shouldAddToExistingBalance() {
        UUID walletId = UUID.randomUUID();
        restTemplate.postForEntity("/api/v1/wallet",
                new WalletOperationRequest(walletId, OperationType.DEPOSIT, 500L), Void.class);
        restTemplate.postForEntity("/api/v1/wallet",
                new WalletOperationRequest(walletId, OperationType.DEPOSIT, 300L), Void.class);

        ResponseEntity<WalletBalanceResponse> response =
                restTemplate.getForEntity("/api/v1/wallets/" + walletId, WalletBalanceResponse.class);

        assertThat(response.getBody().balance()).isEqualTo(800L);
    }

    @Test
    void withdraw_shouldDeductFromBalance() {
        UUID walletId = UUID.randomUUID();
        restTemplate.postForEntity("/api/v1/wallet",
                new WalletOperationRequest(walletId, OperationType.DEPOSIT, 1000L), Void.class);

        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/wallet",
                new WalletOperationRequest(walletId, OperationType.WITHDRAW, 400L), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<WalletBalanceResponse> balanceResponse =
                restTemplate.getForEntity("/api/v1/wallets/" + walletId, WalletBalanceResponse.class);
        assertThat(balanceResponse.getBody().balance()).isEqualTo(600L);
    }

    @Test
    void withdraw_shouldReturn400_whenInsufficientFunds() {
        UUID walletId = UUID.randomUUID();
        restTemplate.postForEntity("/api/v1/wallet",
                new WalletOperationRequest(walletId, OperationType.DEPOSIT, 100L), Void.class);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/wallet",
                new WalletOperationRequest(walletId, OperationType.WITHDRAW, 500L), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void withdraw_shouldReturn404_whenWalletNotFound() {
        UUID walletId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/wallet",
                new WalletOperationRequest(walletId, OperationType.WITHDRAW, 100L), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getBalance_shouldReturn404_whenWalletNotFound() {
        UUID walletId = UUID.randomUUID();

        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/wallets/" + walletId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void post_shouldReturn400_whenInvalidJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{invalid json}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/wallet", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void post_shouldReturn400_whenAmountIsNegative() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/wallet",
                new WalletOperationRequest(UUID.randomUUID(), OperationType.DEPOSIT, -100L), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void post_shouldReturn400_whenFieldsMissing() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/wallet",
                new WalletOperationRequest(null, OperationType.DEPOSIT, 100L), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void concurrentDeposits_shouldProduceCorrectBalance() throws InterruptedException {
        UUID walletId = UUID.randomUUID();
        int threadCount = 100;
        long depositAmount = 10L;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/wallet",
                            new WalletOperationRequest(walletId, OperationType.DEPOSIT, depositAmount), Void.class);
                    if (response.getStatusCode() == HttpStatus.OK) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount);

        ResponseEntity<WalletBalanceResponse> balanceResponse =
                restTemplate.getForEntity("/api/v1/wallets/" + walletId, WalletBalanceResponse.class);
        assertThat(balanceResponse.getBody().balance()).isEqualTo(threadCount * depositAmount);
    }
}
