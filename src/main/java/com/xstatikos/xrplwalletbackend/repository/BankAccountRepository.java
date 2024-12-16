package com.xstatikos.xrplwalletbackend.repository;

import com.xstatikos.xrplwalletbackend.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
}