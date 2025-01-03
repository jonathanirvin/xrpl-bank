package com.xstatikos.xrplwalletbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DepositRequest {
	@NotNull
	private BigDecimal amountToDeposit;

}
