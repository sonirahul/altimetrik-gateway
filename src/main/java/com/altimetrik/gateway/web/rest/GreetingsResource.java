package com.altimetrik.gateway.web.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gateway")
public class GreetingsResource {

    @GetMapping("/greet")
    public ResponseEntity<String> greet() {
        return ResponseEntity.ok("hello rahul soni");
    }

}
