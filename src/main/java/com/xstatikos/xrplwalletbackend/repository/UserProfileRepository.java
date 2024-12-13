package com.xstatikos.xrplwalletbackend.repository;

import com.xstatikos.xrplwalletbackend.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
	Optional<UserProfile> findByEmail( String email );

	boolean existsByEmail( String email );
}