package com.xstatikos.xrplwalletbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankAccountRequest {

	@NotNull
	private Long userId;

	@NotBlank
	private String accountType;

}
