package com.xstatikos.xrplwalletbackend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RegisterRequest {
	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String password;

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}
}
