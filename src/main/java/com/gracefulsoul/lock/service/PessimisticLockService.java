package com.gracefulsoul.lock.service;

import com.gracefulsoul.lock.domain.PessimisticAccount;
import com.gracefulsoul.lock.repository.PessimisticAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비관락(Pessimistic Locking) 서비스
 *
 * 동작 원리:
 * 1. 데이터 조회 시 즉시 행 잠금 획득 (SELECT FOR UPDATE)
 * 2. 트랜잭션 내에서만 다른 트랜잭션의 접근 차단
 * 3. 비즈니스 로직 처리
 * 4. 업데이트 수행
 * 5. 트랜잭션 종료 시 잠금 해제
 *
 * 배타락(PESSIMISTIC_WRITE)과 공유락(PESSIMISTIC_READ):
 * - 배타락: 읽기/쓰기 모두 차단 (UPDATE, DELETE 시 사용)
 * - 공유락: 다른 공유락은 허용, 배타락만 차단 (SELECT 시 사용)
 *
 * 특징:
 * - 충돌이 많은 경우 성능 우수
 * - 데드락 주의 필요
 * - 락 대기로 인한 응답 지연 가능
 * - 명시적인 잠금으로 데이터 일관성 보장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PessimisticLockService {

    private final PessimisticAccountRepository accountRepository;

    /**
     * 비관락 기반 계좌 이체 (배타락 사용)
     * @param fromAccountNumber 출금 계좌
     * @param toAccountNumber 입금 계좌
     * @param amount 이체 금액
     */
    @Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, Long amount) {
        // 두 계좌의 ID를 기반으로 일관된 순서로 잠금 (데드락 방지)
        PessimisticAccount fromAccount = accountRepository
            .findByAccountNumberWithPessimisticWriteLock(fromAccountNumber)
            .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

        PessimisticAccount toAccount = accountRepository
            .findByAccountNumberWithPessimisticWriteLock(toAccountNumber)
            .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

        // 비즈니스 로직 처리 (이미 잠금 획득)
        fromAccount.withdraw(amount);
        toAccount.deposit(amount);

        // 자동 Flush 및 업데이트 (트랜잭션 종료 시 잠금 해제)
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        log.info("비관락 이체 완료: {} -> {}, 금액: {}", fromAccountNumber, toAccountNumber, amount);
    }

    /**
     * 비관락 기반 입금 (배타락 사용)
     */
    @Transactional
    public void deposit(String accountNumber, Long amount) {
        PessimisticAccount account = accountRepository
            .findByAccountNumberWithPessimisticWriteLock(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

        account.deposit(amount);
        accountRepository.save(account);

        log.info("비관락 입금 완료: {}, 금액: {}", accountNumber, amount);
    }

    /**
     * 비관락 기반 출금 (배타락 사용)
     */
    @Transactional
    public void withdraw(String accountNumber, Long amount) {
        PessimisticAccount account = accountRepository
            .findByAccountNumberWithPessimisticWriteLock(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

        account.withdraw(amount);
        accountRepository.save(account);

        log.info("비관락 출금 완료: {}, 금액: {}", accountNumber, amount);
    }

    /**
     * 비관락 기반 잔액 조회 (공유락 사용)
     * - 읽기만 필요한 경우 공유락으로 성능 개선
     * - 다른 트랜잭션의 공유락 조회는 허용
     */
    @Transactional(readOnly = true)
    public Long getBalance(String accountNumber) {
        PessimisticAccount account = accountRepository
            .findByAccountNumberWithPessimisticReadLock(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

        log.info("비관락 잔액 조회: {}, 잔액: {}", accountNumber, account.getBalance());
        return account.getBalance();
    }

}
