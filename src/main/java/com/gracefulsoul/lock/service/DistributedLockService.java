package com.gracefulsoul.lock.service;

import com.gracefulsoul.lock.annotation.DistributedLock;
import com.gracefulsoul.lock.domain.DistributedAccount;
import com.gracefulsoul.lock.repository.DistributedAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 분산락(Distributed Locking) 서비스 - Redis 기반
 *
 * 활성화 조건: spring.profiles.active=redis
 *
 * 동작 원리:
 * 1. @DistributedLock 어노테이션으로 메서드 표시
 * 2. DistributedLockAspect가 자동으로 가로채서 처리:
 *    - 락 획득 (Redis)
 *    - 트랜잭션 시작
 *    - 비즈니스 로직 실행
 *    - 트랜잭션 커밋
 *    - 락 해제
 * 3. 실행 순서 보장: 락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 커밋 → 락 해제
 *
 * ✅ 장점:
 * - 분산락 로직이 AOP로 공통화됨 (코드 중복 제거)
 * - 비즈니스 로직만 깔끔하게 작성 가능
 * - @Transactional의 AOP 문제 자동 해결
 * - 모든 메서드에 일관된 순서 보장
 *
 * ⚠️ 주의사항:
 * - AOP는 public 메서드에만 적용됨
 * - 반드시 다른 빈(Bean)에서 호출되어야 적용됨 (self-invocation 안 됨)
 * - Exception 발생 시 자동으로 트랜잭션 롤백됨
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("redis")
public class DistributedLockService {

    private final DistributedAccountRepository accountRepository;

    /**
     * 분산락 기반 계좌 이체
     *
     * @DistributedLock("transfer") 처리 순서:
     * 1. 락 획득 (account1, account2 키 기반)
     * 2. 트랜잭션 시작
     * 3. 이체 로직 실행
     * 4. 트랜잭션 커밋 (DB에 반영)
     * 5. 락 해제 (다른 스레드가 안전하게 접근 가능)
     */
    @DistributedLock(keys = "transfer", waitTime = 10, leaseTime = 3)
    public void transfer(String fromAccountNumber, String toAccountNumber, Long amount) {
        DistributedAccount fromAccount = accountRepository
            .findByAccountNumber(fromAccountNumber)
            .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다: " + fromAccountNumber));

        DistributedAccount toAccount = accountRepository
            .findByAccountNumber(toAccountNumber)
            .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다: " + toAccountNumber));

        // 비즈니스 로직만 작성 (분산락은 AOP가 처리)
        fromAccount.withdraw(amount);
        toAccount.deposit(amount);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        log.info("분산락 이체 완료: {} → {}, 금액: {}", fromAccountNumber, toAccountNumber, amount);
    }

    /**
     * 분산락 기반 입금
     *
     * @DistributedLock("deposit") 처리 순서:
     * 1. 락 획득
     * 2. 트랜잭션 시작
     * 3. 입금 로직 실행
     * 4. 트랜잭션 커밋
     * 5. 락 해제
     */
    @DistributedLock(keys = "deposit", waitTime = 10, leaseTime = 3)
    public void deposit(String accountNumber, Long amount) {
        DistributedAccount account = accountRepository
            .findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다: " + accountNumber));

        // 비즈니스 로직만 작성 (분산락은 AOP가 처리)
        account.deposit(amount);
        accountRepository.save(account);

        log.info("분산락 입금 완료: {}, 금액: {}", accountNumber, amount);
    }

    /**
     * 분산락 기반 출금
     *
     * @DistributedLock("withdraw") 처리 순서:
     * 1. 락 획득
     * 2. 트랜잭션 시작
     * 3. 출금 로직 실행
     * 4. 트랜잭션 커밋
     * 5. 락 해제
     */
    @DistributedLock(keys = "withdraw", waitTime = 10, leaseTime = 3)
    public void withdraw(String accountNumber, Long amount) {
        DistributedAccount account = accountRepository
            .findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다: " + accountNumber));

        // 비즈니스 로직만 작성 (분산락은 AOP가 처리)
        account.withdraw(amount);
        accountRepository.save(account);

        log.info("분산락 출금 완료: {}, 금액: {}", accountNumber, amount);
    }

}
