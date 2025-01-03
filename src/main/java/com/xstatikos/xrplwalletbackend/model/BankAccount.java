package com.xstatikos.xrplwalletbackend.model;

import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.XAddress;

import java.math.BigDecimal;
import java.time.Instant;

@Setter
@Getter
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
	private String classicAddress;

	@Column(nullable = false, unique = true)
	private String xAddress;

	@Column(nullable = false)
	private BigDecimal balance;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@PrePersist
	public void prePersist() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = Instant.now();
	}

	public void setClassicAddress( Address classicAddress ) {
		this.classicAddress = classicAddress.value();
	}

	public void setXAddress( XAddress xAddress ) {
		this.xAddress = xAddress.value();
	}

	public Address getClassicAddress() {
		return Address.of( this.classicAddress );
	}

	public XAddress getXAddress() {
		return XAddress.of( this.xAddress );
	}

	public BankAccountResource toResource() {
		return BankAccountResource.builder()
				.id( id )
				.userId( id )
				.balance( balance )
				.accountType( accountType )
				.classicAddress( Address.of( classicAddress ) )
				.xAddress( XAddress.of( xAddress ) )
				.createdAt( createdAt )
				.updatedAt( updatedAt )
				.build();
	}

}
