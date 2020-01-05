package com.altimetrik.gateway.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

@Document
public class AuthenticationResponse implements Serializable {

    @Id
    private final String jwt;

    private Date validity;

    public AuthenticationResponse(String jwt, Date validity) {
        this.jwt = jwt;
        this.validity = validity;
    }

    public String getJwt() {
        return jwt;
    }

    public Date getValidity() {
        return validity;
    }
}
