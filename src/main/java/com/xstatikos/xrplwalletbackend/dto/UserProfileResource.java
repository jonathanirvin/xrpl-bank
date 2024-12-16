package com.xstatikos.xrplwalletbackend.dto;

import com.xstatikos.xrplwalletbackend.model.UserProfile;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class UserProfileResource implements Serializable {
	private final String email;
	private final Long id;

	public UserProfileResource( UserProfile userProfile ) {
		this.id = userProfile.getId();
		this.email = userProfile.getEmail();
	}

}
