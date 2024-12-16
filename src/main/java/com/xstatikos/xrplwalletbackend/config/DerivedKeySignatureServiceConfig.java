package com.xstatikos.xrplwalletbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xrpl.xrpl4j.crypto.ServerSecret;

@Configuration
public class DerivedKeySignatureServiceConfig {
	@Value("${app.derived-key-signature-service-secret}")
	private String customerSecret;

	@Value("${app.bank-fee-fund-derived-key-signature-service-secret}")
	private String bankFeeFundSecret;

	@Bean
	public ServerSecret customerSecret() {
		// Convert to bytes. Ensure serverSecretString is at least 32 bytes for best security.
		return ServerSecret.of( customerSecret.getBytes() );
	}

	@Bean
	public ServerSecret bankFeeFundSecret() {
		return ServerSecret.of( bankFeeFundSecret.getBytes() );
	}

}
