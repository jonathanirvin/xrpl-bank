package com.xstatikos.xrplwalletbackend.service.impl;

import com.xstatikos.xrplwalletbackend.dto.UserProfileResource;
import com.xstatikos.xrplwalletbackend.model.UserProfile;
import com.xstatikos.xrplwalletbackend.repository.UserProfileRepository;
import com.xstatikos.xrplwalletbackend.service.UserProfileService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileServiceImpl implements UserProfileService {

	private final UserProfileRepository userProfileRepository;

	public UserProfileServiceImpl( UserProfileRepository userProfileRepository ) {
		this.userProfileRepository = userProfileRepository;
	}

	public UserProfileResource getUserByEmail( String email ) throws RuntimeException {
		Optional<UserProfile> user = userProfileRepository.findByEmail( email );
		if ( user.isPresent() ) {
			return new UserProfileResource( user.get() );
		} else {
			throw new RuntimeException( "User not found!" );
		}
	}

}
