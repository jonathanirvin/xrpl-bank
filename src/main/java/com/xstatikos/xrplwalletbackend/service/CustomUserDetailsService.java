package com.xstatikos.xrplwalletbackend.service;

import com.xstatikos.xrplwalletbackend.model.UserProfile;
import com.xstatikos.xrplwalletbackend.repository.UserProfileRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserProfileRepository userProfileRepository;

	public CustomUserDetailsService( UserProfileRepository userProfileRepository ) {
		this.userProfileRepository = userProfileRepository;
	}


	@Override
	public UserDetails loadUserByUsername( String email ) throws UsernameNotFoundException {

		UserProfile userProfile = userProfileRepository.findByEmail( email ).orElseThrow( () -> new UsernameNotFoundException( "User not found" ) );

		return new org.springframework.security.core.userdetails.User(
				userProfile.getEmail(),
				userProfile.getPassword(),
				List.of( new SimpleGrantedAuthority( "ROLE_USER" ) )
		);
	}
}
