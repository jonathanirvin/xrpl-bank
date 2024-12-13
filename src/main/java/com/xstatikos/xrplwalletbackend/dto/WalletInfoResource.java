package com.xstatikos.xrplwalletbackend.dto;

import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;

public class WalletInfoResource {
	private XrpCurrencyAmount balance;
	private boolean validated;

	public WalletInfoResource() {
	}

	public WalletInfoResource( XrpCurrencyAmount balance, boolean validated ) {
		this.balance = balance;
		this.validated = validated;
	}

	public XrpCurrencyAmount getBalance() {
		return balance;
	}

	public boolean isValidated() {
		return validated;
	}

	public void setBalance( XrpCurrencyAmount balance ) {
		this.balance = balance;
	}

	public void setValidated( boolean validated ) {
		this.validated = validated;
	}
}
