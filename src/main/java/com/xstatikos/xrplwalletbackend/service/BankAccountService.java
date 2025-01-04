package com.xstatikos.xrplwalletbackend.service;

import com.xstatikos.xrplwalletbackend.dto.BankAccountRequest;
import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;
import com.xstatikos.xrplwalletbackend.dto.DepositRequest;
import com.xstatikos.xrplwalletbackend.dto.TransactionRequest;

import java.math.BigDecimal;

public interface BankAccountService {
	BankAccountResource createNewBankAccount( Long userId, BankAccountRequest bankAccountRequest ) throws Exception;

	BankAccountResource transferXrpToXrpAddress( Long userId, TransactionRequest transactionRequest ) throws Exception;

	String depositStripe( Long userId, String username, DepositRequest depositRequest ) throws Exception;

	BankAccountResource depositStablecoin( Long userId, BigDecimal amountToDeposit ) throws Exception;

}
