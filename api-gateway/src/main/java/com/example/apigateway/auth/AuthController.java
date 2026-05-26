package com.example.apigateway.auth;

import com.example.apigateway.security.JwtUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    @GetMapping("/auth/token")
    public Map<String, String> token(
            @RequestParam String username
    ) {

        String token =
                JwtUtil.createToken(
                        username,
                        "USER"
                );

        return Map.of(
                "token",
                token
        );
    }
}