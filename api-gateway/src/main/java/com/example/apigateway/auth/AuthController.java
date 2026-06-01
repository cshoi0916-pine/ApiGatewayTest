package com.example.apigateway.auth;

import com.example.apigateway.security.JwtUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/auth/token")
    public Map<String, String> token(@RequestBody TokenRequest request) {
        String token = jwtUtil.createToken(request.getUsername(), request.getRole());
        return Map.of("token", token);
    }

    public static class TokenRequest {
        private String username;
        private String role = "USER";

        public String getUsername() { return username; }
        public String getRole() { return role; }
        public void setUsername(String username) { this.username = username; }
        public void setRole(String role) { this.role = role; }
    }
}
