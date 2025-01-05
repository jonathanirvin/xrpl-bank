package com.xstatikos.xrplwalletbackend.config;

import com.xstatikos.xrplwalletbackend.security.JwtAuthFilter;
import com.xstatikos.xrplwalletbackend.security.JwtService;
import com.xstatikos.xrplwalletbackend.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
	private final CustomUserDetailsService customUserDetailsService;
	private final JwtService jwtService;

	public SecurityConfig( CustomUserDetailsService customUserDetailsService, JwtService jwtService ) {
		this.customUserDetailsService = customUserDetailsService;
		this.jwtService = jwtService;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider daoAuthProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService( customUserDetailsService );
		provider.setPasswordEncoder( passwordEncoder() );
		return provider;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins( List.of( "http://localhost:8100" ) );
		configuration.setAllowedMethods( List.of( "GET", "POST", "PUT", "DELETE", "OPTIONS" ) );
		configuration.setAllowedHeaders( List.of( "*" ) );
		configuration.setAllowCredentials( true );

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration( "/**", configuration ); // Apply CORS settings to all endpoints
		return source;
	}

	@Bean
	public SecurityFilterChain filterChain( HttpSecurity http ) throws Exception {
		http
				.csrf( csrf -> csrf.disable() )
				.cors( cors -> cors.configurationSource( corsConfigurationSource() ) )
				.authorizeHttpRequests( auth -> auth
						.requestMatchers( "/auth/**" ).permitAll()
						.requestMatchers( "/bank-accounts/payment-succeeded-webhook" ).permitAll()

						.anyRequest().authenticated()
				)

				.addFilterBefore( new JwtAuthFilter( jwtService, customUserDetailsService ), UsernamePasswordAuthenticationFilter.class );

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager( HttpSecurity http ) throws Exception {
		AuthenticationManagerBuilder authBuilder = http.getSharedObject( AuthenticationManagerBuilder.class );
		authBuilder.authenticationProvider( daoAuthProvider() );
		return authBuilder.build();
	}

}
