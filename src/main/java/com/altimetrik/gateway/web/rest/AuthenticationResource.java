package com.altimetrik.gateway.web.rest;

import com.altimetrik.gateway.config.Constants;
import com.altimetrik.gateway.config.security.jwt.JwtProvider;
import com.altimetrik.gateway.dto.AuthenticationRequest;
import com.altimetrik.gateway.dto.AuthenticationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping("/api")
public class AuthenticationResource {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private AuthenticationManager authenticationManager;

    private JwtProvider jwtProvider;

    public AuthenticationResource(AuthenticationManager authenticationManager, JwtProvider jwtProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest authenticationRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken
                    (authenticationRequest.getUsername(), authenticationRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Incorrect username or password", e);
        }

        final AuthenticationResponse response =
                jwtProvider.createToken(authentication, authenticationRequest.getUsername());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(Constants.AUTHORIZATION, Constants.BEARER + response.getJwt());
        return new ResponseEntity<>(response, httpHeaders, HttpStatus.OK);
    }

    @GetMapping("/refreshToken")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestHeader(Constants.AUTHORIZATION) final String token) {

        final AuthenticationResponse response = jwtProvider.refreshToken(token.substring(7));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(Constants.AUTHORIZATION, Constants.BEARER + response.getJwt());
        return new ResponseEntity<>(response, httpHeaders, HttpStatus.OK);
    }

}
