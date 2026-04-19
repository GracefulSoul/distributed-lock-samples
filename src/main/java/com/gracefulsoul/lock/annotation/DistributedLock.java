package com.gracefulsoul.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 분산락(Distributed Lock) 어노테이션
 *
 * 사용 방법:
 * ```java
 * @Service
 * public class MyService {
 *     @DistributedLock(keys = "#accountNumber")
 *     public void transfer(String accountNumber, Long amount) {
 *         // 비즈니스 로직
 *     }
 * }
 * ```
 *
 * 동작 원리:
 * 1. 메서드 호출 전: 락 획득 시도 (waitTime, leaseTime)
 * 2. 락 획득 성공: 트랜잭션 시작
 * 3. 메서드 실행 (@DistributedLockAspect에서 처리)
 * 4. 메서드 반환: 트랜잭션 커밋
 * 5. 최종: 락 해제
 *
 * 실행 순서 보장:
 * - 락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 커밋 → 락 해제
 * - 이를 통해 @Transactional의 AOP 문제 해결
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * Redis 저장소에서 사용할 락 키 prefix
     * 
     * SpEL(Spring Expression Language) 지원:
     * - "#parameterName": 메서드 파라미터 참조
     * - "T(java.util.UUID).randomUUID()": 표현식 지원
     *
     * 예시:
     * - keys = "transfer"                    → 고정 키
     * - keys = "#accountNumber"              → 파라미터 기반
     * - keys = "#from + '-' + #to"          → 여러 파라미터 조합
     */
    String keys();

    /**
     * 락 획득 시도 시간 (초)
     * 
     * 이 시간 동안 락 획득을 시도합니다.
     * 기본값: 10초
     */
    long waitTime() default 10;

    /**
     * 락 자동 해제 시간 (초)
     * 
     * 락을 획득한 후 이 시간이 지나면 자동으로 해제됩니다.
     * Watch Dog 메커니즘으로 자동 연장됩니다.
     * 기본값: 3초
     */
    long leaseTime() default 3;

    /**
     * 락 획득 실패 시 동작
     * 
     * - THROW: RuntimeException 발생 (기본값)
     * - SKIP: 락 없이 비즈니스 로직 진행 (주의 필요)
     */
    LockFailurePolicy failurePolicy() default LockFailurePolicy.THROW;

    /**
     * 락 획득 실패 시 동작 정책
     */
    enum LockFailurePolicy {
        /**
         * 락 획득 실패 시 예외 발생
         */
        THROW,
        /**
         * 락 획득 실패 시 무시하고 진행 (위험)
         */
        SKIP
    }
}
