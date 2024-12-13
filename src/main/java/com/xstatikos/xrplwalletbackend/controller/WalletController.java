package com.xstatikos.xrplwalletbackend.controller;

import com.xstatikos.xrplwalletbackend.dto.WalletInfoResource;
import okhttp3.HttpUrl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.client.faucet.FaucetClient;
import org.xrpl.xrpl4j.client.faucet.FundAccountRequest;
import org.xrpl.xrpl4j.codec.addresses.AddressCodec;
import org.xrpl.xrpl4j.crypto.keys.KeyPair;
import org.xrpl.xrpl4j.crypto.keys.Seed;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.XAddress;

@RestController
@RequestMapping("/wallet")
public class WalletController {

	private static final boolean RIPPLE_LIVE = false;

	@GetMapping("create")
	public ResponseEntity<WalletInfoResource> create() {

		String rippledUrl = "";
		String faucetUrl = "";

		if ( RIPPLE_LIVE ) {
			// rippledUrl = "https://s2.ripple.com:51234/"; // live rippled
			// rippledUrl = "http://localhost:5005/"; // live self-hosted rippled
		} else {
			rippledUrl = "https://s.altnet.rippletest.net:51234/"; // Testnet
			faucetUrl = "https://faucet.altnet.rippletest.net"; // Testnet
		}

		// init XRPL Client
		HttpUrl rippledHttpUrl = HttpUrl.get( rippledUrl );
		XrplClient xrplClient = new XrplClient( rippledHttpUrl );
		System.out.println( "Constructing an XRPL client, connecting to " + rippledUrl );

		// Create random key pair
		KeyPair randomKeyPair = Seed.ed25519Seed().deriveKeyPair();
		System.out.println( "Generated KeyPair: " + randomKeyPair );

		// Get wallet address from the public key
		Address classicAddress = randomKeyPair.publicKey().deriveAddress();
		XAddress xAddress = AddressCodec.getInstance().classicAddressToXAddress( classicAddress, RIPPLE_LIVE );

		if ( !RIPPLE_LIVE ) {
			// Fund the account using the testnet faucet
			FaucetClient faucetClient = FaucetClient.construct( HttpUrl.get( faucetUrl ) );
			faucetClient.fundAccount( FundAccountRequest.of( classicAddress ) );
			System.out.println( "Funded the account using the testnet faucet" );
		}

		// Lookup Account info
		AccountInfoRequestParams requestParams = AccountInfoRequestParams.of( classicAddress );
		try {
			AccountInfoResult accountInfoResult = xrplClient.accountInfo( requestParams );
			System.out.println( "Account balanace: " + accountInfoResult.accountData().balance() );
			System.out.println( "Validated: " + accountInfoResult.validated() );

			WalletInfoResource walletInfoResource = new WalletInfoResource( accountInfoResult.accountData().balance(), accountInfoResult.validated() );

			System.out.println( "wallet info" + walletInfoResource );
			return ResponseEntity.ok().body( walletInfoResource );
		} catch ( JsonRpcClientErrorException e ) {
			throw new RuntimeException( e );
		}

	}
}
