package com.fashionstore.service;

import com.fashionstore.model.PasswordResetToken;

/**
 * PasswordResetService - MVC Service Layer Interface
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL password reset token operations in service layer
 * - ALL token validation in service layer
 * - Controllers only handle request/response
 * - DAO layer only handles database access
 */
public interface PasswordResetService {
    
    /**
     * Create a password reset token for a user
     */
    PasswordResetToken createToken(String email);
    
    /**
     * Get token by token string
     */
    PasswordResetToken getTokenByToken(String token);
    
    /**
     * Validate token is valid and not expired
     */
    boolean isTokenValid(String token);
    
    /**
     * Invalidate token after use
     */
    boolean invalidateToken(String token);
    
    /**
     * Clean up expired tokens
     */
    int cleanupExpiredTokens();
}
