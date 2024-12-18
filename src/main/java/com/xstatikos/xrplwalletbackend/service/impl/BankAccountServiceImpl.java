package com.xstatikos.xrplwalletbackend.service.impl;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.xstatikos.xrplwalletbackend.dto.BankAccountRequest;
import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;
import com.xstatikos.xrplwalletbackend.model.BankAccount;
import com.xstatikos.xrplwalletbackend.repository.BankAccountRepository;
import com.xstatikos.xrplwalletbackend.service.BankAccountService;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.common.LedgerSpecifier;
import org.xrpl.xrpl4j.model.client.fees.FeeResult;
import org.xrpl.xrpl4j.model.client.ledger.LedgerRequestParams;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.client.transactions.TransactionRequestParams;
import org.xrpl.xrpl4j.model.client.transactions.TransactionResult;
import org.xrpl.xrpl4j.model.immutables.FluentCompareTo;
import org.xrpl.xrpl4j.model.ledger.AccountRootObject;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.Payment;
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
		bankAccount.setBalance( UnsignedLong.ZERO );
		bankAccount = bankAccountRepository.save( bankAccount );

		BankAccountResource bankAccountResource = new BankAccountResource();
		bankAccountResource.setId( bankAccount.getId() );
		bankAccountResource.setAccountType( bankAccount.getAccountType() );
		bankAccountResource.setBalance( ( XrpCurrencyAmount.of( bankAccount.getBalance() ) ) );
		bankAccountResource.setClassicAddress( bankAccount.getClassicAddress() );
		bankAccountResource.setXAddress( bankAccount.getXAddress() );
		bankAccountResource.setCreatedAt( bankAccount.getCreatedAt() );
		bankAccountResource.setUpdatedAt( bankAccount.getUpdatedAt() );
		return bankAccountResource;
	}

	private void fundNewCustomerAccountWithBankFeeFundForReserve( Address destinationAddress ) throws Exception {
		try {

			SignatureService<PrivateKeyReference> derivedKeySignatureService = new BcDerivedKeySignatureService( () -> bankFeeFundSecret );

			PrivateKeyReference privateKeyReference = getPrivateKeyReference( bankFeeFundWalletIdentifier );

			// derive the public key from the wallet identifier
			PublicKey bankFeeFundPublicKey = derivedKeySignatureService.derivePublicKey( privateKeyReference );
			Address bankFeeFundClassicAddress = bankFeeFundPublicKey.deriveAddress();

			if ( !RIPPLE_LIVE && USE_FAUCET_FOR_BANK_FEE_FUND_WALLET ) {
				FaucetClient faucetClient = FaucetClient.construct( HttpUrl.get( FAUCET_URL ) );
				faucetClient.fundAccount( FundAccountRequest.of( bankFeeFundClassicAddress ) );
				System.out.println( "Funded the account using the testnet faucet for address: " + bankFeeFundClassicAddress );
			}

			AccountInfoRequestParams accountInfoRequestParams = AccountInfoRequestParams.builder()
					.account( bankFeeFundClassicAddress )
					.ledgerSpecifier( LedgerSpecifier.VALIDATED ).build();

			AccountRootObject accountRootObject = xrplClient.accountInfo( accountInfoRequestParams ).accountData();
			UnsignedInteger sequence = accountRootObject.sequence();

			System.out.println( "Bank Fee Fund Balance Before: " + accountRootObject.balance() );

			// Request current fee info from rippled
			FeeResult feeResult = xrplClient.fee();
			XrpCurrencyAmount openLedgerFee = feeResult.drops().openLedgerFee();

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

			Payment payment = Payment.builder()
					.account( bankFeeFundClassicAddress )
					.destination( destinationAddress )
					.amount( XrpCurrencyAmount.ofXrp( BigDecimal.TEN ) )
					.fee( openLedgerFee )
					.sequence( sequence )
					.lastLedgerSequence( lastLedgerSequenceBeforeTransactionExpires )
					.signingPublicKey( bankFeeFundPublicKey )
					.build();

			// Sign the transaction
			SingleSignedTransaction<Payment> signedPayment = derivedKeySignatureService.sign( privateKeyReference, payment );

			System.out.println( "Signed Payment: " + signedPayment.signedTransaction() );

			// Submit the transaction
			SubmitResult<Payment> result = xrplClient.submit( signedPayment );

			if ( !result.applied() ) {
				throw new RuntimeException( result.toString() );
			}

			// todo log all transactions in the database
			waitForTransactionValidation( bankFeeFundClassicAddress, signedPayment, lastLedgerSequenceBeforeTransactionExpires );

		} catch ( Exception e ) {
			throw new RuntimeException( "There was a problem funding the account with the reserve amount" + e );
		}

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
