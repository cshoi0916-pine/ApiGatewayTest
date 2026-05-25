package com.example.apigateway.auth;

import com.example.apigateway.jwt.JwtProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    private final JwtProvider jwtProvider;

    public AuthController(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @GetMapping("/auth/token")
    public Map<String, String> token(
            @RequestParam String username
    ) {

        String token =
                jwtProvider.createToken(
                        username,
                        "USER"
                );

        return Map.of(
                "token",
                token
        );
    }
}