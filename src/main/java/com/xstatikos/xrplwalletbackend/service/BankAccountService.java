package com.xstatikos.xrplwalletbackend.service;

import com.xstatikos.xrplwalletbackend.dto.BankAccountRequest;
import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;

public interface BankAccountService {
	BankAccountResource createNewBankAccount( Long userId, BankAccountRequest bankAccountRequest ) throws Exception;

}
