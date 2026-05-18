package com.fashionstore.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Security-focused logger for audit trails and security monitoring
 * Provides comprehensive security event logging with threat detection
 */
public class SecurityLogger {

    private static final SecurityLogger INSTANCE = new SecurityLogger();
    private final EnterpriseLogger enterpriseLogger;
    
    // Security metrics
    private final Map<String, AtomicLong> securityEvents = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> ipRequests = new ConcurrentHashMap<>();
    private final Map<String, Long> lastFailedLogin = new ConcurrentHashMap<>();
    
    // Security thresholds
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final long LOGIN_ATTEMPT_WINDOW = 15 * 60 * 1000; // 15 minutes
    
    private SecurityLogger() {
        this.enterpriseLogger = EnterpriseLogger.getLogger(SecurityLogger.class);
    }
    
    public static SecurityLogger getInstance() {
        return INSTANCE;
    }
    
    /**
     * Log authentication event with security analysis
     */
    public void logAuthentication(String eventType, String userId, String email, boolean success, 
                                 String reason, String ipAddress, String userAgent, HttpServletRequest request) {
        
        // Update security metrics
        updateSecurityMetrics(eventType, userId, ipAddress, success);
        
        // Check for suspicious patterns
        SecurityLevel securityLevel = analyzeSecurityEvent(eventType, userId, ipAddress, success, reason);
        
        // Build security metadata
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        metadata.put("ipAddress", ipAddress);
        metadata.put("userAgent", userAgent);
        metadata.put("securityLevel", securityLevel.name());
        metadata.put("riskScore", calculateRiskScore(eventType, userId, ipAddress, success));
        
        if (request != null) {
            metadata.put("requestUri", request.getRequestURI());
            metadata.put("requestMethod", request.getMethod());
        }
        
        // Add geolocation data (simplified)
        metadata.put("country", getCountryFromIP(ipAddress));
        
        // Log with appropriate severity
        String severity = determineSeverity(eventType, success, securityLevel);
        
        enterpriseLogger.logSecurityEvent(
            eventType,
            severity,
            userId,
            ipAddress,
            reason,
            metadata
        );
        
        // Trigger alerts for high-risk events
        if (securityLevel == SecurityLevel.HIGH || securityLevel == SecurityLevel.CRITICAL) {
            triggerSecurityAlert(eventType, userId, ipAddress, securityLevel, metadata);
        }
    }
    
    /**
     * Log authorization event
     */
    public void logAuthorization(String eventType, String userId, String resource, String action, 
                              boolean success, String ipAddress, HttpServletRequest request) {
        
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        metadata.put("resource", resource);
        metadata.put("action", action);
        metadata.put("ipAddress", ipAddress);
        metadata.put("success", String.valueOf(success));
        
        if (request != null) {
            metadata.put("requestUri", request.getRequestURI());
            metadata.put("requestMethod", request.getMethod());
            metadata.put("queryString", request.getQueryString());
        }
        
        String severity = success ? "LOW" : "MEDIUM";
        if ("ADMIN".equals(resource) && !success) {
            severity = "HIGH";
        }
        
        enterpriseLogger.logSecurityEvent(
            eventType,
            severity,
            userId,
            ipAddress,
            "Authorization " + (success ? "granted" : "denied") + " for " + action + " on " + resource,
            metadata
        );
    }
    
    /**
     * Log data access event
     */
    public void logDataAccess(String eventType, String userId, String dataType, String recordId, 
                             boolean success, String ipAddress) {
        
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        metadata.put("dataType", dataType);
        metadata.put("recordId", recordId);
        metadata.put("ipAddress", ipAddress);
        metadata.put("success", String.valueOf(success));
        
        String severity = "LOW";
        if ("SENSITIVE".equals(dataType)) {
            severity = success ? "MEDIUM" : "HIGH";
        }
        
        enterpriseLogger.logSecurityEvent(
            eventType,
            severity,
            userId,
            ipAddress,
            "Data access " + (success ? "granted" : "denied") + " for " + dataType + ":" + recordId,
            metadata
        );
    }
    
    /**
     * Log payment security event
     */
    public void logPaymentSecurity(String eventType, String userId, String orderId, String paymentMethod,
                                   double amount, boolean success, String ipAddress, String riskFactors) {
        
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        metadata.put("orderId", orderId);
        metadata.put("paymentMethod", paymentMethod);
        metadata.put("amount", amount);
        metadata.put("ipAddress", ipAddress);
        metadata.put("success", String.valueOf(success));
        metadata.put("riskFactors", riskFactors);
        
        // Payment events are always at least MEDIUM severity
        String severity = "MEDIUM";
        if (!success || amount > 1000) {
            severity = "HIGH";
        }
        if (amount > 5000) {
            severity = "CRITICAL";
        }
        
        enterpriseLogger.logSecurityEvent(
            eventType,
            severity,
            userId,
            ipAddress,
            "Payment security event: " + eventType + " for order " + orderId,
            metadata
        );
    }
    
    /**
     * Log admin action with security context
     */
    public void logAdminAction(String action, String target, String userId, String ipAddress,
                              String details, HttpServletRequest request) {
        
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        metadata.put("action", action);
        metadata.put("target", target);
        metadata.put("ipAddress", ipAddress);
        metadata.put("details", details);
        
        if (request != null) {
            metadata.put("requestUri", request.getRequestURI());
            metadata.put("requestMethod", request.getMethod());
        }
        
        // Admin actions are always at least MEDIUM severity
        String severity = "MEDIUM";
        if ("DELETE".equals(action) || "BULK_UPDATE".equals(action)) {
            severity = "HIGH";
        }
        
        enterpriseLogger.logSecurityEvent(
            "ADMIN_ACTION",
            severity,
            userId,
            ipAddress,
            "Admin action performed: " + action + " on " + target,
            metadata
        );
    }
    
    /**
     * Log session security event
     */
    public void logSessionSecurity(String eventType, String userId, String sessionId, 
                                  String ipAddress, String details) {
        
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        metadata.put("sessionId", sessionId);
        metadata.put("ipAddress", ipAddress);
        metadata.put("details", details);
        
        String severity = "LOW";
        if ("SESSION_HIJACK".equals(eventType) || "SESSION_FIXATION".equals(eventType)) {
            severity = "CRITICAL";
        } else if ("UNUSUAL_ACTIVITY".equals(eventType)) {
            severity = "HIGH";
        }
        
        enterpriseLogger.logSecurityEvent(
            eventType,
            severity,
            userId,
            ipAddress,
            "Session security event: " + eventType,
            metadata
        );
    }
    
    /**
     * Log API security event
     */
    public void logApiSecurity(String eventType, String apiKey, String endpoint, String method,
                              boolean success, String ipAddress, String userAgent) {
        
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        metadata.put("endpoint", endpoint);
        metadata.put("method", method);
        metadata.put("ipAddress", ipAddress);
        metadata.put("userAgent", userAgent);
        metadata.put("success", String.valueOf(success));
        
        String severity = success ? "LOW" : "MEDIUM";
        if ("ADMIN".equals(endpoint) || !success) {
            severity = "HIGH";
        }
        
        enterpriseLogger.logSecurityEvent(
            eventType,
            severity,
            null, // API key users might not have traditional user IDs
            ipAddress,
            "API security event: " + eventType + " for " + method + " " + endpoint,
            metadata
        );
    }
    
    /**
     * Check if IP is blocked
     */
    public boolean isIpBlocked(String ipAddress) {
        AtomicLong requestCount = ipRequests.get(ipAddress);
        return requestCount != null && requestCount.get() > MAX_REQUESTS_PER_MINUTE;
    }
    
    /**
     * Check if user has exceeded login attempts
     */
    public boolean hasExceededLoginAttempts(String userId, String ipAddress) {
        String key = userId + ":" + ipAddress;
        Long lastFailure = lastFailedLogin.get(key);
        
        if (lastFailure != null) {
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailure;
            if (timeSinceLastFailure < LOGIN_ATTEMPT_WINDOW) {
                AtomicLong attempts = securityEvents.get("LOGIN_FAILED:" + key);
                return attempts != null && attempts.get() >= MAX_LOGIN_ATTEMPTS;
            }
        }
        
        return false;
    }
    
    /**
     * Get security metrics
     */
    public Map<String, Object> getSecurityMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        
        metrics.put("totalSecurityEvents", securityEvents.values().stream().mapToLong(AtomicLong::get).sum());
        metrics.put("blockedIPs", ipRequests.entrySet().stream().filter(entry -> entry.getValue().get() > MAX_REQUESTS_PER_MINUTE).count());
        metrics.put("highRiskEvents", securityEvents.entrySet().stream().filter(entry -> entry.getKey().contains("HIGH")).count());
        metrics.put("criticalEvents", securityEvents.entrySet().stream().filter(entry -> entry.getKey().contains("CRITICAL")).count());
        
        return metrics;
    }
    
    // Private helper methods
    
    private void updateSecurityMetrics(String eventType, String userId, String ipAddress, boolean success) {
        String eventKey = eventType + ":" + (userId != null ? userId : ipAddress);
        securityEvents.computeIfAbsent(eventKey, k -> new AtomicLong(0)).incrementAndGet();
        
        // Track IP requests
        ipRequests.computeIfAbsent(ipAddress, k -> new AtomicLong(0)).incrementAndGet();
        
        // Track failed logins
        if ("LOGIN_FAILED".equals(eventType)) {
            String key = (userId != null ? userId : "") + ":" + ipAddress;
            lastFailedLogin.put(key, System.currentTimeMillis());
        }
    }
    
    private SecurityLevel analyzeSecurityEvent(String eventType, String userId, String ipAddress, 
                                            boolean success, String reason) {
        
        // Critical events
        if ("SESSION_HIJACK".equals(eventType) || "DATA_BREACH".equals(eventType)) {
            return SecurityLevel.CRITICAL;
        }
        
        // High risk events
        if (!success && ("LOGIN_FAILED".equals(eventType) || "ADMIN_ACCESS_DENIED".equals(eventType))) {
            if (hasExceededLoginAttempts(userId, ipAddress)) {
                return SecurityLevel.CRITICAL;
            }
            return SecurityLevel.HIGH;
        }
        
        // Medium risk events
        if ("UNUSUAL_ACTIVITY".equals(eventType) || "PAYMENT_SECURITY".equals(eventType)) {
            return SecurityLevel.MEDIUM;
        }
        
        // Low risk events
        return SecurityLevel.LOW;
    }
    
    private int calculateRiskScore(String eventType, String userId, String ipAddress, boolean success) {
        int score = 0;
        
        // Base score for event type
        switch (eventType) {
            case "LOGIN_SUCCESS": score += 0; break;
            case "LOGIN_FAILED": score += 10; break;
            case "ADMIN_ACCESS_DENIED": score += 15; break;
            case "SESSION_HIJACK": score += 50; break;
            case "DATA_BREACH": score += 50; break;
            default: score += 5;
        }
        
        // Add for failures
        if (!success) {
            score += 10;
        }
        
        // Add for repeated failures
        if (hasExceededLoginAttempts(userId, ipAddress)) {
            score += 20;
        }
        
        // Add for suspicious IP
        if (isIpBlocked(ipAddress)) {
            score += 25;
        }
        
        return Math.min(score, 100);
    }
    
    private String determineSeverity(String eventType, boolean success, SecurityLevel securityLevel) {
        switch (securityLevel) {
            case CRITICAL: return "CRITICAL";
            case HIGH: return "HIGH";
            case MEDIUM: return "MEDIUM";
            case LOW: return success ? "LOW" : "MEDIUM";
            default: return "LOW";
        }
    }
    
    private void triggerSecurityAlert(String eventType, String userId, String ipAddress, 
                                      SecurityLevel securityLevel, Map<String, Object> metadata) {
        
        // Log alert
        enterpriseLogger.logSecurityEvent(
            "SECURITY_ALERT",
            "CRITICAL",
            userId,
            ipAddress,
            "Security alert triggered: " + eventType + " with risk level " + securityLevel,
            metadata
        );
        
        // In a real implementation, this would trigger notifications:
        // - Send email to security team
        // - Create alert in monitoring system
        // - Potentially block IP or user
        // - Send SMS for critical events

        // Log the security alert
        // enterpriseLogger.logSecurityEvent(eventType, securityLevel.name(), ipAddress, null);
        // Method signature mismatch, commenting out for now
        enterpriseLogger.logSecurityEvent(eventType, securityLevel.name(), ipAddress, null, null, new java.util.HashMap<>());
    }
    
    private String getCountryFromIP(String ipAddress) {
        // In a real implementation, this would use a GeoIP service
        // For now, return a placeholder
        return "UNKNOWN";
    }
    
    private enum SecurityLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
