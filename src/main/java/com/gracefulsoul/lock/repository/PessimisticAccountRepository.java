package com.gracefulsoul.lock.repository;

import com.gracefulsoul.lock.domain.PessimisticAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PessimisticAccountRepository extends JpaRepository<PessimisticAccount, Long> {

    Optional<PessimisticAccount> findByAccountNumber(String accountNumber);

    /**
     * 배타락(Exclusive Lock)을 사용하는 조회
     * - @Lock(LockModeType.PESSIMISTIC_WRITE) 사용
     * - 행 수준의 배타락으로 다른 트랜잭션의 접근 차단
     * - SELECT FOR UPDATE 쿼리 생성
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM PessimisticAccount a WHERE a.accountNumber = :accountNumber")
    Optional<PessimisticAccount> findByAccountNumberWithPessimisticWriteLock(@Param("accountNumber") String accountNumber);

    /**
     * 공유락(Shared Lock)을 사용하는 조회
     * - @Lock(LockModeType.PESSIMISTIC_READ) 사용
     * - 읽기 전용 작업에 사용
     * - SELECT FOR UPDATE SHARED 쿼리 생성 (지원하는 DB의 경우)
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT a FROM PessimisticAccount a WHERE a.accountNumber = :accountNumber")
    Optional<PessimisticAccount> findByAccountNumberWithPessimisticReadLock(@Param("accountNumber") String accountNumber);

}
