package com.bikeparking.backend.util;

import com.bikeparking.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @Autowired
    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Integer userId, String username, List<String> roles) {
        return generateToken(userId, username, roles, jwtProperties.getAccessTokenExpiration());
    }

    public String generateRefreshToken(Integer userId, String username) {
        return generateToken(userId, username, null, jwtProperties.getRefreshTokenExpiration());
    }

    private String generateToken(Integer userId, String username, List<String> roles, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey);

        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }

        return builder.compact();
    }

    public Integer getUserIdFromToken(String token) {
        return Integer.valueOf(getClaimFromToken(token, Claims::getSubject));
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("username", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("roles", List.class));
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public JwtProperties getJwtProperties() {
        return jwtProperties;
    }
}

