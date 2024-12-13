package com.xstatikos.xrplwalletbackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xstatikos.xrplwalletbackend.model.UserProfile;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserProfileResource implements Serializable {
	private final String email;

	public UserProfileResource( UserProfile userProfile ) {
		this.email = userProfile.getEmail();
	}

	public String getEmail() {
		return this.email;
	}
}
