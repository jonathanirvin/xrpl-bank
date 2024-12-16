package com.xstatikos.xrplwalletbackend.dto;

import lombok.Getter;
import lombok.Setter;
import org.xrpl.xrpl4j.model.transactions.Address;

@Getter
@Setter
public class BankAccountResource {
	private Long id;
	private String accountType;
	private Address xrplAddress;
	private String createdAt;
	private String updatedAt;

}
