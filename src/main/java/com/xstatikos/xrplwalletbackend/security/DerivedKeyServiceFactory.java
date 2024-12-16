package com.xstatikos.xrplwalletbackend.security;

import org.springframework.stereotype.Component;
import org.xrpl.xrpl4j.codec.addresses.KeyType;
import org.xrpl.xrpl4j.crypto.ServerSecret;
import org.xrpl.xrpl4j.crypto.keys.PrivateKeyReference;
import org.xrpl.xrpl4j.crypto.keys.PublicKey;
import org.xrpl.xrpl4j.crypto.signing.SignatureService;
import org.xrpl.xrpl4j.crypto.signing.bc.BcDerivedKeySignatureService;

@Component
public class DerivedKeyServiceFactory {
	private final ServerSecret customerSecret;
	private final ServerSecret bankFeeFundSecret;


	public DerivedKeyServiceFactory( ServerSecret customerSecret, ServerSecret bankFeeFundSecret ) {
		this.customerSecret = customerSecret;
		this.bankFeeFundSecret = bankFeeFundSecret;
	}

	public SignatureService<PrivateKeyReference> derivedKeysSignatureServiceForCustomer() {
		return new BcDerivedKeySignatureService( () -> customerSecret );
	}

	// derive public key based on server secret and wallet identifer
	public PublicKey derivePublicKeyForCustomer( String walletIdentifier ) {
		PrivateKeyReference ref = createReference( walletIdentifier );
		return derivedKeysSignatureServiceForCustomer().derivePublicKey( ref );
	}

	public SignatureService<PrivateKeyReference> derivedKeysSignatureServiceForBankFeeFund() {
		return new BcDerivedKeySignatureService( () -> bankFeeFundSecret );
	}

	public PublicKey derivePublicKeyForBankFeeFund( String walletIdentifier ) {
		PrivateKeyReference ref = createReference( walletIdentifier );
		return derivedKeysSignatureServiceForBankFeeFund().derivePublicKey( ref );
	}

	// derive private key reference based on the server secret and wallet identifier
	public PrivateKeyReference createReference( String walletIdentifier ) {
		return new PrivateKeyReference() {
			@Override
			public String keyIdentifier() {
				return walletIdentifier;
			}

			@Override
			public KeyType keyType() {
				return KeyType.ED25519;
			}
		};
	}

}
