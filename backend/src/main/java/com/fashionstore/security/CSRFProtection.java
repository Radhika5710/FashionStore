package com.fashionstore.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CSRFProtection - Hybrid MVC + React Admin Architecture
 * 
 * ARCHITECTURE:
 * =============
 * - JSP Customer Frontend: Uses CSRF protection
 *   * Session-based authentication
 *   * Form submissions require CSRF token
 *   * AJAX requests include CSRF token
 *   * Token stored in HttpSession
 * 
 * - React Admin APIs: NO CSRF protection (uses JWT instead)
 *   * JWT token-based authentication
 *   * No HttpSession required
 *   * No CSRF token needed
 *   * Routes: /api/admin/*
 * 
 * CSRF PROTECTION DETAILS:
 * =======================
 * - Secure token generation using cryptographically strong random numbers
 * - Session-based token storage and validation
 * - Token rotation for enhanced security
 * - AJAX request support (X-CSRF-Token header)
 * - Configurable token expiration (1 hour)
 * - Token reuse prevention via cache
 * 
 * HYBRID ARCHITECTURE BENEFITS:
 * =============================
 * ✓ CSRF protection for JSP forms (session-based)
 * ✓ No CSRF overhead for admin APIs (JWT-based)
 * ✓ Separate authentication methods for different frontends
 * ✓ No session conflicts between customer and admin auth
 * ✓ Clear separation of concerns
 * 
 * USAGE:
 * ======
 * Customer Frontend (JSP):
 * - Generate token: CSRFProtection.generateToken(request)
 * - Validate token: CSRFProtection.validateToken(request, token)
 * - Add to request attributes: CSRFProtection.addTokenToRequest(request)
 * 
 * Admin APIs (React):
 * - No CSRF protection needed
 * - JWT validation handles security
 * - No session required
 */
public class CSRFProtection {
    
    private static final Logger logger = LoggerFactory.getLogger(CSRFProtection.class);
    
    private static final String CSRF_TOKEN_SESSION_KEY = "csrfToken";
    private static final String CSRF_TOKEN_TIME_KEY = "csrfTokenTime";
    private static final int TOKEN_EXPIRY_TIME = 3600; // 1 hour
    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // Cache for storing tokens to prevent reuse
    private static final ConcurrentHashMap<String, Long> usedTokens = new ConcurrentHashMap<>();
    
    // CSRF enabled flag
    private static volatile boolean csrfEnabled = true;
    
    static {
        // Check if CSRF is enabled via environment variable
        String csrfEnv = System.getenv("CSRF_ENABLED");
        if (csrfEnv != null && csrfEnv.equalsIgnoreCase("false")) {
            csrfEnabled = false;
            logger.info("CSRF protection disabled via CSRF_ENABLED=false");
        }
        
        // Check if demo mode is enabled
        String demoMode = System.getenv("DEMO_MODE");
        if (demoMode != null && demoMode.equalsIgnoreCase("true")) {
            csrfEnabled = false;
            logger.info("CSRF protection disabled via DEMO_MODE=true");
        }
    }
    
    /**
     * Checks if CSRF protection is enabled
     */
    public static boolean isEnabled() {
        return csrfEnabled;
    }
    
    /**
     * Generates a new CSRF token for the session
     * @param request HTTP request
     * @return CSRF token
     */
    public static String generateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        
        // Generate secure random token
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        // Store token in session with timestamp
        long generatedAt = System.currentTimeMillis();
        session.setAttribute(CSRF_TOKEN_SESSION_KEY, token);
        session.setAttribute(CSRF_TOKEN_TIME_KEY, generatedAt);
        
        logger.debug("CSRF token generated for session: {}", session.getId());
        
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
            logger.warn("CSRF validation failed: token is null or empty - Path: {}", request.getRequestURI());
            return false;
        }
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            logger.warn("CSRF validation failed: no session exists - Path: {}", request.getRequestURI());
            return false;
        }
        
        // Get stored token
        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_SESSION_KEY);
        Long tokenTime = (Long) session.getAttribute(CSRF_TOKEN_TIME_KEY);
        
        if (sessionToken == null) {
            logger.warn("CSRF validation failed: no token in session - Session ID: {}, Path: {}", 
                session.getId(), request.getRequestURI());
            return false;
        }
        
        // Check if token matches
        if (!token.equals(sessionToken)) {
            logger.warn("CSRF validation failed: token mismatch - Session ID: {}, Path: {}, Provided token: {}... , Expected token: {}...", 
                session.getId(), request.getRequestURI(), 
                token.substring(0, Math.min(8, token.length())), 
                sessionToken.substring(0, Math.min(8, sessionToken.length())));
            return false;
        }

        // Check if token has expired
        if (tokenTime != null && System.currentTimeMillis() - tokenTime > TOKEN_EXPIRY_TIME * 1000) {
            logger.warn("CSRF validation failed: token expired - Session ID: {}, Path: {}, Token age: {}ms", 
                session.getId(), request.getRequestURI(), 
                System.currentTimeMillis() - tokenTime);
            return false;
        }
        
        logger.debug("CSRF validation successful - Session ID: {}, Path: {}", session.getId(), request.getRequestURI());
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
        if (token == null || token.trim().isEmpty()) {
            logger.debug("CSRF token not found in parameter: {} - Path: {}", paramName, request.getRequestURI());
        }
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
        if (token == null || token.trim().isEmpty()) {
            logger.debug("CSRF token not found in header: {} - Path: {}", headerName, request.getRequestURI());
        }
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
            request.setAttribute("csrfTokenParamName", "csrfToken");
            request.setAttribute("csrfTokenHeaderName", "X-CSRF-Token");
        }
    }
    
    /**
     * Servlet path relative to the application context (no query string, no session suffix).
     */
    static String contextRelativePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isEmpty()) {
            uri = "/";
        }
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        if (uri.isEmpty()) {
            uri = "/";
        }
        int semi = uri.indexOf(';');
        if (semi >= 0) {
            uri = uri.substring(0, semi);
        }
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    /**
     * Checks if the request should be protected from CSRF
     * @param request HTTP request
     * @return true if request should be protected
     */
    public static boolean requiresProtection(HttpServletRequest request) {
        String method = request.getMethod();

        // Only protect state-changing methods
        if (!"POST".equalsIgnoreCase(method) &&
            !"PUT".equalsIgnoreCase(method) &&
            !"DELETE".equalsIgnoreCase(method) &&
            !"PATCH".equalsIgnoreCase(method)) {
            return false;
        }

        String path = contextRelativePath(request);

        // Login / register / logout forms use their own flow; still safe when tokens are absent only if skipped here.
        if ("/login".equals(path) || "/logout".equals(path) || "/register".equals(path)) {
            return false;
        }

        // Payment gateway callbacks cannot supply browser session CSRF tokens.
        if ("/payment".equals(path) && "webhook".equals(request.getParameter("action"))) {
            return false;
        }

        // Optional dedicated CSRF refresh endpoint (reserved).
        if ("/csrf-token".equals(path)) {
            return false;
        }

        // Admin JSON API: protected by Origin/CORS allow-list + session cookie.
        // CSRF tokens cannot be shared across origins (Vite dev server runs on :5173).
        if (path != null && path.startsWith("/api/admin/")) {
            return false;
        }

        // Metrics endpoint for client-side telemetry - public endpoint
        if ("/api/metrics".equals(path)) {
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
        // Skip CSRF validation if disabled
        if (!csrfEnabled) {
            return true;
        }
        
        if (!requiresProtection(request)) {
            return true;
        }
        
        // Check parameter first (standardized key)
        if (validateTokenFromRequest(request, "csrfToken")) {
            return true;
        }
        
        // Check header for AJAX requests
        if (validateTokenFromHeader(request, "X-CSRF-Token")) {
            return true;
        }
        
        logger.warn("CSRF validation failed for request - Path: {}, Method: {}", 
            request.getRequestURI(), request.getMethod());
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
