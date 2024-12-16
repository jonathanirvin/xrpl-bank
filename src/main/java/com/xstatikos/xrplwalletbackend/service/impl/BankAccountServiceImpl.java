package com.xstatikos.xrplwalletbackend.service.impl;

import com.xstatikos.xrplwalletbackend.dto.BankAccountRequest;
import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;
import com.xstatikos.xrplwalletbackend.model.BankAccount;
import com.xstatikos.xrplwalletbackend.repository.BankAccountRepository;
import com.xstatikos.xrplwalletbackend.security.DerivedKeyServiceFactory;
import com.xstatikos.xrplwalletbackend.service.BankAccountService;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.crypto.keys.PublicKey;
import org.xrpl.xrpl4j.model.transactions.Address;

import java.time.Instant;
import java.util.UUID;

@Service
public class BankAccountServiceImpl implements BankAccountService {
	private final BankAccountRepository bankAccountRepository;
	private final DerivedKeyServiceFactory derivedKeyServiceFactory;

	public BankAccountServiceImpl( BankAccountRepository bankAccountRepository, DerivedKeyServiceFactory derivedKeyServiceFactory ) {
		this.derivedKeyServiceFactory = derivedKeyServiceFactory;
		this.bankAccountRepository = bankAccountRepository;
	}

	public BankAccountResource createNewBankAccount( BankAccountRequest bankAccountRequest ) {
		String walletIdentifier = "user-" + bankAccountRequest.getUserId() + "-" + UUID.randomUUID();

		// derive the public key from the wallet identifier
		PublicKey publicKey = derivedKeyServiceFactory.derivePublicKey( walletIdentifier );
		Address address = publicKey.deriveAddress();

		BankAccount bankAccount = new BankAccount();
		bankAccount.setXrplAddress( address );
		bankAccount.setWalletIdentifier( walletIdentifier );
		bankAccount.setAccountType( "CHECKING" );
		bankAccount.setUserId( bankAccountRequest.getUserId() );
		bankAccount.setCreated_at( Instant.now() );
		bankAccount = bankAccountRepository.save( bankAccount );

		BankAccountResource bankAccountResource = new BankAccountResource();
		bankAccountResource.setId( bankAccount.getId() );
		bankAccountResource.setAccountType( bankAccount.getAccountType() );
		bankAccountResource.setXrplAddress( bankAccount.getXrplAddress() );
		return bankAccountResource;
	}

}
