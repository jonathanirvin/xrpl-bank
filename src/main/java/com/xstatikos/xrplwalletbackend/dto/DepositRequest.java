package com.xstatikos.xrplwalletbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepositRequest {

	private long amountToDeposit;

	private String email;

	private String customerName;

}
