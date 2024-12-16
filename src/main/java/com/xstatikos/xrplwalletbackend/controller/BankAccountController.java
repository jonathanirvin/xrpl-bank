package com.xstatikos.xrplwalletbackend.controller;

import com.xstatikos.xrplwalletbackend.dto.BankAccountRequest;
import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;
import com.xstatikos.xrplwalletbackend.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bank-accounts")
public class BankAccountController {
	private final BankAccountService bankAccountService;

	public BankAccountController( BankAccountService bankAccountService ) {
		this.bankAccountService = bankAccountService;
	}

	@PostMapping("/create")
	public ResponseEntity<BankAccountResource> createBankAccount( @RequestBody @Valid BankAccountRequest bankAccountRequest ) {
		// TODO: check to make sure the userId in the request matches the user Id for the principal.
		// Throw 403 Forbidden 

		BankAccountResource bankAccountResource = bankAccountService.createNewBankAccount( bankAccountRequest );
		return ResponseEntity.ok( bankAccountResource );

	}

}
