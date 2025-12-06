package com.tiptracker.backend.security;

import com.tiptracker.backend.model.User;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for handling JSON Web Token (JWT) operations,
 * including token generation, validation, and claim extraction.
 */
@Component
public class JwtUtil {

    private final Key signingKey;
    private final long expirationTime;

    /**
     * Constructs the JwtUtil and initializes the signing key and expiration time
     * from environment variables injected by Spring.
     */
    public JwtUtil(@Value("${APP_JWT_SECRET}") String secretKeyString,
                   @Value("${APP_JWT_EXPIRATION}") long expirationTime) {
        if (secretKeyString == null || secretKeyString.isEmpty()) {
            throw new IllegalStateException("JWT secret key ('APP_JWT_SECRET') is not set in the environment configuration.");
        }
        this.expirationTime = expirationTime;
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a new JWT for a given user.
     * @param user The user for whom the token is being generated.
     * @return A signed JWT string.
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates a JWT. Checks if the token belongs to the given user and has not expired.
     * @param token The JWT string.
     * @param userEmail The email of the user to validate against.
     * @return True if the token is valid, false otherwise.
     */
    public boolean validateToken(String token, String userEmail) {
        try {
            final String emailInToken = extractEmail(token);
            return (emailInToken.equals(userEmail) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extracts the user's email (the subject) from the JWT.
     * @param token The JWT string.
     * @return The user's email.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * A generic function to extract a specific claim from a JWT.
     * @param token The JWT string.
     * @param claimsResolver A function to apply to the claims.
     * @return The extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Checks if a token has expired.
     * @param token The JWT string.
     * @return True if the token is expired, false otherwise.
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Extracts all claims from a JWT after validating its signature.
     * @param token The JWT string.
     * @return The claims body of the token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}