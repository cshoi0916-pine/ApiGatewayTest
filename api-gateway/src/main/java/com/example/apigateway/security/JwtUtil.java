package com.example.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET =
            "my-secret-key-my-secret-key-my-secret-key-1234";

    private static final SecretKey key =
            Keys.hmacShaKeyFor(SECRET.getBytes());

    private static final long EXP =
            1000 * 60 * 60;

    public static String createToken(String userId) {

        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis() + EXP
                        )
                )
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static boolean validate(String token) {

        try {

            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    public static String getUserId(String token) {

        Claims claims =
                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

        return claims.getSubject();
    }
}