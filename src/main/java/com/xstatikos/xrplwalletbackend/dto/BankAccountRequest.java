package com.xstatikos.xrplwalletbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankAccountRequest {
	
	@NotBlank
	private String accountType;

}
