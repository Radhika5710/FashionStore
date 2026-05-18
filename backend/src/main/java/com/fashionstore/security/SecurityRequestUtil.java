package com.fashionstore.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Security Request Utilities
 * 
 * PURPOSE:
 * =======
 * Shared utility methods for extracting security-relevant information from HTTP requests.
 * This class consolidates common request parsing logic used across multiple security components.
 * 
 * USAGE:
 * ======
 * Use this class for extracting request metadata like client IP address, user agent, etc.
 * This ensures consistent behavior across all security filters and utilities.
 */
public class SecurityRequestUtil {
    
    /**
     * Get client IP address from request
     * 
     * Handles proxy scenarios by checking X-Forwarded-For and X-Real-IP headers.
     * Returns the original client IP when behind a load balancer or proxy.
     * 
     * Priority order:
     * 1. X-Forwarded-For header (first IP in comma-separated list)
     * 2. X-Real-IP header
     * 3. Remote address (direct connection)
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    public static String getClientIP(HttpServletRequest request) {
        // Check X-Forwarded-For header (load balancer/proxy)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP header (nginx/proxy)
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }
    
    /**
     * Get user agent from request
     * 
     * @param request HTTP request
     * @return User agent string or null if not present
     */
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
    
    /**
     * Check if request is from localhost
     * 
     * @param request HTTP request
     * @return true if request is from localhost or 127.0.0.1
     */
    public static boolean isLocalhost(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return "127.0.0.1".equals(remoteAddr) || 
               "::1".equals(remoteAddr) || 
               "localhost".equalsIgnoreCase(remoteAddr);
    }
    
    /**
     * Check if request is using HTTPS
     * 
     * @param request HTTP request
     * @return true if request is secure (HTTPS)
     */
    public static boolean isSecure(HttpServletRequest request) {
        return request.isSecure() || 
               "https".equalsIgnoreCase(request.getScheme()) ||
               "on".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }
}
