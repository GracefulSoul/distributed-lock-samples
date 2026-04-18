package com.gracefulsoul.lock.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 비관락(Pessimistic Locking) 샘플 엔티티
 * - 공유락(Shared Lock) 또는 배타락(Exclusive Lock)을 사용
 * - 트랜잭션 내에서 명시적으로 행 잠금
 */
@Entity
@Table(name = "pessimistic_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PessimisticAccount {

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
