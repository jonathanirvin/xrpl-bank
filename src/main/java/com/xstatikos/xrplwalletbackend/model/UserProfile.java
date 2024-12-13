package com.xstatikos.xrplwalletbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "users")
public class UserProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Email
	@Column(unique = true)
	private String email;

	@NotBlank
	private String password;

	public UserProfile() { }

	public UserProfile( String email, String password ) {
		this.email = email;
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail( String Email ) {
		this.email = email;
	}

	public void setPassword( String password ) {
		this.password = password;
	}

	public String getPassword() {
		return this.password;
	}

}
