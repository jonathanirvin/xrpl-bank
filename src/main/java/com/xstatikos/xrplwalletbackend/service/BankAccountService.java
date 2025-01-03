package com.xstatikos.xrplwalletbackend.service;

import com.xstatikos.xrplwalletbackend.dto.BankAccountRequest;
import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;
import com.xstatikos.xrplwalletbackend.dto.DepositRequest;
import com.xstatikos.xrplwalletbackend.dto.TransactionRequest;

public interface BankAccountService {
	BankAccountResource createNewBankAccount( Long userId, BankAccountRequest bankAccountRequest ) throws Exception;

	BankAccountResource transferXrpToXrpAddress( Long userId, TransactionRequest transactionRequest ) throws Exception;

	BankAccountResource deposit( Long userId, DepositRequest depositRequest ) throws Exception;

}
