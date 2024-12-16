package com.xstatikos.xrplwalletbackend.service.impl;

import com.xstatikos.xrplwalletbackend.dto.BankAccountRequest;
import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;
import com.xstatikos.xrplwalletbackend.model.BankAccount;
import com.xstatikos.xrplwalletbackend.repository.BankAccountRepository;
import com.xstatikos.xrplwalletbackend.security.DerivedKeyServiceFactory;
import com.xstatikos.xrplwalletbackend.service.BankAccountService;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.client.faucet.FaucetClient;
import org.xrpl.xrpl4j.client.faucet.FundAccountRequest;
import org.xrpl.xrpl4j.crypto.keys.PrivateKeyReference;
import org.xrpl.xrpl4j.crypto.keys.PublicKey;
import org.xrpl.xrpl4j.crypto.signing.SingleSignedTransaction;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.Payment;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

import java.time.Instant;
import java.util.UUID;

@Service
public class BankAccountServiceImpl implements BankAccountService {

	@Value("${app.bank-fee-fund-wallet-identifier}")
	private String bankFeeFundWalletIdentifier;

	private static final boolean RIPPLE_LIVE = false;

	private final String RIPPLED_URL;
	private final String FAUCET_URL;

	private final BankAccountRepository bankAccountRepository;
	private final DerivedKeyServiceFactory derivedKeyServiceFactory;
	private final XrplClient xrplClient;

	public BankAccountServiceImpl( BankAccountRepository bankAccountRepository, DerivedKeyServiceFactory derivedKeyServiceFactory ) {
		this.derivedKeyServiceFactory = derivedKeyServiceFactory;
		this.bankAccountRepository = bankAccountRepository;

		if ( RIPPLE_LIVE ) {
			// RIPPLED_URL = "https://s2.ripple.com:51234/"; // live rippled
			// RIPPLED_URL = "http://localhost:5005/"; // live self-hosted rippled
		} else {
			this.RIPPLED_URL = "https://s.altnet.rippletest.net:51234/"; // Testnet
			this.FAUCET_URL = "https://faucet.altnet.rippletest.net"; // Testnet
		}

		HttpUrl rippledHttpUrl = HttpUrl.get( RIPPLED_URL );
		xrplClient = new XrplClient( rippledHttpUrl );
	}

	public BankAccountResource createNewBankAccount( Long userId, BankAccountRequest bankAccountRequest ) throws Exception {
		String walletIdentifier = "user-" + userId + "-" + UUID.randomUUID();

		// derive the public key from the wallet identifier
		PublicKey publicKey = derivedKeyServiceFactory.derivePublicKeyForCustomer( walletIdentifier );
		Address customerAddress = publicKey.deriveAddress();

		// Assuming here that the bank will pay the reserve amount right now as part of the account creation
		if ( !RIPPLE_LIVE ) {
			FaucetClient faucetClient = FaucetClient.construct( HttpUrl.get( FAUCET_URL ) );
			faucetClient.fundAccount( FundAccountRequest.of( customerAddress ) );
			System.out.println( "Funded the account using the testnet faucet" );
		} else {
			fundNewCustomerAccountWithBankFeeFundForReserve( customerAddress );
		}

		BankAccount bankAccount = new BankAccount();
		bankAccount.setXrplAddress( customerAddress );
		bankAccount.setWalletIdentifier( walletIdentifier );
		bankAccount.setAccountType( "CHECKING" );
		bankAccount.setUserId( userId );
		bankAccount.setCreated_at( Instant.now() );
		bankAccount = bankAccountRepository.save( bankAccount );

		BankAccountResource bankAccountResource = new BankAccountResource();
		bankAccountResource.setId( bankAccount.getId() );
		bankAccountResource.setAccountType( bankAccount.getAccountType() );
		bankAccountResource.setXrplAddress( bankAccount.getXrplAddress() );
		return bankAccountResource;
	}

	private void fundNewCustomerAccountWithBankFeeFundForReserve( Address destinationAddress ) throws Exception {
		try {
			Address bankFeeFundAddress = derivedKeyServiceFactory.derivePublicKeyForBankFeeFund( bankFeeFundWalletIdentifier ).deriveAddress();

			Payment payment = Payment.builder()
					.account( bankFeeFundAddress )
					.destination( destinationAddress )
					.amount( XrpCurrencyAmount.ofDrops( 1000000 ) )
					.fee( XrpCurrencyAmount.ofDrops( 10 ) )
					// .sequence(  )
					.signingPublicKey( derivedKeyServiceFactory.derivePublicKeyForBankFeeFund( bankFeeFundWalletIdentifier ) )
					.build();

			// Sign the transaction
			PrivateKeyReference bankFeeFundPrivateKeyReference = derivedKeyServiceFactory.createReference( bankFeeFundWalletIdentifier );
			SingleSignedTransaction<Payment> signedPayment = derivedKeyServiceFactory.derivedKeysSignatureServiceForBankFeeFund().sign( bankFeeFundPrivateKeyReference, payment );

			// Submit the transaction
			SubmitResult<Payment> result = xrplClient.submit( signedPayment );
		} catch ( Exception e ) {
			throw new RuntimeException( "There was a problem funding the account with the reserve amount" );
		}

	}


}
