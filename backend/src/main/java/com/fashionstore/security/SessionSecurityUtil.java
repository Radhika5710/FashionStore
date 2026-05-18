package com.fashionstore.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Session Security Utilities - Minimal Session Validation
 * 
 * PURPOSE:
 * =======
 * Provides basic session security validation for customer MVC authentication.
 * Validates session expiration and inactivity only.
 * 
 * USAGE:
 * ======
 * - Use SecurityUtil for simple session attribute retrieval (getAuthenticatedCustomer, isAuthenticated)
 * - Use SessionSecurityUtil for session validation (expiration, inactivity)
 * - Use AuthContext for JWT-based authentication (admin APIs only)
 * 
 * SEPARATION OF CONCERNS:
 * ========================
 * - SecurityUtil: Simple session attribute checks (used by controllers)
 * - SessionSecurityUtil: Basic session validation (expiration, inactivity)
 * - AuthContext: JWT-based authentication (used by admin APIs only)
 * 
 * EXAMPLE:
 * ========
 * // Simple attribute check (controller level)
 * User customer = SecurityUtil.getAuthenticatedCustomer(request);
 * 
 * // Basic session validation (filter level)
 * SessionValidationResult result = SessionSecurityUtil.validateSession(request);
 * if (!result.isValid()) {
 *     // Handle invalid session (expired or inactive)
 * }
 */
public class SessionSecurityUtil {
    private static final Logger logger = LoggerFactory.getLogger(SessionSecurityUtil.class);
    
    // Session configuration
    private static final int MAX_SESSION_AGE_SECONDS = 30 * 60; // 30 minutes
    private static final int SESSION_INACTIVITY_TIMEOUT_SECONDS = 15 * 60; // 15 minutes
    
    /**
     * Create secure session with protection against session fixation
     */
    public static HttpSession createSecureSession(HttpServletRequest request) {
        // Invalidate existing session if present
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            logger.info("Invalidating existing session before creating secure session: {}", oldSession.getId());
            oldSession.invalidate();
        }
        
        // Create new session
        HttpSession newSession = request.getSession(true);
        
        // Set session security attributes
        initializeSessionAttributes(newSession, request);
        
        logger.info("Secure session created with all attributes: {}", newSession.getId());
        
        return newSession;
    }
    
    /**
     * Initialize session security attributes
     */
    private static void initializeSessionAttributes(HttpSession session, HttpServletRequest request) {
        session.setMaxInactiveInterval(MAX_SESSION_AGE_SECONDS);
        
        session.setAttribute("sessionCreationTime", System.currentTimeMillis());
        session.setAttribute("sessionLastAccessedTime", System.currentTimeMillis());
    }
    
    /**
     * Validate session security
     */
    public static SessionValidationResult validateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String path = request.getRequestURI();
        
        if (session == null) {
            SessionValidationResult result = new SessionValidationResult();
            result.setValid(false);
            result.setReason("No active session");
            return result;
        }
        
        // Check if this is an admin session
        Object adminAuth = session.getAttribute("adminAuth");
        boolean isAdminSession = (adminAuth != null);
        
        // Route to appropriate validator
        if (isAdminSession && (path.startsWith("/api/admin") || path.startsWith("/admin"))) {
            return validateAdminSession(request);
        } else {
            return validateUserSession(request);
        }
    }
    
    /**
     * Validate user session with basic security checks
     */
    public static SessionValidationResult validateUserSession(HttpServletRequest request) {
        SessionValidationResult result = new SessionValidationResult();
        HttpSession session = request.getSession(false);
        String path = request.getRequestURI();
        
        if (session == null) {
            result.setValid(false);
            result.setReason("No active session");
            return result;
        }
        
        // Skip strict validation for login endpoints
        if (path.equals("/api/auth/login") || path.equals("/login")) {
            result.setValid(true);
            result.setSessionId(session.getId());
            return result;
        }
        
        // Validate session metadata (expiration and inactivity only)
        SessionValidationResult metadataResult = validateSessionMetadata(session, request);
        if (!metadataResult.isValid()) {
            result.setValid(false);
            result.setReason(metadataResult.getReason());
            logger.warn("validateUserSession failed: {} - Path: {}, Session ID: {}", 
                metadataResult.getReason(), path, session.getId());
            return result;
        }
        
        result.setValid(true);
        result.setSessionId(session.getId());
        
        return result;
    }
    
    /**
     * Validate admin session with admin-specific checks
     */
    public static SessionValidationResult validateAdminSession(HttpServletRequest request) {
        SessionValidationResult result = new SessionValidationResult();
        HttpSession session = request.getSession(false);
        String path = request.getRequestURI();
        
        if (session == null) {
            result.setValid(false);
            result.setReason("No active session");
            return result;
        }
        
        // Skip strict validation for admin login/register endpoints
        if (path.equals("/api/admin/login") || path.equals("/api/admin/register") || path.equals("/admin/login")) {
            result.setValid(true);
            result.setSessionId(session.getId());
            return result;
        }
        
        // Check adminAuth attribute exists
        Object adminAuth = session.getAttribute("adminAuth");
        if (adminAuth == null) {
            result.setValid(false);
            result.setReason("Admin authentication not found in session");
            logger.warn("validateAdminSession failed: adminAuth not in session - Path: {}, Session ID: {}", 
                path, session.getId());
            return result;
        }
        
        // Validate session metadata (expiration and inactivity only)
        SessionValidationResult metadataResult = validateSessionMetadata(session, request);
        if (!metadataResult.isValid()) {
            result.setValid(false);
            result.setReason(metadataResult.getReason());
            logger.warn("validateAdminSession failed: {} - Path: {}, Session ID: {}", 
                metadataResult.getReason(), path, session.getId());
            return result;
        }
        
        result.setValid(true);
        result.setSessionId(session.getId());
        
        return result;
    }
    
    /**
     * Validate session metadata (creation time, expiration, IP, User-Agent)
     */
    public static SessionValidationResult validateSessionMetadata(HttpSession session, HttpServletRequest request) {
        SessionValidationResult result = new SessionValidationResult();
        String path = request.getRequestURI();
        
        // Check session creation time
        Long creationTime = (Long) session.getAttribute("sessionCreationTime");
        if (creationTime == null) {
            result.setValid(false);
            result.setReason("Session creation time not set");
            logger.warn("validateSessionMetadata failed: Session creation time not set - Path: {}", path);
            return result;
        }
        
        // Check session age
        long sessionAge = System.currentTimeMillis() - creationTime;
        long maxAge = MAX_SESSION_AGE_SECONDS * 1000L;
        
        if (sessionAge > maxAge) {
            result.setValid(false);
            result.setReason("Session expired due to age");
            session.invalidate();
            logger.warn("validateSessionMetadata failed: Session expired due to age - Path: {}, Session age: {}ms", 
                path, sessionAge);
            return result;
        }
        
        // Check session inactivity
        Long lastAccessedTime = (Long) session.getAttribute("sessionLastAccessedTime");
        if (lastAccessedTime != null) {
            long inactivity = System.currentTimeMillis() - lastAccessedTime;
            long maxInactivity = SESSION_INACTIVITY_TIMEOUT_SECONDS * 1000L;
            
            if (inactivity > maxInactivity) {
                result.setValid(false);
                result.setReason("Session expired due to inactivity");
                session.invalidate();
                logger.warn("validateSessionMetadata failed: Session expired due to inactivity - Path: {}, Inactivity: {}ms", 
                    path, inactivity);
                return result;
            }
        }
        
        // Update last accessed time
        session.setAttribute("sessionLastAccessedTime", System.currentTimeMillis());
        
        result.setValid(true);
        
        return result;
    }
    
    /**
     * Invalidate session
     */
    public static void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            logger.info("Session invalidated: {}", session.getId());
        }
    }
    
    /**
     * Session validation result class
     */
    public static class SessionValidationResult {
        private boolean valid;
        private String reason;
        private String sessionId;
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}
