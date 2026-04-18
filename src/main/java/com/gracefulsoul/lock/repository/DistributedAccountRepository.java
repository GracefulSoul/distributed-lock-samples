package com.gracefulsoul.lock.repository;

import com.gracefulsoul.lock.domain.DistributedAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistributedAccountRepository extends JpaRepository<DistributedAccount, Long> {

    Optional<DistributedAccount> findByAccountNumber(String accountNumber);

}
