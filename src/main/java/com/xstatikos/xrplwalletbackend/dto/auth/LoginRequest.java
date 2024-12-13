package com.xstatikos.xrplwalletbackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String password;

	public String getEmail() {
		return this.email;
	}

	public String getPassword() {
		return this.password;
	}
}
