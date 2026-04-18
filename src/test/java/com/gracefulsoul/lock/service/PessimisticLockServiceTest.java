package com.gracefulsoul.lock.service;

import com.gracefulsoul.lock.domain.PessimisticAccount;
import com.gracefulsoul.lock.repository.PessimisticAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("비관락(Pessimistic Lock) 테스트")
public class PessimisticLockServiceTest {

    @Autowired
    private PessimisticLockService pessimisticLockService;

    @Autowired
    private PessimisticAccountRepository repository;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();

        repository.save(PessimisticAccount.builder()
            .accountNumber("TEST-PES-001")
            .balance(10000L)
            .build());

        repository.save(PessimisticAccount.builder()
            .accountNumber("TEST-PES-002")
            .balance(5000L)
            .build());
    }

    @Test
    @DisplayName("비관락 이체 성공 테스트")
    public void testTransferSuccess() {
        // When
        pessimisticLockService.transfer("TEST-PES-001", "TEST-PES-002", 1000L);

        // Then
        PessimisticAccount from = repository.findByAccountNumber("TEST-PES-001").get();
        PessimisticAccount to = repository.findByAccountNumber("TEST-PES-002").get();

        assertEquals(9000L, from.getBalance());
        assertEquals(6000L, to.getBalance());
    }

    @Test
    @DisplayName("비관락 입금 성공 테스트")
    public void testDepositSuccess() {
        // When
        pessimisticLockService.deposit("TEST-PES-001", 5000L);

        // Then
        PessimisticAccount account = repository.findByAccountNumber("TEST-PES-001").get();
        assertEquals(15000L, account.getBalance());
    }

    @Test
    @DisplayName("비관락 잔액 조회 성공 테스트")
    public void testGetBalanceSuccess() {
        // When
        Long balance = pessimisticLockService.getBalance("TEST-PES-001");

        // Then
        assertEquals(10000L, balance);
    }

}
