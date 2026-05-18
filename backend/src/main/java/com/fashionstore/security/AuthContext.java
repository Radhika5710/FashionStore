package com.fashionstore.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized Authentication Context
 * Provides single source of truth for JWT authentication
 * Handles token extraction from headers and cookies
 */
public class AuthContext {
    private static final Logger logger = LoggerFactory.getLogger(AuthContext.class);
    
    private final HttpServletRequest request;
    private JWTUtil.TokenValidationResult validationResult;
    private String accessToken;
    private String refreshToken;
    
    public AuthContext(HttpServletRequest request) {
        this.request = request;
        extractTokens();
        validateAccessToken();
    }
    
    /**
     * Extract tokens from Authorization header and cookies
     */
    private void extractTokens() {
        // Try Authorization header first
        String authHeader = request.getHeader("Authorization");
        this.accessToken = JWTUtil.extractTokenFromHeader(authHeader);
        
        // If not in header, try cookie
        if (accessToken == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("access_token".equals(cookie.getName())) {
                        this.accessToken = cookie.getValue();
                    } else if ("refresh_token".equals(cookie.getName())) {
                        this.refreshToken = cookie.getValue();
                    }
                }
            }
        }
    }
    
    /**
     * Validate access token
     */
    private void validateAccessToken() {
        if (accessToken != null) {
            this.validationResult = JWTUtil.validateToken(accessToken);
        } else {
            this.validationResult = new JWTUtil.TokenValidationResult();
            this.validationResult.setValid(false);
            this.validationResult.setError("No access token provided");
        }
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        return validationResult != null && validationResult.isValid();
    }
    
    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return isAuthenticated() && "admin".equals(validationResult.getRole());
    }
    
    /**
     * Get user ID
     */
    public String getUserId() {
        return isAuthenticated() ? validationResult.getUserId() : null;
    }
    
    /**
     * Get user email
     */
    public String getEmail() {
        return isAuthenticated() ? validationResult.getEmail() : null;
    }
    
    /**
     * Get user role
     */
    public String getRole() {
        return isAuthenticated() ? validationResult.getRole() : null;
    }
    
    /**
     * Get validation result
     */
    public JWTUtil.TokenValidationResult getValidationResult() {
        return validationResult;
    }
    
    /**
     * Get access token
     */
    public String getAccessToken() {
        return accessToken;
    }
    
    /**
     * Get refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired() {
        return validationResult != null && !validationResult.isValid() && 
               validationResult.getError() != null && validationResult.getError().contains("expired");
    }
    
    /**
     * Get authentication error
     */
    public String getError() {
        return validationResult != null ? validationResult.getError() : "Unknown error";
    }
    
    /**
     * Refresh access token using refresh token
     */
    public JWTUtil.TokenRefreshResult refreshAccessToken() {
        if (refreshToken == null) {
            JWTUtil.TokenRefreshResult result = new JWTUtil.TokenRefreshResult();
            result.setSuccess(false);
            result.setError("No refresh token available");
            return result;
        }
        
        return JWTUtil.refreshToken(refreshToken);
    }
    
    /**
     * Get client IP address
     */
    public String getClientIP() {
        return SecurityRequestUtil.getClientIP(request);
    }
    
    /**
     * Get user agent
     */
    public String getUserAgent() {
        return request.getHeader("User-Agent");
    }
    
    /**
     * Static factory method to create AuthContext from request
     */
    public static AuthContext fromRequest(HttpServletRequest request) {
        return new AuthContext(request);
    }
}
