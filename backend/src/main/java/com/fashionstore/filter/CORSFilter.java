package com.fashionstore.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * CORSFilter - Cross-Origin Resource Sharing for React Admin
 * 
 * FILTER CHAIN ARCHITECTURE:
 * ==========================
 * Filter execution order (from web.xml):
 * 1. RequestLoggingFilter - Adds request ID to MDC
 * 2. CORSFilter - Handles CORS preflight (OPTIONS) (THIS FILTER)
 * 3. SecurityHardeningFilter - Rate limiting, attack prevention
 * 4. JWTAuthenticationFilter - JWT validation for /api/admin/*
 * 5. Servlet/Controller - Handles request
 * 
 * RESPONSIBILITIES:
 * =================
 * ✓ Handle CORS preflight requests (OPTIONS)
 * ✓ Set Access-Control-Allow-Origin header
 * ✓ Set Access-Control-Allow-Credentials header
 * ✓ Set Access-Control-Allow-Methods header
 * ✓ Set Access-Control-Allow-Headers header
 * ✓ Validate origin against whitelist
 * ✓ Support same-origin requests
 * 
 * DOES NOT HANDLE:
 * ================
 * ✗ Authentication (JWTAuthenticationFilter)
 * ✗ Authorization (JWTAuthenticationFilter)
 * ✗ Rate limiting (SecurityHardeningFilter)
 * ✗ Request logging (RequestLoggingFilter)
 * ✗ Security headers (SecurityHardeningFilter)
 * 
 * APPLIES TO:
 * ===========
 * - React admin frontend requests (/api/admin/*)
 * - CORS preflight requests (OPTIONS)
 * - Cross-origin requests from allowed origins
 * 
 * DOES NOT APPLY TO:
 * ==================
 * - JSP pages (same-origin)
 * - MVC controllers (same-origin)
 * - Customer APIs (same-origin)
 * - Static assets (same-origin)
 * 
 * CONFIGURATION:
 * ===============
 * Environment variable: CORS_ALLOWED_ORIGINS
 * Format: comma-separated list of origins
 * Example: http://localhost:5173,http://localhost:3000
 * 
 * Default (development):
 * - http://localhost:5173 (Vite dev server)
 * - http://127.0.0.1:5173
 * - http://localhost:3000 (React dev server)
 * - http://127.0.0.1:3000
 * - http://localhost:8080
 * 
 * PREFLIGHT HANDLING:
 * ===================
 * OPTIONS requests:
 * 1. Check if origin is allowed
 * 2. Set CORS headers
 * 3. Return 200 OK (no body)
 * 4. Do NOT continue filter chain
 * 
 * Regular requests:
 * 1. Check if origin is allowed
 * 2. Set CORS headers
 * 3. Continue filter chain
 * 
 * PERFORMANCE NOTES:
 * ==================
 * - Minimal overhead (origin check + header set)
 * - Early return for OPTIONS (prevents unnecessary processing)
 * - No blocking operations
 * - Preflight caching (Access-Control-Max-Age: 3600)
 */
public class CORSFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CORSFilter.class);
    private Set<String> allowedOrigins;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Load allowed origins from environment variable
        String allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        
        if (allowedOriginsEnv != null && !allowedOriginsEnv.isBlank()) {
            allowedOrigins = new HashSet<>(Arrays.asList(allowedOriginsEnv.split(",")));
            logger.info("CORSFilter initialized with allowed origins from env: {}", allowedOrigins);
        } else {
            // Fallback to localhost for local development only
            allowedOrigins = new HashSet<>(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:8080"
            ));
            logger.info("CORSFilter initialized with default localhost origins for development");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");

        // Allow requests from configured origins or same-origin
        if (origin != null && (allowedOrigins.contains(origin) || isSameOrigin(httpRequest, origin))) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, X-CSRF-Token");
            httpResponse.setHeader("Access-Control-Max-Age", "3600");
        }

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isSameOrigin(HttpServletRequest request, String origin) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String requestOrigin = scheme + "://" + serverName + (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "");
        return origin.equals(requestOrigin);
    }

    @Override
    public void destroy() {
        logger.info("CORSFilter destroyed");
    }
}
