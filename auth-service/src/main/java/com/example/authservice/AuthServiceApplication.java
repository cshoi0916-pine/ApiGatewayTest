package com.example.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// [미사용] 실제 인증서버(192.168.1.183:10101) + Keycloak JWKS 방식으로 전환 완료 (2026-07-28).
// auth-service는 초기 구현 방식(HS256 + 자체 DB) 참고용으로 보존하며 실제로 기동하지 않는다.
@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
