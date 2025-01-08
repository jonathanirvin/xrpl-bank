package com.xstatikos.xrplwalletbackend.config;

import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xrpl.xrpl4j.client.XrplClient;

@Configuration
public class XrplConfig {
	@Value("${xrpl.rippled-url}")
	private String xrplRippledUrl;

	@Bean
	public XrplClient xrplClient() {
		HttpUrl rippledHttpUrl = HttpUrl.get( xrplRippledUrl );
		return new XrplClient( rippledHttpUrl );
	}

}
