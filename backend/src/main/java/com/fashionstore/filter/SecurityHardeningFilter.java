package com.fashionstore.filter;

import com.fashionstore.security.SecurityRequestUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Base64;

/**
 * SecurityHardeningFilter - Rate Limiting & Attack Prevention
 * 
 * FILTER CHAIN ARCHITECTURE:
 * ==========================
 * Filter execution order (from web.xml):
 * 1. RequestLoggingFilter - Adds request ID to MDC
 * 2. CORSFilter - Handles CORS preflight (OPTIONS)
 * 3. SecurityHardeningFilter - Rate limiting, attack prevention (THIS FILTER)
 * 4. JWTAuthenticationFilter - JWT validation for /api/admin/*
 * 5. Servlet/Controller - Handles request
 * 
 * CRITICAL SEPARATION:
 * ====================
 * This filter handles:
 * ✓ Rate limiting (max requests per minute)
 * ✓ Brute-force protection (login attempts)
 * ✓ Attack pattern detection (suspicious paths)
 * ✓ Request size validation
 * ✓ Security headers (X-Content-Type-Options, etc.)
 * ✓ Session cookie configuration
 * 
 * This filter does NOT handle:
 * ✗ Authentication (JWTAuthenticationFilter)
 * ✗ Authorization (JWTAuthenticationFilter)
 * ✗ CORS (CORSFilter)
 * ✗ Request logging (RequestLoggingFilter)
 * ✗ JWT validation (JWTAuthenticationFilter)
 * ✗ CSRF validation (handled by CSRFProtection in controllers)
 * 
 * APPLIES TO:
 * ===========
 * - ALL requests (rate limiting applies globally)
 * - JSP pages and MVC controllers
 * - Customer APIs (/api/*)
 * - Admin APIs (/api/admin/*)
 * - Static assets
 * 
 * DOES NOT BLOCK:
 * ===============
 * - OPTIONS requests (handled by CORSFilter)
 * - Whitelisted paths (/api/admin/login, /api/admin/register)
 * - Static assets (if rate limit not exceeded)
 * 
 * PERFORMANCE NOTES:
 * ==================
 * - Uses ConcurrentHashMap for thread-safe tracking
 * - Cleanup thread removes expired trackers every 5 minutes
 * - Minimal overhead per request (map lookup + counter increment)
 * - No blocking operations in request path
 */
public class SecurityHardeningFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityHardeningFilter.class);
    
    // Rate limiting configurations
    private static final int MAX_REQUESTS_PER_MINUTE = 1000;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_LOCKOUT_MINUTES = 15;
    
    // Rate limiting enabled flag
    private boolean rateLimitEnabled = true;
    
    // Secure random for nonce generation
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // Rate limiting storage
    private final Map<String, RequestTracker> requestTrackers = new ConcurrentHashMap<>();
    private final Map<String, LoginTracker> loginTrackers = new ConcurrentHashMap<>();
    private final Map<String, FailedRequestTracker> failedRequestTrackers = new ConcurrentHashMap<>();
    
    // Suspicious activity patterns
    private static final Set<String> SUSPICIOUS_PATHS = Set.of(
        "/admin", "/api/internal", "/system", "/config"
    );
    
    private static final Set<String> WHITELISTED_PATHS = Set.of(
        "/api/metrics",
        "/api/admin/login",
        "/api/admin/register"
    );
    
    private static final Set<String> SENSITIVE_OPERATIONS = Set.of(
        "DELETE", "PUT", "PATCH", "admin", "delete", "update", "modify"
    );
    
    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("SecurityHardeningFilter initialized");
        
        // Check if rate limiting is enabled via environment variable
        String rateLimitEnv = System.getenv("RATE_LIMIT_ENABLED");
        if (rateLimitEnv != null && rateLimitEnv.equalsIgnoreCase("false")) {
            rateLimitEnabled = false;
            logger.info("Rate limiting disabled via RATE_LIMIT_ENABLED=false");
        }
        
        // Check if demo mode is enabled
        String demoMode = System.getenv("DEMO_MODE");
        if (demoMode != null && demoMode.equalsIgnoreCase("true")) {
            rateLimitEnabled = false;
            logger.info("Rate limiting disabled via DEMO_MODE=true");
        }
        
        // Start cleanup thread for expired trackers
        startCleanupThread();
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // Configure session cookies based on environment
            configureSessionCookies(httpRequest, httpResponse);
            
            // Apply security hardening
            if (!applySecurityMeasures(httpRequest, httpResponse)) {
                return; // Security measure blocked the request
            }
            
            // Continue with the request
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Security filter error: {}", e.getMessage(), e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Configure session cookies based on environment
     * - Production/HTTPS: Secure=true, SameSite=Strict
     * - Development/localhost: Secure=false, SameSite=Lax
     */
    private void configureSessionCookies(HttpServletRequest request, HttpServletResponse response) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        boolean isHttps = "https".equalsIgnoreCase(scheme);
        boolean isLocalhost = serverName.equals("localhost") || serverName.equals("127.0.0.1");
        boolean isDevelopment = isLocalhost || !isHttps;
        
        // Set secure flag based on environment
        boolean secureFlag = isHttps && !isLocalhost;
        
        // Set SameSite based on environment
        String sameSiteValue = isDevelopment ? "Lax" : "Strict";
        
        // Set cookie headers
        String cookieHeader = String.format(
            "JSESSIONID=%s; Path=/; %s; SameSite=%s; HttpOnly",
            request.getSession(false) != null ? request.getSession(false).getId() : "",
            secureFlag ? "Secure" : "",
            sameSiteValue
        );
        
        // Add Set-Cookie header for new sessions
        HttpSession session = request.getSession(false);
        if (session != null && session.isNew()) {
            response.setHeader("Set-Cookie", cookieHeader);
        }
    }
    
    /**
     * Apply comprehensive security measures
     * 
     * IMPORTANT: This filter focuses on:
     * - Rate limiting
     * - Brute-force protection
     * - Attack pattern detection
     * - Security headers
     * - Request size validation
     * 
     * This filter does NOT handle:
     * - Session validation (let JWTAuthenticationFilter handle JWT)
     * - Authentication (handled by JWTAuthenticationFilter)
     * - Authorization (handled by JWTAuthenticationFilter)
     */
    private boolean applySecurityMeasures(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        String clientIP = SecurityRequestUtil.getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // 1. Rate limiting check (skip if disabled)
        if (rateLimitEnabled && !checkRateLimit(clientIP, path, method)) {
            logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIP, path);
            response.sendError(429, "Rate limit exceeded");
            return false;
        }
        
        // 2. Brute-force protection for login attempts (only on POST requests)
        // Skip for whitelisted login endpoints
        if ("POST".equalsIgnoreCase(method) && (path.contains("/login") || path.contains("/auth")) &&
            !WHITELISTED_PATHS.stream().anyMatch(path::contains)) {
            if (!checkBruteForceProtection(clientIP, userAgent)) {
                logger.warn("Brute force protection triggered for IP: {}", clientIP);
                response.sendError(429, "Too many login attempts");
                return false;
            }
        }
        
        // 3. Suspicious activity detection
        if (detectSuspiciousActivity(request, clientIP, userAgent)) {
            logger.warn("Suspicious activity detected from IP: {} on path: {}", clientIP, path);
            // Log suspicious activity but don't block immediately
            trackSuspiciousActivity(clientIP, path, userAgent);
        }
        
        // 4. Apply security headers
        applySecurityHeaders(request, response);
        
        // 5. Check for common attack patterns
        if (detectAttackPatterns(request)) {
            logger.warn("Attack pattern detected from IP: {} on path: {}", clientIP, path);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return false;
        }
        
        // 6. Validate request size
        if (!validateRequestSize(request)) {
            logger.warn("Request size exceeded from IP: {}", clientIP);
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request too large");
            return false;
        }
        
        return true;
    }
    
    /**
     * Rate limiting implementation
     */
    private boolean checkRateLimit(String clientIP, String path, String method) {
        String key = clientIP + ":" + path;
        RequestTracker tracker = requestTrackers.computeIfAbsent(key, k -> new RequestTracker());
        
        return tracker.allowRequest();
    }
    
    /**
     * Brute-force protection for login attempts
     */
    private boolean checkBruteForceProtection(String clientIP, String userAgent) {
        String key = clientIP + ":" + userAgent;
        LoginTracker tracker = loginTrackers.computeIfAbsent(key, k -> new LoginTracker());
        
        return tracker.allowLoginAttempt();
    }
    
    /**
     * Detect suspicious activity patterns
     */
    private boolean detectSuspiciousActivity(HttpServletRequest request, String clientIP, String userAgent) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip suspicious activity detection for whitelisted paths
        if (WHITELISTED_PATHS.stream().anyMatch(path::contains)) {
            return false;
        }
        
        // Check for suspicious paths
        boolean suspiciousPath = SUSPICIOUS_PATHS.stream().anyMatch(path::contains);
        
        // Check for sensitive operations on non-admin users
        boolean sensitiveOp = SENSITIVE_OPERATIONS.stream()
            .anyMatch(op -> path.toLowerCase().contains(op.toLowerCase()) || method.equalsIgnoreCase(op));
        
        // Check for unusual user agents
        boolean suspiciousUA = userAgent == null || userAgent.isEmpty() || 
            userAgent.length() < 10 || userAgent.contains("bot") || userAgent.contains("crawler");
        
        // Check for rapid requests to sensitive areas
        FailedRequestTracker failedTracker = failedRequestTrackers.computeIfAbsent(clientIP, k -> new FailedRequestTracker());
        boolean highFailureRate = failedTracker.getFailureRate() > 0.5; // More than 50% failure rate
        
        return suspiciousPath || (sensitiveOp && suspiciousUA) || highFailureRate;
    }
    
    /**
     * Track suspicious activity
     */
    private void trackSuspiciousActivity(String clientIP, String path, String userAgent) {
        FailedRequestTracker tracker = failedRequestTrackers.computeIfAbsent(clientIP, k -> new FailedRequestTracker());
        tracker.recordSuspiciousActivity(path, userAgent);
        
        // Log to audit system
        logger.info("Suspicious activity logged - IP: {}, Path: {}, UA: {}", clientIP, path, userAgent);
    }
    
    /**
     * Generate a cryptographically secure nonce for CSP
     */
    private String generateNonce() {
        byte[] nonceBytes = new byte[16];
        secureRandom.nextBytes(nonceBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
    }
    
    /**
     * Apply security headers
     */
    private void applySecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        // Generate a nonce for this request
        String nonce = generateNonce();
        
        // Store nonce in request attribute for use in JSP
        request.setAttribute("cspNonce", nonce);
        
        // Content Security Policy - Production-grade without unsafe-inline
        // SECURITY FIX: Removed 'unsafe-inline' and replaced with nonce-based CSP
        // Exploit scenario: Attacker injects malicious script via XSS vulnerability
        // Impact: Cross-site scripting attacks can steal cookies, session tokens, or redirect users
        // Remediation: Use nonce-based CSP for legitimate inline scripts only
        String cspPolicy = 
            "default-src 'self'; " +
            "script-src 'self' 'nonce-" + nonce + "' https://js.stripe.com https://cdn.jsdelivr.net https://www.gstatic.com; " +
            "style-src 'self' 'nonce-" + nonce + "' https://fonts.googleapis.com; " +
            "font-src 'self' https://fonts.gstatic.com; " +
            "img-src 'self' data: https: https://*.stripe.com; " +
            "connect-src 'self' https://api.stripe.com https://js.stripe.com; " +
            "frame-src 'self' https://js.stripe.com https://hooks.stripe.com; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'; " +
            "report-uri /csp-violation-report";
        
        response.setHeader("Content-Security-Policy", cspPolicy);
        
        // Other security headers
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=(), payment=(), usb=(), interest-cohort=()");
        
        // HSTS (only in production)
        if (isProductionEnvironment()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }
        
        // Remove server information
        response.setHeader("Server", "");
    }
    
    /**
     * Detect common attack patterns
     */
    private boolean detectAttackPatterns(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String path = request.getRequestURI();
        
        // Check for SQL injection patterns
        if (queryString != null && containsSQLInjection(queryString)) {
            return true;
        }
        
        // Check for XSS patterns
        if (queryString != null && containsXSS(queryString)) {
            return true;
        }
        
        // Check for path traversal
        if (containsPathTraversal(path)) {
            return true;
        }
        
        // Check for command injection
        if (queryString != null && containsCommandInjection(queryString)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Validate request size
     */
    private boolean validateRequestSize(HttpServletRequest request) {
        int contentLength = request.getContentLength();
        
        // Limit request size to 10MB
        int maxRequestSize = 10 * 1024 * 1024;
        
        return contentLength <= maxRequestSize;
    }
    
    /**
     * SQL injection detection
     */
    private boolean containsSQLInjection(String input) {
        String[] sqlPatterns = {
            "('('|.*'|')|;|\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT|SELECT|UNION|UPDATE)\\b)",
            "(\\b(OR|AND)\\b\\s+\\d+\\s*=\\s*\\d+)",
            "(\\b(OR|AND)\\b\\s+['\"]?\\w+['\"]?\\s*=\\s*['\"]?\\w+['\"]?)",
            "(--|#|\\/\\*|\\*\\/)",
            "(\\b(SCRIPT|JAVASCRIPT|VBSCRIPT|ONLOAD|ONERROR)\\b)"
        };
        
        String upperInput = input.toUpperCase();
        for (String pattern : sqlPatterns) {
            if (upperInput.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * XSS detection
     */
    private boolean containsXSS(String input) {
        String[] xssPatterns = {
            "(<script[^>]*>.*?</script>)",
            "(javascript:)",
            "(on\\w+\\s*=)",
            "(<iframe[^>]*>)",
            "(<object[^>]*>)",
            "(<embed[^>]*>)",
            "(<link[^>]*>)",
            "(<meta[^>]*>)",
            "(<style[^>]*>.*?</style>)",
            "(<img[^>]*on\\w+[^>]*>)"
        };
        
        for (String pattern : xssPatterns) {
            if (input.toLowerCase().matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Path traversal detection
     */
    private boolean containsPathTraversal(String path) {
        String[] traversalPatterns = {
            "(\\.\\.)",
            "(/|\\\\)etc(/|\\\\)passwd",
            "(/|\\\\)bin(/|\\\\)",
            "(/|\\\\)usr(/|\\\\)",
            "(%2e%2e)",
            "(%2f)",
            "(%5c)"
        };
        
        for (String pattern : traversalPatterns) {
            if (path.toLowerCase().matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Command injection detection
     */
    private boolean containsCommandInjection(String input) {
        String[] commandPatterns = {
            "(;|\\||&)",
            "(\\$\\()",
            "(\\`[^\\`]*\\`)",
            "(\\$\\{[^}]*\\})",
            "(\\b(curl|wget|nc|netcat|telnet|ssh|ftp)\\b)",
            "(\\b(rm|mv|cp|chmod|chown)\\b)"
        };
        
        for (String pattern : commandPatterns) {
            if (input.toLowerCase().matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if running in production environment
     */
    private boolean isProductionEnvironment() {
        String env = System.getProperty("spring.profiles.active", "development");
        String envVar = System.getenv("SPRING_PROFILES_ACTIVE");
        if (envVar != null) {
            env = envVar;
        }
        return "production".equals(env);
    }
    
    /**
     * Start cleanup thread for expired trackers
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Clean up every minute
                    cleanupExpiredTrackers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        cleanupThread.setDaemon(true);
        cleanupThread.setName("SecurityTrackerCleanup");
        cleanupThread.start();
    }
    
    /**
     * Clean up expired trackers
     */
    private void cleanupExpiredTrackers() {
        long now = System.currentTimeMillis();
        
        // Clean up request trackers
        requestTrackers.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(now));
        
        // Clean up login trackers
        loginTrackers.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(now));
        
        // Clean up failed request trackers
        failedRequestTrackers.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(now));
    }
    
    /**
     * Request tracker for rate limiting
     */
    private static class RequestTracker {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile long lastReset = System.currentTimeMillis();
        private static final long RESET_INTERVAL = 60 * 1000; // 1 minute
        
        public boolean allowRequest() {
            long now = System.currentTimeMillis();
            
            if (now - lastReset > RESET_INTERVAL) {
                synchronized (this) {
                    if (now - lastReset > RESET_INTERVAL) {
                        requestCount.set(0);
                        lastReset = now;
                    }
                }
            }
            
            return requestCount.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
        }
        
        public boolean isExpired(long now) {
            return now - lastReset > RESET_INTERVAL * 5; // Expire after 5 minutes
        }
    }
    
    /**
     * Login tracker for brute-force protection
     */
    private static class LoginTracker {
        private final AtomicInteger attemptCount = new AtomicInteger(0);
        private volatile long lockoutUntil = 0;
        private volatile long lastAttempt = System.currentTimeMillis();
        
        public boolean allowLoginAttempt() {
            long now = System.currentTimeMillis();
            
            if (now < lockoutUntil) {
                return false; // Still locked out
            }
            
            lastAttempt = now;
            int attempts = attemptCount.incrementAndGet();
            
            if (attempts > MAX_LOGIN_ATTEMPTS) {
                lockoutUntil = now + (LOGIN_LOCKOUT_MINUTES * 60 * 1000);
                attemptCount.set(0); // Reset after lockout
                return false;
            }
            
            return true;
        }
        
        public boolean isExpired(long now) {
            return now - lastAttempt > 24 * 60 * 60 * 1000; // Expire after 24 hours
        }
    }
    
    /**
     * Failed request tracker for suspicious activity detection
     */
    private static class FailedRequestTracker {
        private final AtomicInteger totalRequests = new AtomicInteger(0);
        private final AtomicInteger failedRequests = new AtomicInteger(0);
        private volatile long lastActivity = System.currentTimeMillis();
        private final List<String> suspiciousActivities = new ArrayList<>();
        
        public void recordSuspiciousActivity(String path, String userAgent) {
            totalRequests.incrementAndGet();
            failedRequests.incrementAndGet();
            lastActivity = System.currentTimeMillis();
            
            // Keep only recent activities
            synchronized (suspiciousActivities) {
                suspiciousActivities.add(path + "|" + userAgent);
                if (suspiciousActivities.size() > 100) {
                    suspiciousActivities.remove(0);
                }
            }
        }
        
        public double getFailureRate() {
            int total = totalRequests.get();
            int failed = failedRequests.get();
            return total > 0 ? (double) failed / total : 0.0;
        }
        
        public boolean isExpired(long now) {
            return now - lastActivity > 60 * 60 * 1000; // Expire after 1 hour
        }
    }
    
    @Override
    public void destroy() {
        logger.info("SecurityHardeningFilter destroyed");
        // Cleanup resources
        requestTrackers.clear();
        loginTrackers.clear();
        failedRequestTrackers.clear();
    }
}
