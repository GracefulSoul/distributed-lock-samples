package com.gracefulsoul.lock;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.gracefulsoul.lock.domain.OptimisticAccount;
import com.gracefulsoul.lock.domain.PessimisticAccount;
import com.gracefulsoul.lock.domain.DistributedAccount;
import com.gracefulsoul.lock.repository.OptimisticAccountRepository;
import com.gracefulsoul.lock.repository.PessimisticAccountRepository;
import com.gracefulsoul.lock.repository.DistributedAccountRepository;

/**
 * 애플리케이션 시작 시 샘플 데이터 생성
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeData(
            OptimisticAccountRepository optimisticRepo,
            PessimisticAccountRepository pessimisticRepo,
            DistributedAccountRepository distributedRepo) {

        return args -> {
            // 낙관락 샘플 데이터
            optimisticRepo.save(OptimisticAccount.builder()
                .accountNumber("OPT-001")
                .balance(10000L)
                .build());

            optimisticRepo.save(OptimisticAccount.builder()
                .accountNumber("OPT-002")
                .balance(5000L)
                .build());

            // 비관락 샘플 데이터
            pessimisticRepo.save(PessimisticAccount.builder()
                .accountNumber("PES-001")
                .balance(10000L)
                .build());

            pessimisticRepo.save(PessimisticAccount.builder()
                .accountNumber("PES-002")
                .balance(5000L)
                .build());

            // 분산락 샘플 데이터
            distributedRepo.save(DistributedAccount.builder()
                .accountNumber("DIS-001")
                .balance(10000L)
                .build());

            distributedRepo.save(DistributedAccount.builder()
                .accountNumber("DIS-002")
                .balance(5000L)
                .build());

            System.out.println("========== 샘플 데이터 초기화 완료 ==========");
        };
    }

}
