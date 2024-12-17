package com.xstatikos.xrplwalletbackend.dto;

import lombok.Getter;
import lombok.Setter;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.XAddress;

@Getter
@Setter
public class BankAccountResource {
	private Long id;
	private String accountType;
	private Address classicAddress;
	private XAddress xAddress;
	private String createdAt;
	private String updatedAt;

}
