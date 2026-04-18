package com.gracefulsoul.lock.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 낙관락(Optimistic Locking) 샘플 엔티티
 * - 버전 필드를 통해 동시성 제어
 * - 업데이트 시 버전 확인으로 충돌 감지
 */
@Entity
@Table(name = "optimistic_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimisticAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private Long balance;

    /**
     * 버전 필드: 낙관락의 핵심
     * - JPA에서 자동으로 관리
     * - 엔티티 업데이트 시마다 증가
     * - 업데이트 쿼리의 WHERE 절에 포함되어 동시성 충돌 감지
     */
    @Version
    private Long version;

    public void withdraw(Long amount) {
        if (balance < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다");
        }
        this.balance -= amount;
    }

    public void deposit(Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("입금액은 0보다 커야 합니다");
        }
        this.balance += amount;
    }

}
