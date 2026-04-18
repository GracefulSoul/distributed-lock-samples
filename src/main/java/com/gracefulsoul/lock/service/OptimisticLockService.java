package com.gracefulsoul.lock.service;

import com.gracefulsoul.lock.domain.OptimisticAccount;
import com.gracefulsoul.lock.repository.OptimisticAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 낙관락(Optimistic Locking) 서비스
 *
 * 동작 원리:
 * 1. 엔티티 조회 시 버전값(Version) 읽음
 * 2. 비즈니스 로직 처리
 * 3. 업데이트 시 WHERE 절에 버전값 추가
 *    UPDATE optimistic_account SET balance = ?, version = version + 1
 *    WHERE id = ? AND version = ?
 * 4. 버전이 일치하지 않으면 ObjectOptimisticLockingFailureException 발생
 * 5. 필요시 재시도(Retry) 로직 구현
 *
 * 특징:
 * - 충돌 빈도가 낮은 경우 성능 우수
 * - 충돌 시 Exception 처리 필요
 * - 디버깅 시 버전값 확인 필요
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimisticLockService {

    private final OptimisticAccountRepository accountRepository;

    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 낙관락 기반 계좌 이체
     * @param fromAccountNumber 출금 계좌
     * @param toAccountNumber 입금 계좌
     * @param amount 이체 금액
     */
    @Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, Long amount) {
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                OptimisticAccount fromAccount = accountRepository
                    .findByAccountNumberWithOptimisticLock(fromAccountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

                OptimisticAccount toAccount = accountRepository
                    .findByAccountNumberWithOptimisticLock(toAccountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

                // 비즈니스 로직 처리
                fromAccount.withdraw(amount);
                toAccount.deposit(amount);

                // 저장 시 버전 체크 및 업데이트
                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);

                log.info("이체 완료: {} -> {}, 금액: {}", fromAccountNumber, toAccountNumber, amount);
                return;

            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.warn("낙관락 충돌 발생. 재시도 횟수: {}/{}", retryCount, MAX_RETRY_COUNT);

                if (retryCount >= MAX_RETRY_COUNT) {
                    throw new RuntimeException("최대 재시도 횟수 초과", e);
                }
            }
        }
    }

    /**
     * 낙관락 기반 입금
     */
    @Transactional
    public void deposit(String accountNumber, Long amount) {
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                OptimisticAccount account = accountRepository
                    .findByAccountNumberWithOptimisticLock(accountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

                account.deposit(amount);
                accountRepository.save(account);

                log.info("입금 완료: {}, 금액: {}", accountNumber, amount);
                return;

            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.warn("낙관락 충돌 발생. 재시도 횟수: {}/{}", retryCount, MAX_RETRY_COUNT);

                if (retryCount >= MAX_RETRY_COUNT) {
                    throw new RuntimeException("최대 재시도 횟수 초과", e);
                }
            }
        }
    }

    /**
     * 낙관락 기반 출금
     */
    @Transactional
    public void withdraw(String accountNumber, Long amount) {
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                OptimisticAccount account = accountRepository
                    .findByAccountNumberWithOptimisticLock(accountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다"));

                account.withdraw(amount);
                accountRepository.save(account);

                log.info("출금 완료: {}, 금액: {}", accountNumber, amount);
                return;

            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.warn("낙관락 충돌 발생. 재시도 횟수: {}/{}", retryCount, MAX_RETRY_COUNT);

                if (retryCount >= MAX_RETRY_COUNT) {
                    throw new RuntimeException("최대 재시도 횟수 초과", e);
                }
            }
        }
    }

}
