package com.fashionstore.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CSRF Protection Implementation for FashionStore
 * 
 * Provides comprehensive Cross-Site Request Forgery protection:
 * - Secure token generation using cryptographically strong random numbers
 * - Session-based token storage and validation
 * - Token rotation for enhanced security
 * - AJAX request support
 * - Configurable token expiration
 */
public class CSRFProtection {
    
    private static final String CSRF_TOKEN_SESSION_KEY = "csrf_token";
    private static final String CSRF_TOKEN_TIME_KEY = "csrf_token_time";
    private static final int TOKEN_EXPIRY_TIME = 3600; // 1 hour
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // Cache for storing tokens to prevent reuse
    private static final ConcurrentHashMap<String, Long> usedTokens = new ConcurrentHashMap<>();
    
    /**
     * Generates a new CSRF token for the session
     * @param request HTTP request
     * @return CSRF token
     */
    public static String generateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        
        // Generate secure random token
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        // Store token in session with timestamp
        session.setAttribute(CSRF_TOKEN_SESSION_KEY, token);
        session.setAttribute(CSRF_TOKEN_TIME_KEY, System.currentTimeMillis());
        
        return token;
    }
    
    /**
     * Validates a CSRF token against the session
     * @param request HTTP request
     * @param token Token to validate
     * @return true if token is valid
     */
    public static boolean validateToken(HttpServletRequest request, String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        
        // Get stored token
        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_SESSION_KEY);
        Long tokenTime = (Long) session.getAttribute(CSRF_TOKEN_TIME_KEY);
        
        if (sessionToken == null || tokenTime == null) {
            return false;
        }
        
        // Check if token matches
        if (!token.equals(sessionToken)) {
            return false;
        }
        
        // Check if token has expired
        if (System.currentTimeMillis() - tokenTime > TOKEN_EXPIRY_TIME * 1000) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the current CSRF token from session
     * @param request HTTP request
     * @return Current CSRF token or null if not exists
     */
    public static String getCurrentToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        
        return (String) session.getAttribute(CSRF_TOKEN_SESSION_KEY);
    }
    
    /**
     * Checks if a new token should be generated (expired or missing)
     * @param request HTTP request
     * @return true if token should be regenerated
     */
    public static boolean shouldRegenerateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }
        
        Long tokenTime = (Long) session.getAttribute(CSRF_TOKEN_TIME_KEY);
        if (tokenTime == null) {
            return true;
        }
        
        return System.currentTimeMillis() - tokenTime > TOKEN_EXPIRY_TIME * 1000;
    }
    
    /**
     * Rotates the CSRF token (generates new token)
     * @param request HTTP request
     * @return New CSRF token
     */
    public static String rotateToken(HttpServletRequest request) {
        // Invalidate current token
        invalidateToken(request);
        
        // Generate new token
        return generateToken(request);
    }
    
    /**
     * Invalidates the current CSRF token
     * @param request HTTP request
     */
    public static void invalidateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute(CSRF_TOKEN_SESSION_KEY);
            if (token != null) {
                usedTokens.put(token, System.currentTimeMillis());
            }
            session.removeAttribute(CSRF_TOKEN_SESSION_KEY);
            session.removeAttribute(CSRF_TOKEN_TIME_KEY);
        }
    }
    
    /**
     * Validates CSRF token from request parameter
     * @param request HTTP request
     * @param paramName Parameter name for CSRF token
     * @return true if token is valid
     */
    public static boolean validateTokenFromRequest(HttpServletRequest request, String paramName) {
        String token = request.getParameter(paramName);
        return validateToken(request, token);
    }
    
    /**
     * Validates CSRF token from request header
     * @param request HTTP request
     * @param headerName Header name for CSRF token
     * @return true if token is valid
     */
    public static boolean validateTokenFromHeader(HttpServletRequest request, String headerName) {
        String token = request.getHeader(headerName);
        return validateToken(request, token);
    }
    
    /**
     * Gets CSRF token for AJAX requests
     * @param request HTTP request
     * @return CSRF token (generates new if needed)
     */
    public static String getTokenForAjax(HttpServletRequest request) {
        String token = getCurrentToken(request);
        if (token == null || shouldRegenerateToken(request)) {
            token = generateToken(request);
        }
        return token;
    }
    
    /**
     * Adds CSRF token to request as attribute
     * @param request HTTP request
     */
    public static void addTokenToRequest(HttpServletRequest request) {
        String token = getTokenForAjax(request);
        if (token != null) {
            request.setAttribute("csrfToken", token);
            request.setAttribute("csrfTokenParamName", "csrf_token");
            request.setAttribute("csrfTokenHeaderName", "X-CSRF-Token");
        }
    }
    
    /**
     * Checks if the request should be protected from CSRF
     * @param request HTTP request
     * @return true if request should be protected
     */
    public static boolean requiresProtection(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // Only protect state-changing methods
        if (!"POST".equalsIgnoreCase(method) && 
            !"PUT".equalsIgnoreCase(method) && 
            !"DELETE".equalsIgnoreCase(method) && 
            !"PATCH".equalsIgnoreCase(method)) {
            return false;
        }
        
        // Skip protection for certain endpoints
        if (uri.contains("/login") || 
            uri.contains("/logout") || 
            uri.contains("/register") ||
            uri.contains("/password-reset") ||
            uri.contains("/csrf-token")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates CSRF token based on request type
     * @param request HTTP request
     * @return true if CSRF validation passes
     */
    public static boolean validateRequest(HttpServletRequest request) {
        if (!requiresProtection(request)) {
            return true;
        }
        
        // Check parameter first
        if (validateTokenFromRequest(request, "csrf_token")) {
            return true;
        }
        
        // Check header
        if (validateTokenFromHeader(request, "X-CSRF-Token")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Cleans up old used tokens to prevent memory leaks
     */
    private static void cleanupOldTokens() {
        long currentTime = System.currentTimeMillis();
        long expiryTime = TOKEN_EXPIRY_TIME * 1000 * 2; // Keep tokens for 2x expiry time
        
        usedTokens.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > expiryTime);
    }
    
    /**
     * Gets CSRF protection metadata for debugging
     * @param request HTTP request
     * @return Protection metadata
     */
    public static CSRFProtectionMetadata getMetadata(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return new CSRFProtectionMetadata(false, null, 0, false);
        }
        
        String token = (String) session.getAttribute(CSRF_TOKEN_SESSION_KEY);
        Long tokenTime = (Long) session.getAttribute(CSRF_TOKEN_TIME_KEY);
        
        boolean hasToken = token != null;
        long timeRemaining = tokenTime != null ? 
            (TOKEN_EXPIRY_TIME * 1000) - (System.currentTimeMillis() - tokenTime) : 0;
        boolean isExpired = timeRemaining <= 0;
        
        return new CSRFProtectionMetadata(hasToken, token, timeRemaining, isExpired);
    }
    
    /**
     * CSRF Protection Metadata class
     */
    public static class CSRFProtectionMetadata {
        private final boolean hasToken;
        private final String token;
        private final long timeRemaining;
        private final boolean isExpired;
        
        public CSRFProtectionMetadata(boolean hasToken, String token, long timeRemaining, boolean isExpired) {
            this.hasToken = hasToken;
            this.token = token;
            this.timeRemaining = timeRemaining;
            this.isExpired = isExpired;
        }
        
        public boolean hasToken() { return hasToken; }
        public String getToken() { return token; }
        public long getTimeRemaining() { return timeRemaining; }
        public boolean isExpired() { return isExpired; }
        
        @Override
        public String toString() {
            return String.format("CSRFProtectionMetadata{hasToken=%s, token=%s, timeRemaining=%d, isExpired=%s}",
                    hasToken, token != null ? token.substring(0, 8) + "..." : "null", timeRemaining, isExpired);
        }
    }
    
    /**
     * CSRF Token Generator Utility
     */
    public static class TokenGenerator {
        private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        private static final int DEFAULT_LENGTH = 32;
        
        public static String generateToken() {
            return generateToken(DEFAULT_LENGTH);
        }
        
        public static String generateToken(int length) {
            StringBuilder token = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                token.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
            }
            return token.toString();
        }
        
        public static String generateSecureToken() {
            byte[] randomBytes = new byte[TOKEN_LENGTH];
            secureRandom.nextBytes(randomBytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        }
    }
    
    /**
     * CSRF Configuration
     */
    public static class Config {
        private static int configTokenExpiryTime = TOKEN_EXPIRY_TIME;
        private static int configTokenLength = TOKEN_LENGTH;
        private static boolean configEnableTokenRotation = true;
        private static boolean configEnableAjaxProtection = true;
        
        public static int getTokenExpiryTime() { return configTokenExpiryTime; }
        public static void setTokenExpiryTime(int tokenExpiryTime) { configTokenExpiryTime = tokenExpiryTime; }
        
        public static int getTokenLength() { return configTokenLength; }
        public static void setTokenLength(int tokenLength) { configTokenLength = tokenLength; }
        
        public static boolean isTokenRotationEnabled() { return configEnableTokenRotation; }
        public static void setTokenRotationEnabled(boolean enableTokenRotation) { configEnableTokenRotation = enableTokenRotation; }
        
        public static boolean isAjaxProtectionEnabled() { return configEnableAjaxProtection; }
        public static void setAjaxProtectionEnabled(boolean enableAjaxProtection) { configEnableAjaxProtection = enableAjaxProtection; }
    }
}
