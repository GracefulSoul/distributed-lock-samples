package com.gracefulsoul.lock.service;

import com.gracefulsoul.lock.domain.OptimisticAccount;
import com.gracefulsoul.lock.repository.OptimisticAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("낙관락(Optimistic Lock) 테스트")
public class OptimisticLockServiceTest {

    @Autowired
    private OptimisticLockService optimisticLockService;

    @Autowired
    private OptimisticAccountRepository repository;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();
        
        repository.save(OptimisticAccount.builder()
            .accountNumber("TEST-OPT-001")
            .balance(10000L)
            .build());

        repository.save(OptimisticAccount.builder()
            .accountNumber("TEST-OPT-002")
            .balance(5000L)
            .build());
    }

    @Test
    @DisplayName("낙관락 이체 성공 테스트")
    public void testTransferSuccess() {
        // When
        optimisticLockService.transfer("TEST-OPT-001", "TEST-OPT-002", 1000L);

        // Then
        OptimisticAccount from = repository.findByAccountNumber("TEST-OPT-001").get();
        OptimisticAccount to = repository.findByAccountNumber("TEST-OPT-002").get();

        assertEquals(9000L, from.getBalance());
        assertEquals(6000L, to.getBalance());
    }

    @Test
    @DisplayName("낙관락 입금 성공 테스트")
    public void testDepositSuccess() {
        // When
        optimisticLockService.deposit("TEST-OPT-001", 5000L);

        // Then
        OptimisticAccount account = repository.findByAccountNumber("TEST-OPT-001").get();
        assertEquals(15000L, account.getBalance());
    }

    @Test
    @DisplayName("낙관락 출금 성공 테스트")
    public void testWithdrawSuccess() {
        // When
        optimisticLockService.withdraw("TEST-OPT-001", 3000L);

        // Then
        OptimisticAccount account = repository.findByAccountNumber("TEST-OPT-001").get();
        assertEquals(7000L, account.getBalance());
    }

    @Test
    @DisplayName("잔액 부족 시 출금 실패")
    public void testWithdrawFail() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            optimisticLockService.withdraw("TEST-OPT-001", 20000L);
        });
    }

}
