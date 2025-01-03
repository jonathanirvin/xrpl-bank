package com.xstatikos.xrplwalletbackend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.XAddress;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
public class BankAccountResource {
	private Long id;
	private Long userId;
	private String accountType;
	private Address classicAddress;
	private XAddress xAddress;
	private BigDecimal balance;
	private Instant createdAt;
	private Instant updatedAt;

}
