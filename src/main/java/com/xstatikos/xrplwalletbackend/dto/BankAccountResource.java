package com.xstatikos.xrplwalletbackend.dto;

import lombok.Getter;
import lombok.Setter;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.XAddress;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

import java.time.Instant;

@Getter
@Setter
public class BankAccountResource {
	private Long id;
	private String accountType;
	private Address classicAddress;
	private XAddress xAddress;
	private XrpCurrencyAmount balance;
	private Instant createdAt;
	private Instant updatedAt;

}
