package com.xstatikos.xrplwalletbackend.service;

import com.xstatikos.xrplwalletbackend.dto.UserProfileResource;


public interface UserProfileService {
	UserProfileResource getUserByEmail( String email );


}
