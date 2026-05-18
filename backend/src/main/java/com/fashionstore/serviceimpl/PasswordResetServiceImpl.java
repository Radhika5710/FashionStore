package com.fashionstore.serviceimpl;

import com.fashionstore.dao.PasswordResetTokenDAO;
import com.fashionstore.daoimpl.PasswordResetTokenDAOImpl;
import com.fashionstore.model.PasswordResetToken;
import com.fashionstore.model.User;
import com.fashionstore.service.PasswordResetService;
import com.fashionstore.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * PasswordResetServiceImpl - MVC Service Layer Implementation
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL password reset token business logic in service layer
 * - ALL validation in service layer
 * - Controllers only handle request/response
 * - DAO layer only handles database access
 */
public class PasswordResetServiceImpl implements PasswordResetService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRY_HOURS = 1;
    
    private final PasswordResetTokenDAO tokenDAO;
    private final UserService userService;
    
    public PasswordResetServiceImpl() {
        this.tokenDAO = new PasswordResetTokenDAOImpl();
        this.userService = new UserService();
    }
    
    public PasswordResetServiceImpl(PasswordResetTokenDAO tokenDAO, UserService userService) {
        this.tokenDAO = tokenDAO;
        this.userService = userService;
    }
    
    @Override
    public PasswordResetToken createToken(String email) {
        // Business logic: Validate email exists
        User user = userService.getUserByEmail(email);
        if (user == null) {
            logger.warn("Password reset requested for non-existent email: {}", email);
            return null;
        }
        
        // Business logic: Generate secure token
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String tokenString = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Business logic: Create token with expiry
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getUserId());
        token.setToken(tokenString);
        token.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        
        int tokenId = tokenDAO.createToken(token);
        if (tokenId > 0) {
            token.setTokenId(tokenId);
            logger.info("Password reset token created for user: {}", user.getUserId());
            return token;
        }
        
        logger.error("Failed to save password reset token for user: {}", user.getUserId());
        return null;
    }
    
    @Override
    public PasswordResetToken getTokenByToken(String token) {
        return tokenDAO.getTokenByToken(token);
    }
    
    @Override
    public boolean isTokenValid(String token) {
        PasswordResetToken resetToken = tokenDAO.getTokenByToken(token);
        if (resetToken == null) {
            return false;
        }
        return resetToken.isValid();
    }
    
    @Override
    public boolean invalidateToken(String token) {
        PasswordResetToken resetToken = tokenDAO.getTokenByToken(token);
        if (resetToken == null) return false;
        return tokenDAO.deleteToken(resetToken.getTokenId());
    }
    
    @Override
    public int cleanupExpiredTokens() {
        return tokenDAO.deleteExpiredTokens() ? 1 : 0;
    }
}
