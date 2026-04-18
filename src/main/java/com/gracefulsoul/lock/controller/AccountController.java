package com.gracefulsoul.lock.controller;

import com.gracefulsoul.lock.service.OptimisticLockService;
import com.gracefulsoul.lock.service.PessimisticLockService;
import com.gracefulsoul.lock.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final OptimisticLockService optimisticLockService;
    private final PessimisticLockService pessimisticLockService;
    
    // Redis 프로필이 활성화되지 않으면 null
    private final ObjectProvider<DistributedLockService> distributedLockServiceProvider;

    // ========== Optimistic Lock API ==========

    @PostMapping("/optimistic/transfer")
    public ResponseEntity<String> optimisticTransfer(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam Long amount) {
        try {
            optimisticLockService.transfer(from, to, amount);
            return ResponseEntity.ok("낙관락 이체 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("이체 실패: " + e.getMessage());
        }
    }

    @PostMapping("/optimistic/deposit")
    public ResponseEntity<String> optimisticDeposit(
            @RequestParam String accountNumber,
            @RequestParam Long amount) {
        try {
            optimisticLockService.deposit(accountNumber, amount);
            return ResponseEntity.ok("낙관락 입금 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("입금 실패: " + e.getMessage());
        }
    }

    @PostMapping("/optimistic/withdraw")
    public ResponseEntity<String> optimisticWithdraw(
            @RequestParam String accountNumber,
            @RequestParam Long amount) {
        try {
            optimisticLockService.withdraw(accountNumber, amount);
            return ResponseEntity.ok("낙관락 출금 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("출금 실패: " + e.getMessage());
        }
    }

    // ========== Pessimistic Lock API ==========

    @PostMapping("/pessimistic/transfer")
    public ResponseEntity<String> pessimisticTransfer(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam Long amount) {
        try {
            pessimisticLockService.transfer(from, to, amount);
            return ResponseEntity.ok("비관락 이체 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("이체 실패: " + e.getMessage());
        }
    }

    @PostMapping("/pessimistic/deposit")
    public ResponseEntity<String> pessimisticDeposit(
            @RequestParam String accountNumber,
            @RequestParam Long amount) {
        try {
            pessimisticLockService.deposit(accountNumber, amount);
            return ResponseEntity.ok("비관락 입금 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("입금 실패: " + e.getMessage());
        }
    }

    @PostMapping("/pessimistic/withdraw")
    public ResponseEntity<String> pessimisticWithdraw(
            @RequestParam String accountNumber,
            @RequestParam Long amount) {
        try {
            pessimisticLockService.withdraw(accountNumber, amount);
            return ResponseEntity.ok("비관락 출금 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("출금 실패: " + e.getMessage());
        }
    }

    @GetMapping("/pessimistic/balance")
    public ResponseEntity<Long> getPessimisticBalance(
            @RequestParam String accountNumber) {
        try {
            Long balance = pessimisticLockService.getBalance(accountNumber);
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ========== Distributed Lock API (Redis Profile Only) ==========

    @PostMapping("/distributed/transfer")
    public ResponseEntity<String> distributedTransfer(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam Long amount) {
        DistributedLockService service = distributedLockServiceProvider.getIfAvailable();
        if (service == null) {
            return ResponseEntity.status(503)
                .body("분산락 기능을 사용하려면 Redis 프로필을 활성화하세요: --spring.profiles.active=redis");
        }
        
        try {
            service.transfer(from, to, amount);
            return ResponseEntity.ok("분산락 이체 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("이체 실패: " + e.getMessage());
        }
    }

    @PostMapping("/distributed/deposit")
    public ResponseEntity<String> distributedDeposit(
            @RequestParam String accountNumber,
            @RequestParam Long amount) {
        DistributedLockService service = distributedLockServiceProvider.getIfAvailable();
        if (service == null) {
            return ResponseEntity.status(503)
                .body("분산락 기능을 사용하려면 Redis 프로필을 활성화하세요: --spring.profiles.active=redis");
        }
        
        try {
            service.deposit(accountNumber, amount);
            return ResponseEntity.ok("분산락 입금 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("입금 실패: " + e.getMessage());
        }
    }

    @PostMapping("/distributed/withdraw")
    public ResponseEntity<String> distributedWithdraw(
            @RequestParam String accountNumber,
            @RequestParam Long amount) {
        DistributedLockService service = distributedLockServiceProvider.getIfAvailable();
        if (service == null) {
            return ResponseEntity.status(503)
                .body("분산락 기능을 사용하려면 Redis 프로필을 활성화하세요: --spring.profiles.active=redis");
        }
        
        try {
            service.withdraw(accountNumber, amount);
            return ResponseEntity.ok("분산락 출금 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("출금 실패: " + e.getMessage());
        }
    }

}
