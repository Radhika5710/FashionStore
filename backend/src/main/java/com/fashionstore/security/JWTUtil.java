package com.fashionstore.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Utility for token generation, validation, and refresh
 * Production-ready JWT implementation with HMAC SHA256
 * 
 * SECURITY REQUIREMENT:
 * JWT_SECRET_KEY environment variable MUST be set
 * Minimum 64 characters recommended for production
 */
public class JWTUtil {
    private static final Logger logger = LoggerFactory.getLogger(JWTUtil.class);
    
    // JWT secret key (MUST be set via environment variable)
    private static final String JWT_SECRET = getJwtSecretKey();
    
    private static String getJwtSecretKey() {
        String secret = System.getenv("JWT_SECRET_KEY");
        if (secret == null || secret.isEmpty()) {
            String env = System.getProperty("spring.profiles.active", "development");
            if ("production".equals(env) || "prod".equals(env)) {
                throw new IllegalStateException("JWT_SECRET_KEY environment variable is required in production");
            }
            logger.warn("JWT_SECRET_KEY not set - using development mode only. Set JWT_SECRET_KEY environment variable for production.");
            return "FashionStoreJWTSecretKey2026ForDevelopmentUseOnly";
        }
        if (secret.length() < 32) {
            logger.warn("JWT_SECRET_KEY is too short ({} chars). Minimum 32 characters recommended.", secret.length());
        }
        return secret;
    }
    
    // Token expiration times
    private static final long ACCESS_TOKEN_EXPIRATION = 15 * 60 * 1000; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 days
    
    // Secret key for signing
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    
    /**
     * Generate JWT access token
     */
    public static String generateToken(String userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);
        claims.put("type", "access");
        
        return createToken(claims, ACCESS_TOKEN_EXPIRATION);
    }
    
    /**
     * Generate JWT refresh token
     */
    public static String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        
        return createToken(claims, REFRESH_TOKEN_EXPIRATION);
    }
    
    /**
     * Create JWT token with claims and expiration
     */
    private static String createToken(Map<String, Object> claims, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Validate JWT token and return validation result
     */
    public static TokenValidationResult validateToken(String token) {
        TokenValidationResult result = new TokenValidationResult();
        
        if (token == null || token.isBlank()) {
            result.setValid(false);
            result.setError("Token is null or empty");
            return result;
        }
        
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            result.setValid(true);
            result.setUserId(claims.get("userId", String.class));
            result.setEmail(claims.get("email", String.class));
            result.setRole(claims.get("role", String.class));
            result.setTokenType(claims.get("type", String.class));
            
            return result;
        } catch (ExpiredJwtException e) {
            result.setValid(false);
            result.setError("Token expired");
            logger.warn("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            result.setValid(false);
            result.setError("Unsupported token");
            logger.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            result.setValid(false);
            result.setError("Malformed token");
            logger.warn("Malformed JWT token: {}", e.getMessage());
        } catch (io.jsonwebtoken.security.SignatureException e) {
            result.setValid(false);
            result.setError("Invalid signature");
            logger.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            result.setValid(false);
            result.setError("Invalid token");
            logger.warn("Invalid JWT token: {}", e.getMessage());
        } catch (Exception e) {
            result.setValid(false);
            result.setError("Token validation failed");
            logger.error("JWT validation error: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * Refresh access token using refresh token
     */
    public static TokenRefreshResult refreshToken(String refreshToken) {
        TokenRefreshResult result = new TokenRefreshResult();
        
        TokenValidationResult validation = validateToken(refreshToken);
        
        if (!validation.isValid()) {
            result.setSuccess(false);
            result.setError("Invalid refresh token: " + validation.getError());
            return result;
        }
        
        if (!"refresh".equals(validation.getTokenType())) {
            result.setSuccess(false);
            result.setError("Invalid token type for refresh");
            return result;
        }
        
        try {
            // Generate new access token
            String newAccessToken = generateToken(
                validation.getUserId(),
                validation.getEmail(),
                validation.getRole()
            );
            
            result.setSuccess(true);
            result.setAccessToken(newAccessToken);
            result.setTokenType("Bearer");
            result.setExpiresIn(ACCESS_TOKEN_EXPIRATION / 1000); // in seconds
            
            return result;
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("Failed to refresh token: " + e.getMessage());
            logger.error("Token refresh error: {}", e.getMessage(), e);
            return result;
        }
    }
    
    /**
     * Extract token from Authorization header
     */
    public static String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7); // Remove "Bearer " prefix
    }
    
    /**
     * Get claims from token without validation (for debugging)
     */
    public static Claims getClaimsWithoutValidation(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Failed to parse JWT claims: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Token validation result class
     */
    public static class TokenValidationResult {
        private boolean valid;
        private String error;
        private String userId;
        private String email;
        private String role;
        private String tokenType;
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getTokenType() {
            return tokenType;
        }
        
        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }
    }
    
    /**
     * Token refresh result class
     */
    public static class TokenRefreshResult {
        private boolean success;
        private String error;
        private String accessToken;
        private String tokenType;
        private long expiresIn;
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public String getAccessToken() {
            return accessToken;
        }
        
        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
        
        public String getTokenType() {
            return tokenType;
        }
        
        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }
        
        public long getExpiresIn() {
            return expiresIn;
        }
        
        public void setExpiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
        }
    }
}
