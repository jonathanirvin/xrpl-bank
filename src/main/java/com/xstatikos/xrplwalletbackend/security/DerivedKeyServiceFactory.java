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
	private final ServerSecret serverSecret;

	public DerivedKeyServiceFactory( ServerSecret serverSecret ) {
		this.serverSecret = serverSecret;
	}

	public SignatureService<PrivateKeyReference> derivedKeysSignatureService() {
		return new BcDerivedKeySignatureService( () -> serverSecret );
	}

	// derive public key based on server secret and wallet identifer
	public PublicKey derivePublicKey( String walletIdentifier ) {
		PrivateKeyReference ref = createReference( walletIdentifier );
		return derivedKeysSignatureService().derivePublicKey( ref );
	}

	// derive private key reference based on the server secret and wallet identifier
	public PrivateKeyReference createReference( String walletIdentifier ) {
		return new PrivateKeyReference() {
			@Override
			public String keyIdentifier() {
				return keyIdentifier();
			}

			@Override
			public KeyType keyType() {
				return KeyType.ED25519;
			}
		};
	}

}
