package com.xstatikos.xrplwalletbackend.controller;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.xstatikos.xrplwalletbackend.dto.BankAccountRequest;
import com.xstatikos.xrplwalletbackend.dto.BankAccountResource;
import com.xstatikos.xrplwalletbackend.dto.DepositRequest;
import com.xstatikos.xrplwalletbackend.dto.TransactionRequest;
import com.xstatikos.xrplwalletbackend.dto.UserProfileResource;
import com.xstatikos.xrplwalletbackend.service.BankAccountService;
import com.xstatikos.xrplwalletbackend.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/bank-accounts")
public class BankAccountController {

	private final BankAccountService bankAccountService;
	private final UserProfileService userProfileService;

	@Value("${stripe.webhook-secret}")
	private String stripeWebhookSecret;

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

	@PostMapping("/payment-succeeded-webhook")
	public ResponseEntity<String> handleStripePaymentWebhook(
			@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String sigHeader
	) {

		Event event = null;

		try {
			event = Webhook.constructEvent(
					payload, sigHeader, stripeWebhookSecret
			);

			switch ( event.getType() ) {
				case "payment_intent.succeeded":
					handlePaymentIntentSucceeded( event );
					break;

				case "payment_intent.payment_failed":
					// handlePaymentFailed( event );
					break;

				default:
					System.out.println( "Unhandled event type: " + event.getType() );
					break;
			}

			// 4) Return 2xx so Stripe knows you received the event
			return ResponseEntity.ok( "Webhook received" );
		} catch ( Exception e ) {
			throw new RuntimeException( "There was a problem with parsing the webhook event: " + e );
		}
	}

	private void handlePaymentIntentSucceeded( Event event ) {
		try {
			PaymentIntent paymentIntent = ( PaymentIntent ) event.getData().getObject();

			if ( paymentIntent != null ) {
				Map<String, String> metadata = paymentIntent.getMetadata();
				String userId = metadata.get( "userId" );
				long amountInCents = paymentIntent.getAmount(); // in cents

				BigDecimal amountInDollars = BigDecimal.valueOf( amountInCents ).divide( BigDecimal.valueOf( 100 ) );

				bankAccountService.depositStablecoin( Long.valueOf( userId ), amountInDollars );
			} else {
				throw new RuntimeException( "The payment intent was null" );
			}
		} catch ( Exception e ) {
			throw new RuntimeException( "There was an issue with depositing the stablecoin: " + e );
		}
	}

	// Todo: Setup contacts and make it as easy as possible to add a sender account
	// Todo: Setup logins for transactions 
	@PostMapping("/transfer-xrp-to-xrp-address")
	public ResponseEntity<BankAccountResource> transferXrpToXrpAddress( @AuthenticationPrincipal UserDetails
			                                                                    userDetails, @RequestBody @Valid TransactionRequest transactionRequest ) throws Exception {

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
