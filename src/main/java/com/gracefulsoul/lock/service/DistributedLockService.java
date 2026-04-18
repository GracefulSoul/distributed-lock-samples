package com.gracefulsoul.lock.service;

import com.gracefulsoul.lock.domain.DistributedAccount;
import com.gracefulsoul.lock.repository.DistributedAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 분산락(Distributed Locking) 서비스 - Redis 기반
 *
 * 활성화 조건: spring.profiles.active=redis
 *
 * 동작 원리:
 * 1. Redis를 사용한 분산 환경의 동시성 제어
 * 2. 비즈니스 로직 처리 전 락 획득 (Redisson RLock 사용)
 * 3. 타임아웃 설정으로 데드락 방지
 * 4. 락 획득 실패 시 재시도 또는 에러 처리
 * 5. 비즈니스 로직 완료 후 락 해제
 *
 * Redisson RLock 특징:
 * - 재진입 가능한 락(Reentrant Lock)
 * - 자동 연장(Watch Dog 메커니즘)
 * - 타임아웃 설정으로 자동 해제
 * - 분산 환경에서 안전한 동시성 제어
 *
 * 사용 시기:
 * - 마이크로서비스 아키텍처
 * - 다중 인스턴스 배포
 * - 높은 동시성 처리 필요 시
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("redis")
public class DistributedLockService {

    private final DistributedAccountRepository accountRepository;
    private final RedissonClient redissonClient;

    private static final long LOCK_WAIT_TIME = 10; // 초
    private static final long LOCK_LEASE_TIME = 3;  // 초
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    /**
     * 분산락 기반 계좌 이체
     * @param fromAccountNumber 출금 계좌
     * @param toAccountNumber 입금 계좌
     * @param amount 이체 금액
     */
    @Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, Long amount) {
        // 데드락 방지: 계좌번호 기준으로 일관된 순서로 락 획득
        String[] accounts = {fromAccountNumber, toAccountNumber};
        java.util.Arrays.sort(accounts);

        RLock lock1 = redissonClient.getLock("account-lock:" + accounts[0]);
        RLock lock2 = redissonClient.getLock("account-lock:" + accounts[1]);

        try {
            // 두 개의 락을 모두 획득 (타임아웃 설정)
            boolean lock1Acquired = lock1.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TIME_UNIT);
            if (!lock1Acquired) {
                throw new RuntimeException("첫 번째 락 획득 실패");
            }

            boolean lock2Acquired = lock2.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TIME_UNIT);
            if (!lock2Acquired) {
                throw new RuntimeException("두 번째 락 획득 실패");
            }

            try {
                // 락 획득 후 DB 조회 및 업데이트
                DistributedAccount fromAccount = accountRepository
                    .findByAccountNumber(fromAccountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

                DistributedAccount toAccount = accountRepository
                    .findByAccountNumber(toAccountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

                // 비즈니스 로직 처리
                fromAccount.withdraw(amount);
                toAccount.deposit(amount);

                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);

                log.info("분산락 이체 완료: {} -> {}, 금액: {}", fromAccountNumber, toAccountNumber, amount);

            } finally {
                // 락 해제
                if (lock2.isHeldByCurrentThread()) {
                    lock2.unlock();
                }
                if (lock1.isHeldByCurrentThread()) {
                    lock1.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        }
    }

    /**
     * 분산락 기반 입금
     */
    @Transactional
    public void deposit(String accountNumber, Long amount) {
        RLock lock = redissonClient.getLock("account-lock:" + accountNumber);

        try {
            boolean lockAcquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TIME_UNIT);
            if (!lockAcquired) {
                throw new RuntimeException("락 획득 실패");
            }

            try {
                DistributedAccount account = accountRepository
                    .findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

                account.deposit(amount);
                accountRepository.save(account);

                log.info("분산락 입금 완료: {}, 금액: {}", accountNumber, amount);

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        }
    }

    /**
     * 분산락 기반 출금
     */
    @Transactional
    public void withdraw(String accountNumber, Long amount) {
        RLock lock = redissonClient.getLock("account-lock:" + accountNumber);

        try {
            boolean lockAcquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TIME_UNIT);
            if (!lockAcquired) {
                throw new RuntimeException("락 획득 실패");
            }

            try {
                DistributedAccount account = accountRepository
                    .findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

                account.withdraw(amount);
                accountRepository.save(account);

                log.info("분산락 출금 완료: {}, 금액: {}", accountNumber, amount);

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        }
    }

}
