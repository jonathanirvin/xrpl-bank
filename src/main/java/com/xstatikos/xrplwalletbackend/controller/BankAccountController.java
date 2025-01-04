package com.xstatikos.xrplwalletbackend.controller;

import com.xstatikos.xrplwalletbackend.dto.BankAccountRequest;
import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;
import com.xstatikos.xrplwalletbackend.dto.DepositRequest;
import com.xstatikos.xrplwalletbackend.dto.TransactionRequest;
import com.xstatikos.xrplwalletbackend.dto.UserProfileResource;
import com.xstatikos.xrplwalletbackend.service.BankAccountService;
import com.xstatikos.xrplwalletbackend.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/bank-accounts")
public class BankAccountController {

	private final BankAccountService bankAccountService;
	private final UserProfileService userProfileService;

	public BankAccountController( BankAccountService bankAccountService, UserProfileService userProfileService ) {
		this.bankAccountService = bankAccountService;
		this.userProfileService = userProfileService;
	}

	@PostMapping("/create")
	public ResponseEntity<BankAccountResource> createBankAccount( @AuthenticationPrincipal UserDetails userDetails, @RequestBody @Valid BankAccountRequest bankAccountRequest ) throws Exception {

		if ( userDetails.isEnabled() && userDetails.getUsername() != null ) {

			UserProfileResource userProfileResource = userProfileService.getUserByEmail( userDetails.getUsername() );
			if ( userProfileResource != null ) {
				BankAccountResource bankAccountResource = bankAccountService.createNewBankAccount( userProfileResource.getId(), bankAccountRequest );
				return ResponseEntity.ok( bankAccountResource );
			} else {
				throw new AccessDeniedException( "User not found" );
			}

		} else {
			throw new AccessDeniedException( "User not found" );
		}
	}

	@PostMapping("/deposit")
	public ResponseEntity<Map<String, String>> deposit( @AuthenticationPrincipal UserDetails userDetails, @RequestBody @Valid DepositRequest depositRequest ) throws Exception {

		if ( userDetails.isEnabled() && userDetails.getUsername() != null ) {
			UserProfileResource userProfileResource = userProfileService.getUserByEmail( userDetails.getUsername() );
			if ( userProfileResource != null ) {
				String stripeClientSecret = bankAccountService.depositStripe( userProfileResource.getId(), userProfileResource.getEmail(), depositRequest );

				Map<String, String> response = new HashMap<>();
				response.put( "clientSecret", stripeClientSecret );
				return ResponseEntity.ok( response );
			} else {
				throw new AccessDeniedException( "User not found" );
			}
		} else {
			throw new AccessDeniedException( "User not found" );
		}
	}

	// Todo: Setup contacts and make it as easy as possible to add a sender account
	// Todo: Setup logins for transactions 
	@PostMapping("/transfer-xrp-to-xrp-address")
	public ResponseEntity<BankAccountResource> transferXrpToXrpAddress( @AuthenticationPrincipal UserDetails userDetails, @RequestBody @Valid TransactionRequest transactionRequest ) throws Exception {

		if ( userDetails.isEnabled() && userDetails.getUsername() != null ) {

			UserProfileResource userProfileResource = userProfileService.getUserByEmail( userDetails.getUsername() );
			if ( userProfileResource != null ) {
				BankAccountResource bankAccountResource = bankAccountService.transferXrpToXrpAddress( userProfileResource.getId(), transactionRequest );
				return ResponseEntity.ok( bankAccountResource );
			} else {
				throw new AccessDeniedException( "User not found" );
			}

		} else {
			throw new AccessDeniedException( "User not found" );
		}

	}

}
