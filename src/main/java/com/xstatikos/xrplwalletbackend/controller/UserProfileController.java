package com.xstatikos.xrplwalletbackend.controller;

import com.xstatikos.xrplwalletbackend.dto.UserProfileResource;
import com.xstatikos.xrplwalletbackend.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserProfileController {
	private final UserProfileService userProfileService;

	public UserProfileController( UserProfileService userProfileService ) {
		this.userProfileService = userProfileService;
	}

	@GetMapping("/profile")
	public ResponseEntity<UserProfileResource> getUserProfile( @AuthenticationPrincipal UserDetails userDetails ) {

		String email = userDetails.getUsername();
		UserProfileResource userProfileResource = userProfileService.getUserByEmail( email );

		return ResponseEntity.ok( userProfileResource );

	}
}
