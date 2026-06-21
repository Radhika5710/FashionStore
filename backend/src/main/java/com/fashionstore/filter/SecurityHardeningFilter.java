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
 * SecurityHardeningFilter - Rate Limiting &amp; Attack Prevention
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
 * SKIPS (NEVER BLOCKS):
 * =====================
 * - JSP FORWARD dispatches (RequestDispatcher.forward)
 * - Static assets (*.css, *.js, *.png, *.jpg, *.svg, *.woff, *.ico)
 * - Known storefront routes (/home, /products, /product, /cart, /checkout, /login, /register, /profile, /wishlist, /orders)
 * - Health endpoints (/healthz, /health)
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

    // ---------------------------------------------------------------------------
    // SAFE STOREFRONT ROUTES — attack pattern detection is SKIPPED for these.
    // Any path that starts with one of these prefixes is considered trusted for
    // the purposes of this filter (authentication is handled separately).
    // ---------------------------------------------------------------------------
    private static final Set<String> SAFE_STOREFRONT_PREFIXES = Set.of(
        "/home",
        "/products",
        "/product",
        "/cart",
        "/checkout",
        "/login",
        "/register",
        "/logout",
        "/account",
        "/orders",
        "/wishlist",
        "/profile",
        "/search",
        "/review",
        "/payment",
        "/success",
        "/api/products",
        "/api/categories",
        "/api/search",
        "/api/cart",
        "/api/wishlist",
        "/api/orders",
        "/api/profile",
        "/api/address",
        "/api/reviews",
        "/api/auth",
        "/api/admin/login",
        "/api/admin/register",
        "/api/metrics",
        "/healthz",
        "/health",
        "/"
    );

    // Static asset extensions — skip all processing for these.
    private static final Set<String> STATIC_EXTENSIONS = Set.of(
        ".css", ".js", ".png", ".jpg", ".jpeg", ".svg",
        ".woff", ".woff2", ".ttf", ".eot", ".ico", ".gif",
        ".map", ".webp", ".avif"
    );

    // Paths that are explicitly whitelisted from rate limits and brute-force protection
    private static final Set<String> WHITELISTED_PATHS = Set.of(
        "/api/metrics",
        "/api/admin/login",
        "/api/admin/register",
        "/healthz",
        "/health"
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

        // ----------------------------------------------------------------
        // SKIP JSP FORWARD dispatches — RequestDispatcher.forward() should
        // never be re-inspected by this filter.  The container sets the
        // dispatcher type to FORWARD for those internal dispatches.
        // ----------------------------------------------------------------
        if (httpRequest.getDispatcherType() == DispatcherType.FORWARD
                || httpRequest.getDispatcherType() == DispatcherType.INCLUDE
                || httpRequest.getDispatcherType() == DispatcherType.ERROR) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String relativePath = path.length() > contextPath.length()
                ? path.substring(contextPath.length())
                : "/";

        // ----------------------------------------------------------------
        // SKIP static assets — no processing needed.
        // ----------------------------------------------------------------
        if (isStaticAsset(relativePath)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // Configure session cookies based on environment
            configureSessionCookies(httpRequest, httpResponse);

            // Apply security hardening
            if (!applySecurityMeasures(httpRequest, httpResponse, relativePath)) {
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
     * Returns true if the path points to a static asset that should bypass all
     * security processing (extension-based check).
     */
    private boolean isStaticAsset(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = path.substring(dot).toLowerCase();
        return STATIC_EXTENSIONS.contains(ext);
    }

    /**
     * Returns true if the path is a known-safe storefront route.
     * Attack-pattern detection is disabled for these paths.
     */
    private boolean isSafeStorefrontRoute(String relativePath) {
        for (String prefix : SAFE_STOREFRONT_PREFIXES) {
            if (relativePath.equals(prefix) || relativePath.startsWith(prefix + "/") || relativePath.startsWith(prefix + "?")) {
                return true;
            }
        }
        // Root path
        return relativePath.isEmpty() || relativePath.equals("/");
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

        // Add Set-Cookie header for new sessions
        HttpSession session = request.getSession(false);
        if (session != null && session.isNew()) {
            String cookieHeader = String.format(
                "JSESSIONID=%s; Path=/; %s; SameSite=%s; HttpOnly",
                session.getId(),
                secureFlag ? "Secure" : "",
                sameSiteValue
            );
            response.setHeader("Set-Cookie", cookieHeader);
        }
    }

    /**
     * Apply comprehensive security measures.
     *
     * @param relativePath path relative to context root (no context prefix)
     * @return true if request should proceed; false if it was blocked
     */
    private boolean applySecurityMeasures(HttpServletRequest request, HttpServletResponse response,
                                          String relativePath)
            throws IOException {

        String clientIP = SecurityRequestUtil.getClientIP(request);
        String method = request.getMethod();

        // 1. Rate limiting check (skip if disabled)
        if (rateLimitEnabled && !checkRateLimit(clientIP, relativePath, method)) {
            logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIP, relativePath);
            response.sendError(429, "Rate limit exceeded");
            return false;
        }

        // 2. Brute-force protection for login attempts (only on POST to login paths)
        //    Skip for whitelisted login endpoints and safe storefront routes.
        if ("POST".equalsIgnoreCase(method)
                && (relativePath.contains("/login") || relativePath.contains("/auth"))
                && WHITELISTED_PATHS.stream().noneMatch(relativePath::startsWith)) {
            String userAgent = request.getHeader("User-Agent");
            if (!checkBruteForceProtection(clientIP, userAgent)) {
                logger.warn("Brute force protection triggered for IP: {}", clientIP);
                response.sendError(429, "Too many login attempts");
                return false;
            }
        }

        // 3. Apply security headers (always, regardless of route)
        applySecurityHeaders(request, response);

        // 4. Attack pattern detection — SKIP for known-safe storefront routes.
        //    This prevents false positives on cart query strings, product searches, etc.
        if (!isSafeStorefrontRoute(relativePath)) {
            if (detectAttackPatterns(request)) {
                logger.warn("Attack pattern detected from IP: {} on path: {}", clientIP, relativePath);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return false;
            }
        }

        // 5. Validate request size
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
        String key = clientIP + ":" + (userAgent != null ? userAgent : "unknown");
        LoginTracker tracker = loginTrackers.computeIfAbsent(key, k -> new LoginTracker());
        return tracker.allowLoginAttempt();
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

        // Content Security Policy
        String cspPolicy =
            "default-src 'self'; " +
            "script-src 'self' 'nonce-" + nonce + "' https://js.stripe.com https://cdn.jsdelivr.net https://www.gstatic.com 'unsafe-inline'; " +
            "style-src 'self' 'nonce-" + nonce + "' https://fonts.googleapis.com 'unsafe-inline'; " +
            "font-src 'self' https://fonts.gstatic.com data:; " +
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
     * Detect common attack patterns.
     *
     * NOTE: This is only called for non-storefront routes. Cart query strings
     * (action=add&amp;productId=5) and similar are never scanned here.
     */
    private boolean detectAttackPatterns(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String path = request.getRequestURI();

        // Check for SQL injection patterns in query string
        if (queryString != null && containsSQLInjection(queryString)) {
            logger.warn("SQL injection pattern detected in query: {}", queryString);
            return true;
        }

        // Check for XSS patterns in query string
        if (queryString != null && containsXSS(queryString)) {
            logger.warn("XSS pattern detected in query: {}", queryString);
            return true;
        }

        // Check for path traversal (only double-dot sequences — NOT encoded slashes)
        if (containsPathTraversal(path)) {
            logger.warn("Path traversal pattern detected in path: {}", path);
            return true;
        }

        return false;
    }

    /**
     * Validate request size
     */
    private boolean validateRequestSize(HttpServletRequest request) {
        int contentLength = request.getContentLength();
        // -1 means unknown length (e.g. chunked transfer) — allow it
        if (contentLength < 0) return true;
        // Limit known-size request bodies to 10MB
        int maxRequestSize = 10 * 1024 * 1024;
        return contentLength <= maxRequestSize;
    }

    /**
     * SQL injection detection — focused on dangerous DDL/DML keywords only.
     * Simple alphanumeric query params with & separators are not flagged.
     */
    private boolean containsSQLInjection(String input) {
        // Check for classic SQL injection markers
        String upper = input.toUpperCase();

        // Detect UNION SELECT, DROP TABLE, etc.
        String[] patterns = {
            "UNION\\s+SELECT",
            "DROP\\s+TABLE",
            "DROP\\s+DATABASE",
            "INSERT\\s+INTO",
            "DELETE\\s+FROM",
            "UPDATE\\s+\\w+\\s+SET",
            "EXEC\\s*\\(",
            "EXECUTE\\s*\\(",
            "CAST\\s*\\(",
            "CONVERT\\s*\\(",
            "DECLARE\\s+@",
            "XP_CMDSHELL",
            "SP_EXECUTESQL",
            "INFORMATION_SCHEMA",
            "SYS\\.TABLES",
            "OR\\s+1\\s*=\\s*1",
            "AND\\s+1\\s*=\\s*1",
            "OR\\s+'[^']*'\\s*=\\s*'[^']*'",
            "--\\s",    // SQL comment at end
            "/\\*.*\\*/" // Block comment
        };

        for (String pattern : patterns) {
            if (upper.matches("(?s).*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }

    /**
     * XSS detection — focused on script injection, not general HTML characters.
     */
    private boolean containsXSS(String input) {
        String lower = input.toLowerCase();
        String[] xssPatterns = {
            "<script[^>]*>",
            "</script>",
            "javascript:",
            "vbscript:",
            "<iframe[^>]*>",
            "<object[^>]*>",
            "<embed[^>]*>",
            "on(load|error|click|mouseover|focus|blur|change|submit|reset|keydown|keyup|keypress)\\s*="
        };

        for (String pattern : xssPatterns) {
            if (lower.matches("(?s).*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Path traversal detection — only flags actual double-dot traversal sequences.
     * Does NOT flag %2f (encoded slash) since that is valid in many query strings.
     */
    private boolean containsPathTraversal(String path) {
        // Normalize percent encoding before checking
        String decoded = path.replace("%2e", ".").replace("%2E", ".");
        return decoded.contains("../") || decoded.contains("..\\") || decoded.endsWith("..");
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
        requestTrackers.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        loginTrackers.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
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
                lockoutUntil = now + (LOGIN_LOCKOUT_MINUTES * 60 * 1000L);
                attemptCount.set(0); // Reset after lockout
                return false;
            }

            return true;
        }

        public boolean isExpired(long now) {
            return now - lastAttempt > 24 * 60 * 60 * 1000L; // Expire after 24 hours
        }
    }

    @Override
    public void destroy() {
        logger.info("SecurityHardeningFilter destroyed");
        requestTrackers.clear();
        loginTrackers.clear();
    }
}
