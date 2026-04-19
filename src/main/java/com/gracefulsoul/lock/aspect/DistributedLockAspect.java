package com.gracefulsoul.lock.aspect;

import com.gracefulsoul.lock.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 분산락(Distributed Lock) AOP Aspect
 *
 * 목적:
 * - @DistributedLock 어노테이션을 감지하여 자동으로 분산락 처리
 * - 모든 메서드에 일관된 분산락 로직 적용
 * - 코드 중복 제거 및 유지보수성 향상
 *
 * 실행 흐름:
 * 1. @DistributedLock이 붙은 메서드 감지
 * 2. SpEL로 락 키 생성 (메서드 파라미터 기반)
 * 3. 락 획득 시도 (waitTime만큼 대기)
 * 4. 락 획득 성공:
 *    - 트랜잭션 시작
 *    - 메서드 실행 (joinPoint.proceed())
 *    - 트랜잭션 커밋
 *    - 락 해제
 * 5. 락 획득 실패:
 *    - failurePolicy에 따라 처리 (THROW 또는 SKIP)
 *
 * 중요한 보장사항:
 * ✅ 락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 커밋 → 락 해제
 * ✅ @Transactional의 AOP 문제 해결
 * ✅ 다른 스레드는 커밋 완료 후 데이터 접근 가능
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("redis")
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * @DistributedLock이 붙은 모든 public 메서드를 가로챔
     */
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // Step 1: 메서드 시그니처 및 파라미터 정보 추출
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // Step 2: SpEL로 락 키 생성
        String lockKey = generateLockKey(distributedLock.keys(), paramNames, args);
        log.debug("분산락 처리 시작: method={}, lockKey={}", method.getName(), lockKey);

        // Step 3: 락 객체 생성
        RLock lock = redissonClient.getLock("distributed-lock:" + lockKey);

        try {
            // Step 4: 락 획득 시도
            boolean lockAcquired = lock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                TimeUnit.SECONDS
            );

            if (!lockAcquired) {
                // 락 획득 실패 처리
                return handleLockFailure(distributedLock, method, lockKey);
            }

            try {
                // Step 5: 트랜잭션 시작 (락 획득 이후!)
                DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
                TransactionStatus txStatus = transactionManager.getTransaction(txDef);

                try {
                    // Step 6: 실제 비즈니스 로직 실행
                    Object result = joinPoint.proceed();

                    // Step 7: 트랜잭션 커밋 (DB에 최종 반영)
                    transactionManager.commit(txStatus);
                    log.debug("분산락 처리 완료: method={}, lockKey={}", method.getName(), lockKey);

                    return result;

                } catch (Throwable e) {
                    // Step 8: 예외 발생 시 트랜잭션 롤백
                    transactionManager.rollback(txStatus);
                    log.error("분산락 처리 중 예외 발생: method={}, lockKey={}", method.getName(), lockKey, e);
                    throw e;
                }

            } finally {
                // Step 9: 락 해제 (트랜잭션 커밋 이후!)
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("분산락 해제: lockKey={}", lockKey);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("분산락 획득 중 인터럽트 발생: lockKey={}", lockKey, e);
            throw new RuntimeException("분산락 획득 중 인터럽트 발생", e);
        }
    }

    /**
     * SpEL을 사용하여 락 키 생성
     *
     * 예시:
     * - keys = "transfer" → "transfer"
     * - keys = "#accountNumber" → "account-123" (파라미터 값)
     * - keys = "#from + '-' + #to" → "account1-account2"
     */
    private String generateLockKey(String spel, String[] paramNames, Object[] args) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();

            // 메서드 파라미터를 SpEL 컨텍스트에 추가
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            // SpEL 파싱 및 평가
            Object value = expressionParser.parseExpression(spel).getValue(context);
            return value != null ? value.toString() : spel;

        } catch (Exception e) {
            log.warn("SpEL 파싱 실패: spel={}, 고정 키로 사용", spel, e);
            return spel; // 파싱 실패 시 고정 키로 사용
        }
    }

    /**
     * 락 획득 실패 처리
     */
    private Object handleLockFailure(DistributedLock distributedLock, Method method, String lockKey) {
        switch (distributedLock.failurePolicy()) {
            case THROW:
                log.error("분산락 획득 실패: method={}, lockKey={}", method.getName(), lockKey);
                throw new RuntimeException("분산락 획득 실패: " + lockKey);

            case SKIP:
                log.warn("분산락 획득 실패 (SKIP 정책): method={}, lockKey={}", method.getName(), lockKey);
                // 락 없이 비즈니스 로직 진행 (위험!)
                // 이 경우 동시성 제어가 보장되지 않음
                return null;

            default:
                throw new RuntimeException("알 수 없는 failurePolicy: " + distributedLock.failurePolicy());
        }
    }
}
