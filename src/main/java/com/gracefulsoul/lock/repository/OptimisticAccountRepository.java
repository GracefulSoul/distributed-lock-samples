package com.gracefulsoul.lock.repository;

import com.gracefulsoul.lock.domain.OptimisticAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OptimisticAccountRepository extends JpaRepository<OptimisticAccount, Long> {

    Optional<OptimisticAccount> findByAccountNumber(String accountNumber);

    /**
     * 낙관락을 명시적으로 사용하는 조회
     * - @Lock(LockModeType.OPTIMISTIC) 사용
     * - 버전 필드를 통한 동시성 제어
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT a FROM OptimisticAccount a WHERE a.accountNumber = :accountNumber")
    Optional<OptimisticAccount> findByAccountNumberWithOptimisticLock(@Param("accountNumber") String accountNumber);

}
