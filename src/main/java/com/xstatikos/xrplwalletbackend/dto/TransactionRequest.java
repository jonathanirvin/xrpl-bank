package com.xstatikos.xrplwalletbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

@Getter
@Setter
public class TransactionRequest {

	@NotNull
	private Address destinationAddress;

	@NotNull
	private XrpCurrencyAmount xrpCurrencyAmount;

}
