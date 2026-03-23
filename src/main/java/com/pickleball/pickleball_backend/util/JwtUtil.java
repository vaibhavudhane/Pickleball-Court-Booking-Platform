package com.pickleball.pickleball_backend.util;

import com.pickleball.pickleball_backend.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Create a JWT token for a user
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())              // "sub" claim = email
                .claim("role", user.getRole().name())     // Custom claim = role
                .claim("userId", user.getId())            // Custom claim = userId
                .setIssuedAt(new Date())                  // Token creation time
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256) // Digital signature
                .compact();
    }

    // Extract email from token
    public String extractEmail(String token) {
        return getAllClaims(token).getSubject();
    }

    // Extract role from token
    public String extractRole(String token) {
        return getAllClaims(token).get("role", String.class);
    }

    // Check if token is valid (not tampered, not expired)
    public boolean isTokenValid(String token) {
        try {
            getAllClaims(token); // This throws exception if invalid
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
