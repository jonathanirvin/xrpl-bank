package com.xstatikos.xrplwalletbackend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.UnsignedInteger;
import com.xstatikos.xrplwalletbackend.dto.BankAccountRequest;
import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;
import com.xstatikos.xrplwalletbackend.dto.DepositRequest;
import com.xstatikos.xrplwalletbackend.dto.TransactionRequest;
import com.xstatikos.xrplwalletbackend.model.BankAccount;
import com.xstatikos.xrplwalletbackend.repository.BankAccountRepository;
import com.xstatikos.xrplwalletbackend.service.BankAccountService;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.client.faucet.FaucetClient;
import org.xrpl.xrpl4j.client.faucet.FundAccountRequest;
import org.xrpl.xrpl4j.codec.addresses.AddressCodec;
import org.xrpl.xrpl4j.codec.addresses.KeyType;
import org.xrpl.xrpl4j.crypto.ServerSecret;
import org.xrpl.xrpl4j.crypto.keys.PrivateKeyReference;
import org.xrpl.xrpl4j.crypto.keys.PublicKey;
import org.xrpl.xrpl4j.crypto.signing.SignatureService;
import org.xrpl.xrpl4j.crypto.signing.SingleSignedTransaction;
import org.xrpl.xrpl4j.crypto.signing.bc.BcDerivedKeySignatureService;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountLinesResult;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.common.LedgerSpecifier;
import org.xrpl.xrpl4j.model.client.fees.FeeResult;
import org.xrpl.xrpl4j.model.client.ledger.LedgerRequestParams;
import org.xrpl.xrpl4j.model.client.transactions.TransactionRequestParams;
import org.xrpl.xrpl4j.model.client.transactions.TransactionResult;
import org.xrpl.xrpl4j.model.immutables.FluentCompareTo;
import org.xrpl.xrpl4j.model.ledger.AccountRootObject;
import org.xrpl.xrpl4j.model.transactions.AccountSet;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.IssuedCurrencyAmount;
import org.xrpl.xrpl4j.model.transactions.OfferCreate;
import org.xrpl.xrpl4j.model.transactions.Payment;
import org.xrpl.xrpl4j.model.transactions.TrustSet;
import org.xrpl.xrpl4j.model.transactions.XAddress;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class BankAccountServiceImpl implements BankAccountService {

	@Value("${app.bank-fee-fund-wallet-identifier}")
	private String bankFeeFundWalletIdentifier;

	private static final boolean RIPPLE_LIVE = false;
	private static final boolean USE_FAUCET_FOR_BANK_FEE_FUND_WALLET = false;

	private final String RIPPLED_URL;
	private final String FAUCET_URL;
	private final Address RLUSD_ISSUER_ADDRESS;
	private final String STABLECOIN_CURRENCY_CODE = "XBANKCOIN";

	private final BankAccountRepository bankAccountRepository;
	private final XrplClient xrplClient;

	private final ServerSecret customerSecret;
	private final ServerSecret bankFeeFundSecret;

	public BankAccountServiceImpl( BankAccountRepository bankAccountRepository, ServerSecret customerSecret, ServerSecret bankFeeFundSecret ) {
		this.bankAccountRepository = bankAccountRepository;
		this.customerSecret = customerSecret;
		this.bankFeeFundSecret = bankFeeFundSecret;

		if ( RIPPLE_LIVE ) {
			// RIPPLED_URL = "https://s2.ripple.com:51234/"; // live rippled
			// RIPPLED_URL = "http://localhost:5005/"; // live self-hosted rippled
			// this.RLUSD_ISSUER_ADDRESS =Address.of( "rMxCKbEDwqr76QuheSUMdEGf4B9xJ8m5De" );
		} else {
			this.RIPPLED_URL = "https://s.altnet.rippletest.net:51234/"; // Testnet
			this.FAUCET_URL = "https://faucet.altnet.rippletest.net"; // Testnet
			this.RLUSD_ISSUER_ADDRESS = Address.of( "rQhWct2fv4Vc4KRjRgMrxa8xPN9Zx9iLKV" );
		}

		HttpUrl rippledHttpUrl = HttpUrl.get( RIPPLED_URL );
		xrplClient = new XrplClient( rippledHttpUrl );
	}

	@Override
	public BankAccountResource deposit( Long userId, DepositRequest depositRequest ) throws Exception {

		BankAccount bankAccount = bankAccountRepository.findByUserId( userId )
				.stream()
				.findFirst()
				.orElseThrow( () -> new RuntimeException( "No bank account found for user" ) );

		// derive the public key from the wallet identifier
		SignatureService<PrivateKeyReference> derivedKeySignatureService = new BcDerivedKeySignatureService( () -> customerSecret );

		PrivateKeyReference privateKeyReference = getPrivateKeyReference( bankAccount.getWalletIdentifier() );
		PublicKey publicKey = derivedKeySignatureService.derivePublicKey( privateKeyReference );
		Address customerClassicAddress = publicKey.deriveAddress();
		
		// Todo: Submit payment through Stripe, then issue bank's stablecoin
		setColdWalletSettings( xrplClient );
		addTrustLineToBankStablecoin( derivedKeySignatureService, privateKeyReference, bankAccount.getClassicAddress(), publicKey );
		depositBankStablecoinFromBankFeeFund( customerClassicAddress, depositRequest.getAmountToDeposit() );

		bankAccount.setBalance( depositRequest.getAmountToDeposit() );
		return bankAccountRepository.save( bankAccount ).toResource();

	}


	@Override
	public BankAccountResource createNewBankAccount( Long userId, BankAccountRequest bankAccountRequest ) throws Exception {
		String walletIdentifier = "user-" + userId + "-" + UUID.randomUUID();

		// derive the public key from the wallet identifier
		SignatureService<PrivateKeyReference> derivedKeySignatureService = new BcDerivedKeySignatureService( () -> customerSecret );

		PrivateKeyReference privateKeyReference = getPrivateKeyReference( walletIdentifier );
		PublicKey publicKey = derivedKeySignatureService.derivePublicKey( privateKeyReference );
		Address customerClassicAddress = publicKey.deriveAddress();
		XAddress customerXAddress = AddressCodec.getInstance().classicAddressToXAddress( customerClassicAddress, RIPPLE_LIVE );


		// Assuming here that the bank will pay the reserve amount right now as part of the account creation
		fundNewCustomerAccountWithBankFeeFundForReserve( customerClassicAddress );

		BankAccount bankAccount = new BankAccount();
		bankAccount.setClassicAddress( customerClassicAddress );
		bankAccount.setXAddress( customerXAddress );
		bankAccount.setWalletIdentifier( walletIdentifier );
		bankAccount.setAccountType( bankAccountRequest.getAccountType() );
		bankAccount.setUserId( userId );
		bankAccount.setBalance( BigDecimal.ZERO );

		return bankAccountRepository.save( bankAccount ).toResource();
	}

	private void addTrustLineToBankStablecoin( SignatureService<PrivateKeyReference> derivedKeySignatureService, PrivateKeyReference privateKeyReference, Address customerClassicAddress, PublicKey customerPublicKey ) throws JsonRpcClientErrorException {
		XrpCurrencyAmount openLedgerFee = getXrpLedgerFee();

		AccountRootObject accountRootObject = getAccountRootObject( customerClassicAddress );
		UnsignedInteger sequence = accountRootObject.sequence();

		UnsignedInteger lastLedgerSequenceBeforeTransactionExpires = getLastLedgerSequenceBeforeTransactionExpires();


		SignatureService<PrivateKeyReference> derivedBankKeySignatureService = new BcDerivedKeySignatureService( () -> bankFeeFundSecret );
		PrivateKeyReference bankPrivateKeyReference = getPrivateKeyReference( bankFeeFundWalletIdentifier );

		PublicKey publicKey = derivedBankKeySignatureService.derivePublicKey( bankPrivateKeyReference );
		Address bankAddress = publicKey.deriveAddress();

		TrustSet trustSet = TrustSet.builder()
				.account( customerClassicAddress )
				.fee( openLedgerFee )
				.signingPublicKey( customerPublicKey )
				.sequence( sequence )
				.lastLedgerSequence( lastLedgerSequenceBeforeTransactionExpires )
				.limitAmount(
						IssuedCurrencyAmount.builder()
								.currency( convertCurrencyCodeToHex( this.STABLECOIN_CURRENCY_CODE ) )
								.issuer( bankAddress )
								.value( "1000000" )
								.build() )
				.build();

		SingleSignedTransaction<TrustSet> signedTrustSet = derivedKeySignatureService.sign( privateKeyReference, trustSet );
		System.out.println( "Signed Trustset: " + signedTrustSet.signedTransaction() );

		try {
			submitAndWaitForValidation( signedTrustSet, xrplClient, TrustSet.class );
		} catch ( Exception e ) {
			throw new RuntimeException( "There was an issue with submitting and validating the transaction: " + e );
		}

	}

	@Override
	public BankAccountResource transferXrpToXrpAddress( Long userId, TransactionRequest transactionRequest ) throws Exception {

		// For now let's just get the first one
		// Todo: pick a specific account by type or id
		BankAccount bankAccount = bankAccountRepository.findByUserId( userId )
				.stream()
				.findFirst()
				.orElseThrow( () -> new RuntimeException( "No bank account found for user" ) );

		XrpCurrencyAmount amountOfXrpToSend = transactionRequest.getXrpCurrencyAmount();
		sendXrpFromCustomerAddress( transactionRequest.getDestinationAddress(), bankAccount.getWalletIdentifier(), amountOfXrpToSend );

		return bankAccount.toResource();
	}

	private void fundNewCustomerAccountWithBankFeeFundForReserve( Address destinationAddress ) throws Exception {
		try {

			SignatureService<PrivateKeyReference> derivedKeySignatureService = new BcDerivedKeySignatureService( () -> bankFeeFundSecret );
			PrivateKeyReference privateKeyReference = getPrivateKeyReference( bankFeeFundWalletIdentifier );
			// TODO: update this to only be the 1 XRP reserve + the trust line .2 (but pretty sure the .2 isn't neccassary as the first two trust lines are free
			transferXrp( destinationAddress, derivedKeySignatureService, privateKeyReference, XrpCurrencyAmount.ofXrp( BigDecimal.TEN.add( BigDecimal.TEN ) ) );

		} catch ( Exception e ) {
			throw new RuntimeException( "There was a problem funding the account with the reserve amount" + e );
		}

	}

	private void depositBankStablecoinFromBankFeeFund( Address destinationAddress, BigDecimal amountToDeposit ) throws Exception {
		try {

			SignatureService<PrivateKeyReference> derivedKeySignatureService = new BcDerivedKeySignatureService( () -> bankFeeFundSecret );
			PrivateKeyReference privateKeyReference = getPrivateKeyReference( bankFeeFundWalletIdentifier );

			transferBankStablecoin( destinationAddress, derivedKeySignatureService, privateKeyReference, amountToDeposit );

		} catch ( Exception e ) {
			throw new RuntimeException( "There was a problem funding the account with Bank's Stablecoin" + e );
		}

	}

	private void sendXrpFromCustomerAddress( Address destinationAddress, String walletIdentifier, XrpCurrencyAmount amountOfXrpToSend ) throws Exception {
		try {
			SignatureService<PrivateKeyReference> derivedKeySignatureService = new BcDerivedKeySignatureService( () -> customerSecret );
			PrivateKeyReference privateKeyReference = getPrivateKeyReference( walletIdentifier );
			transferXrp( destinationAddress, derivedKeySignatureService, privateKeyReference, amountOfXrpToSend );

		} catch ( Exception e ) {
			throw new RuntimeException( "There was a problem submitting the payment" + e );
		}

	}

	private void transferXrp( Address destinationAddress, SignatureService<PrivateKeyReference> derivedKeySignatureService, PrivateKeyReference privateKeyReference, XrpCurrencyAmount amountOfXrpToSend ) throws JsonRpcClientErrorException, JsonProcessingException {

		// derive the public key from the wallet identifier
		PublicKey publicKey = derivedKeySignatureService.derivePublicKey( privateKeyReference );
		Address sourceAddress = publicKey.deriveAddress();


		if ( !RIPPLE_LIVE && USE_FAUCET_FOR_BANK_FEE_FUND_WALLET ) {
			FaucetClient faucetClient = FaucetClient.construct( HttpUrl.get( FAUCET_URL ) );
			faucetClient.fundAccount( FundAccountRequest.of( sourceAddress ) );
			System.out.println( "Funded the account using the testnet faucet for address: " + sourceAddress );
		}

		AccountRootObject accountRootObject = getAccountRootObject( sourceAddress );
		UnsignedInteger sequence = accountRootObject.sequence();
		System.out.println( "Source Address Balance Before Transaction: " + accountRootObject.balance() );

		XrpCurrencyAmount openLedgerFee = getXrpLedgerFee();

		UnsignedInteger lastLedgerSequenceBeforeTransactionExpires = getLastLedgerSequenceBeforeTransactionExpires();

		Payment payment = Payment.builder()
				.account( sourceAddress )
				.destination( destinationAddress )
				.amount( amountOfXrpToSend )
				.fee( openLedgerFee )
				.sequence( sequence )
				.lastLedgerSequence( lastLedgerSequenceBeforeTransactionExpires )
				.signingPublicKey( publicKey )
				.build();

		// Sign the transaction
		SingleSignedTransaction<Payment> signedPayment = derivedKeySignatureService.sign( privateKeyReference, payment );
		System.out.println( "Signed Payment: " + signedPayment.signedTransaction() );

		try {
			submitAndWaitForValidation( signedPayment, xrplClient, Payment.class );
		} catch ( Exception e ) {
			throw new RuntimeException( "There was an issue with submitting and validating the transaction: " + e );
		}

		accountRootObject = getAccountRootObject( sourceAddress );
		System.out.println( "Source Address Balance After Transaction: " + accountRootObject.balance() );
	}

	// Todo: not used or tested yet
	private void swapXrpForBankStablecoin( Address address, SignatureService<PrivateKeyReference> derivedKeySignatureService, PrivateKeyReference privateKeyReference, XrpCurrencyAmount amountOfXrpToSend ) throws JsonRpcClientErrorException, JsonProcessingException {

		// this is just going to swap xrp for the Bank Stablecoin in the bank fund fee account right now
		// sourceAddress is the bank

		// derive the public key from the wallet identifier
		PublicKey publicKey = derivedKeySignatureService.derivePublicKey( privateKeyReference );
		Address sourceAddress = publicKey.deriveAddress();

		try {
			AccountRootObject accountRootObject = getAccountRootObject( sourceAddress );
			UnsignedInteger sequence = accountRootObject.sequence();

			UnsignedInteger lastLedgerSequenceBeforeTransactionExpires = getLastLedgerSequenceBeforeTransactionExpires();

			XrpCurrencyAmount takerGets = XrpCurrencyAmount.ofDrops( 100000000 ); // 100 XRP
			IssuedCurrencyAmount takerPays = IssuedCurrencyAmount.builder()
					.currency( convertCurrencyCodeToHex( this.STABLECOIN_CURRENCY_CODE ) )
					.issuer( RLUSD_ISSUER_ADDRESS )
					.value( "233" )
					.build();

			OfferCreate offerCreate = OfferCreate.builder()
					.account( sourceAddress )
					.takerGets( takerGets )
					.takerPays( takerPays )
					.fee( XrpCurrencyAmount.ofDrops( 10 ) )
					.sequence( sequence )
					.lastLedgerSequence( lastLedgerSequenceBeforeTransactionExpires )
					.signingPublicKey( publicKey )
					.build();

			SingleSignedTransaction<OfferCreate> signedOfferCreate = derivedKeySignatureService.sign( privateKeyReference, offerCreate );

			try {
				submitAndWaitForValidation( signedOfferCreate, xrplClient, OfferCreate.class );
			} catch ( Exception e ) {
				throw new RuntimeException( "There was an issue with submitting and validating the transaction: " + e );
			}

			accountRootObject = getAccountRootObject( sourceAddress );
			System.out.println( "Source Address Balance After Transaction (Bank Stablecoin Transfer): " + accountRootObject.balance() );

			// Fetch trust lines
			AccountLinesRequestParams requestParams = AccountLinesRequestParams.builder()
					.account( sourceAddress )
					.ledgerSpecifier( LedgerSpecifier.VALIDATED )
					.build();

			AccountLinesResult result2 = xrplClient.accountLines( requestParams );

			System.out.println( "Account lines result: " + result2 );

			// Display the trust lines and balances
			result2.lines().forEach( trustLine -> {
				System.out.println( "Currency: " + trustLine.currency() );
				System.out.println( "Issuer: " + trustLine.account() );
				System.out.println( "Balance: " + trustLine.balance() );
				System.out.println( "Limit: " + trustLine.limit() );
				System.out.println( "----------------------------" );
			} );

			// Fetch trust lines
			AccountLinesRequestParams requestParams2 = AccountLinesRequestParams.builder()
					.account( address )
					.ledgerSpecifier( LedgerSpecifier.VALIDATED )
					.build();

			AccountLinesResult result3 = xrplClient.accountLines( requestParams2 );

			System.out.println( "Account lines result: " + result3 );

			// Display the trust lines and balances
			result3.lines().forEach( trustLine -> {
				System.out.println( "Currency: " + trustLine.currency() );
				System.out.println( "Issuer: " + trustLine.account() );
				System.out.println( "Balance: " + trustLine.balance() );
				System.out.println( "Limit: " + trustLine.limit() );
				System.out.println( "----------------------------" );
			} );

		} catch ( Exception e ) {
			System.out.println( "error with the swap: " + e );
		}

	}

	private void transferBankStablecoin( Address destinationAddress, SignatureService<PrivateKeyReference> derivedKeySignatureService, PrivateKeyReference privateKeyReference, BigDecimal amountToDeposit ) throws JsonRpcClientErrorException, JsonProcessingException {

		// derive the public key from the wallet identifier
		PublicKey publicKey = derivedKeySignatureService.derivePublicKey( privateKeyReference );
		Address sourceAddress = publicKey.deriveAddress();

		AccountRootObject accountRootObject = getAccountRootObject( sourceAddress );
		UnsignedInteger sequence = accountRootObject.sequence();
		System.out.println( "Source Address Balance Before Transaction (Bank Stablecoin transfer): " + accountRootObject.balance() );

		XrpCurrencyAmount openLedgerFee = getXrpLedgerFee();

		UnsignedInteger lastLedgerSequenceBeforeTransactionExpires = getLastLedgerSequenceBeforeTransactionExpires();

		Payment payment = Payment.builder()
				.account( sourceAddress )
				.destination( destinationAddress )
				.amount( IssuedCurrencyAmount.builder()
						.currency( convertCurrencyCodeToHex( this.STABLECOIN_CURRENCY_CODE ) )
						.issuer( sourceAddress )
						.value( amountToDeposit.toString() )
						.build() )
				.fee( openLedgerFee )
				.sequence( sequence )
				.lastLedgerSequence( lastLedgerSequenceBeforeTransactionExpires )
				.signingPublicKey( publicKey )
				// .sendMax( XrpCurrencyAmount.ofXrp( BigDecimal.valueOf( 1000000 ) ) )
				.build();

		// Sign the transaction
		SingleSignedTransaction<Payment> signedPayment = derivedKeySignatureService.sign( privateKeyReference, payment );
		System.out.println( "Signed Payment: " + signedPayment.signedTransaction() );

		try {
			submitAndWaitForValidation( signedPayment, xrplClient, Payment.class );
		} catch ( Exception e ) {
			throw new RuntimeException( "There was an issue with submitting and validating the transaction: " + e );
		}

		accountRootObject = getAccountRootObject( sourceAddress );
		System.out.println( "Source Address Balance After Transaction (Bank Stablecoin Transfer): " + accountRootObject.balance() );

		// Fetch trust lines
		AccountLinesRequestParams requestParams = AccountLinesRequestParams.builder()
				.account( sourceAddress )
				.ledgerSpecifier( LedgerSpecifier.VALIDATED )
				.build();

		AccountLinesResult result2 = xrplClient.accountLines( requestParams );

		System.out.println( "result2: " + result2.toString() );

		// Display the trust lines and balances
		result2.lines().forEach( trustLine -> {
			System.out.println( "Currency: " + trustLine.currency() );
			System.out.println( "Issuer: " + trustLine.account() );
			System.out.println( "Balance: " + trustLine.balance() );
			System.out.println( "Limit: " + trustLine.limit() );
			System.out.println( "----------------------------" );
		} );

	}

	private void setColdWalletSettings( XrplClient xrplClient ) {

		try {
			SignatureService<PrivateKeyReference> derivedBankKeySignatureService = new BcDerivedKeySignatureService( () -> bankFeeFundSecret );
			PrivateKeyReference bankPrivateKeyReference = getPrivateKeyReference( bankFeeFundWalletIdentifier );

			PublicKey publicKey = derivedBankKeySignatureService.derivePublicKey( bankPrivateKeyReference );
			Address bankAddress = publicKey.deriveAddress();

			UnsignedInteger lastLedgerSequenceBeforeTransactionExpires = getLastLedgerSequenceBeforeTransactionExpires();

			UnsignedInteger coldWalletSequence = xrplClient.accountInfo(
					AccountInfoRequestParams.builder()
							.ledgerSpecifier( LedgerSpecifier.CURRENT )
							.account( bankAddress )
							.build()
			).accountData().sequence();

			AccountSet setDefaultRipple = AccountSet.builder()
					.account( bankAddress )
					.fee( getXrpLedgerFee() )
					.sequence( coldWalletSequence )
					.signingPublicKey( publicKey )
					.setFlag( AccountSet.AccountSetFlag.DEFAULT_RIPPLE )
					.lastLedgerSequence( lastLedgerSequenceBeforeTransactionExpires )
					.build();

			// Sign the transaction
			SingleSignedTransaction<AccountSet> signedAccountSet = derivedBankKeySignatureService.sign( bankPrivateKeyReference, setDefaultRipple );
			System.out.println( "Signed Payment: " + signedAccountSet.signedTransaction() );

			try {
				submitAndWaitForValidation( signedAccountSet, xrplClient, Payment.class );
			} catch ( Exception e ) {
				throw new RuntimeException( "There was an issue with submitting and validating the transaction: " + e );
			}

		} catch ( Exception e ) {
			throw new RuntimeException( "There was an issue with setting up the cold wallet account settings: " + e );
		}

	}

	// TODO: pick back up with T and transactionClass below
	private static <T> void submitAndWaitForValidation( SingleSignedTransaction<?> signedTransaction, XrplClient xrplClient, Class<T> transactionClass )
			throws InterruptedException, JsonRpcClientErrorException, JsonProcessingException {

		xrplClient.submit( signedTransaction );

		boolean transactionValidated = false;
		boolean transactionExpired = false;
		while ( !transactionValidated && !transactionExpired ) {
			Thread.sleep( 1000 );
			LedgerIndex latestValidatedLedgerIndex = xrplClient.ledger(
							LedgerRequestParams.builder().ledgerSpecifier( LedgerSpecifier.VALIDATED ).build()
					)
					.ledgerIndex()
					.orElseThrow( () ->
							new RuntimeException( "Ledger response did not contain a LedgerIndex." )
					);

			TransactionResult<Payment> transactionResult = xrplClient.transaction(
					TransactionRequestParams.of( signedTransaction.hash() ),
					Payment.class
			);

			if ( transactionResult.validated() ) {
				System.out.println( "Transaction was validated with result code " +
						transactionResult.metadata().get().transactionResult() );
				transactionValidated = true;
			} else {

				boolean lastLedgerSequenceHasPassed = signedTransaction.signedTransaction().lastLedgerSequence()
						.map( ( signedTransactionLastLedgerSeq ) ->
								FluentCompareTo.is( latestValidatedLedgerIndex.unsignedIntegerValue() )
										.greaterThan( signedTransactionLastLedgerSeq )
						)
						.orElse( false );

				if ( lastLedgerSequenceHasPassed ) {
					System.out.println( "LastLedgerSequence has passed. Last tx response: " +
							transactionResult );
					transactionExpired = true;
				} else {
					System.out.println( "Transaction not yet validated." );
				}
			}
		}
	}

	private String convertCurrencyCodeToHex( String currencyCode ) {
		StringBuilder hexBuilder = new StringBuilder();

		// Convert currency code to hexidecimal
		for ( char c : currencyCode.toCharArray() ) {
			hexBuilder.append( String.format( "%02X", ( int ) c ) );
		}

		// Pad with zeros to make it 20 bytes (40 hex characters)
		while ( hexBuilder.length() < 40 ) {
			hexBuilder.append( "00" );
		}

		return hexBuilder.toString();
	}

	private UnsignedInteger getLastLedgerSequenceBeforeTransactionExpires() throws JsonRpcClientErrorException {
		// get the latest validated ledger index
		LedgerIndex validatedLedgerIndex = xrplClient.ledger(
				LedgerRequestParams.builder()
						.ledgerSpecifier( LedgerSpecifier.VALIDATED )
						.build()
		).ledgerIndex().orElseThrow( () -> new RuntimeException( "LedgerIndex not available" ) );

		// current ledger index + 4 
		// 4 is a buffer to ensure that the transaction has enough time to be included in a validated ledger before it expires
		// If the sequence gets passed 4, the transaction will expire
		// This is to prevent an infinite pending state
		// This gives a buffer of 16 seconds (4 ledgers) to validate (with 4 seconds per ledger) 
		UnsignedInteger lastLedgerSequenceBeforeTransactionExpires = validatedLedgerIndex.plus( UnsignedInteger.valueOf( 4 ) ).unsignedIntegerValue();
		return lastLedgerSequenceBeforeTransactionExpires;
	}

	private XrpCurrencyAmount getXrpLedgerFee() throws JsonRpcClientErrorException {
		// Request current fee info from rippled
		FeeResult feeResult = xrplClient.fee();
		XrpCurrencyAmount openLedgerFee = feeResult.drops().openLedgerFee();
		return openLedgerFee;
	}

	private AccountRootObject getAccountRootObject( Address address ) throws JsonRpcClientErrorException {
		AccountInfoRequestParams accountInfoRequestParams = AccountInfoRequestParams.builder()
				.account( address )
				.ledgerSpecifier( LedgerSpecifier.VALIDATED ).build();

		AccountRootObject accountRootObject = xrplClient.accountInfo( accountInfoRequestParams ).accountData();
		return accountRootObject;
	}

	@NotNull
	private PrivateKeyReference getPrivateKeyReference( String walletIdentifier ) {
		PrivateKeyReference privateKeyReference = new PrivateKeyReference() {
			@Override
			public KeyType keyType() {
				return KeyType.ED25519;
			}

			@Override
			public String keyIdentifier() {
				return walletIdentifier;
			}
		};
		return privateKeyReference;
	}

	// Since results are tentative, we'll need a polling mechanism in place to grab transaction results as they're finalized    
	private boolean waitForTransactionValidation( Address senderAddress, SingleSignedTransaction<Payment> signedPayment, UnsignedInteger lastLedgerSequenceBeforeTransactionExpires ) {
		TransactionResult<Payment> transactionResult = null;

		boolean transactionValidated = false;
		boolean transactionExpired = false;
		while ( !transactionValidated && !transactionExpired ) {
			try {
				Thread.sleep( 4000 );
				LedgerIndex latestValidatedLedgerIndex = xrplClient.ledger(
						LedgerRequestParams.builder()
								.ledgerSpecifier( LedgerSpecifier.VALIDATED )
								.build()
				).ledgerIndex().orElseThrow( () -> new RuntimeException( "The ledger response did not contain a LedgerIndex" ) );

				transactionResult = xrplClient.transaction( TransactionRequestParams.of( signedPayment.hash() ), Payment.class );

				if ( transactionResult.validated() ) {
					System.out.println( "Payment was validated with result code: " + transactionResult.metadata().get().transactionResult() );

					System.out.println( "Full Transaction Result: " + transactionResult.toString() );

					AccountInfoRequestParams accountInfoRequestParams = AccountInfoRequestParams.builder()
							.account( senderAddress )
							.ledgerSpecifier( LedgerSpecifier.VALIDATED ).build();

					XrpCurrencyAmount senderBalance = xrplClient.accountInfo( accountInfoRequestParams ).accountData().balance();

					System.out.println( "Sender balance is now " + senderBalance );

					transactionValidated = true;
				} else {
					boolean lastLedgerSequenceHasPassed = FluentCompareTo.is( latestValidatedLedgerIndex.unsignedIntegerValue() )
							.greaterThan( UnsignedInteger.valueOf( lastLedgerSequenceBeforeTransactionExpires.intValue() ) );

					if ( lastLedgerSequenceHasPassed ) {
						System.out.println( "Last ledger sequence has passed. Assuming Expiration for now. Last transaction response: " + transactionResult );
						transactionExpired = true;
					} else {
						System.out.println( "Payment not yet validated" );
					}
				}
			} catch ( Exception e ) {
				System.out.println( "There was a problem validating the ledger: " + e );
			}


		}
		return true;
	}

}
