package com.xstatikos.xrplwalletbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.xrpl.xrpl4j.model.transactions.Address;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bank_accounts")
public class BankAccount {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private long userId;

	@Column(nullable = false)
	private String accountType;

	@Column(nullable = false, unique = true)
	private String walletIdentifier;

	@Column(nullable = false, unique = true)
	private Address xrplAddress;

	@Column(nullable = false)
	private Instant created_at = Instant.now();

	@Column(nullable = false)
	private Instant updated_at = Instant.now();
	
}
