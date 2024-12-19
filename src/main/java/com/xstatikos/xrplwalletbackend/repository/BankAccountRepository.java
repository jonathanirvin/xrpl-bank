package com.xstatikos.xrplwalletbackend.repository;

import com.xstatikos.xrplwalletbackend.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
	List<BankAccount> findByUserId( Long userId );

}