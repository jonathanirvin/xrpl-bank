package com.xstatikos.xrplwalletbackend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

	private final Key key;

	public JwtService( @Value("${jwt.secret}") String secret ) {
		this.key = Keys.hmacShaKeyFor( secret.getBytes() );
	}

	public String generateToken( String username ) {
		return Jwts.builder()
				.setSubject( username )
				.setIssuedAt( new Date() )
				.setExpiration( new Date( System.currentTimeMillis() + 3600000 ) )
				.signWith( key, SignatureAlgorithm.HS256 )
				.compact();

	}

	public String extractUsername( String token ) {
		return Jwts.parserBuilder()
				.setSigningKey( key )
				.build()
				.parseClaimsJws( token )
				.getBody()
				.getSubject();

	}

	public boolean validateToken( String token ) {
		try {
			Jwts.parserBuilder().setSigningKey( key ).build().parseClaimsJws( token );
			return true;
		} catch ( Exception e ) {
			return false;
		}
	}


}
