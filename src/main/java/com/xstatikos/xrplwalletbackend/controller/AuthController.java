package com.xstatikos.xrplwalletbackend.controller;

import com.xstatikos.xrplwalletbackend.dto.auth.AuthResponse;
import com.xstatikos.xrplwalletbackend.dto.auth.LoginRequest;
import com.xstatikos.xrplwalletbackend.dto.auth.RegisterRequest;
import com.xstatikos.xrplwalletbackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController( AuthService authService ) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<Void> register( @RequestBody @Valid RegisterRequest request ) {
		authService.register( request );
		return ResponseEntity.ok().build();
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login( @RequestBody @Valid LoginRequest request ) {
		AuthResponse token = authService.login( request );
		return ResponseEntity.ok( token );
	}

}
