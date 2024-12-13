package com.xstatikos.xrplwalletbackend.service;

import com.xstatikos.xrplwalletbackend.dto.auth.AuthResponse;
import com.xstatikos.xrplwalletbackend.dto.auth.LoginRequest;
import com.xstatikos.xrplwalletbackend.dto.auth.RegisterRequest;
import com.xstatikos.xrplwalletbackend.model.UserProfile;
import com.xstatikos.xrplwalletbackend.repository.UserProfileRepository;
import com.xstatikos.xrplwalletbackend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserProfileRepository userProfileRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final org.springframework.security.authentication.AuthenticationManager authenticationManager;

	public AuthService( UserProfileRepository userProfileRepository, PasswordEncoder passwordEncoder, JwtService jwtService, org.springframework.security.authentication.AuthenticationManager authenticationManager ) {
		this.userProfileRepository = userProfileRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
	}

	public void register( RegisterRequest request ) {
		if ( userProfileRepository.existsByEmail( request.getEmail() ) ) {
			throw new RuntimeException( "Email already taken" );
		}

		UserProfile userProfile = new UserProfile( request.getEmail(), passwordEncoder.encode( request.getPassword() ) );

		userProfileRepository.save( userProfile );
	}

	public AuthResponse login( LoginRequest request ) {
		try {
			authenticationManager.authenticate( new UsernamePasswordAuthenticationToken( request.getEmail(), request.getPassword() ) );
		} catch ( Exception e ) {
			throw new BadCredentialsException( "Invalid Credentials" );
		}

		String token = jwtService.generateToken( request.getEmail() );
		return new AuthResponse( token );
	}


}
