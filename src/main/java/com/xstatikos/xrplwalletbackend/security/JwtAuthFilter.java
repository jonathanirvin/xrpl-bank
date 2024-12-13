package com.xstatikos.xrplwalletbackend.security;

import com.xstatikos.xrplwalletbackend.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.GenericFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

public class JwtAuthFilter extends GenericFilter {
	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;

	public JwtAuthFilter( JwtService jwtService, CustomUserDetailsService customUserDetailsService ) {
		this.jwtService = jwtService;
		this.customUserDetailsService = customUserDetailsService;
	}

	@Override
	public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException, ServletException {
		HttpServletRequest httpReq = ( HttpServletRequest ) request;
		String authHeader = httpReq.getHeader( "Authorization" );

		if ( authHeader != null && authHeader.startsWith( "Bearer " ) ) {
			String token = authHeader.substring( 7 );
			if ( jwtService.validateToken( token ) ) {
				String username = jwtService.extractUsername( token );
				UserDetails userDetails = customUserDetailsService.loadUserByUsername( username );
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken( userDetails, null, userDetails.getAuthorities() );
				SecurityContextHolder.getContext().setAuthentication( authToken );
			}
		}

		chain.doFilter( request, response );
	}
}
