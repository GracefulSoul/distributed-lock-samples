package com.gracefulsoul.lock.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 분산락(Distributed Locking) 샘플 엔티티
 * - Redis 기반의 분산락을 사용하여 다중 서버 환경에서 동시성 제어
 * - 락 정보는 Redis에 저장되며, DB 조회 전 획득
 */
@Entity
@Table(name = "distributed_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistributedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private Long balance;

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
