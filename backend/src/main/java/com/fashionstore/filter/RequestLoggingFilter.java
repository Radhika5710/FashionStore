package com.fashionstore.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * RequestLoggingFilter - Lightweight Request Tracing
 * 
 * FILTER CHAIN ARCHITECTURE:
 * ==========================
 * Filter execution order (from web.xml):
 * 1. RequestLoggingFilter - Adds request ID to MDC (THIS FILTER)
 * 2. CORSFilter - Handles CORS preflight (OPTIONS)
 * 3. SecurityHardeningFilter - Rate limiting, attack prevention
 * 4. JWTAuthenticationFilter - JWT validation for /api/admin/*
 * 5. Servlet/Controller - Handles request
 * 
 * RESPONSIBILITIES:
 * =================
 * ✓ Generate unique request ID (8-char UUID)
 * ✓ Add request ID to MDC (Mapped Diagnostic Context)
 * ✓ Log request method, URI, remote address
 * ✓ Track request duration
 * ✓ Log slow requests (> 1 second)
 * ✓ Add X-Request-ID header to response
 * ✓ Clear MDC after request
 * 
 * DOES NOT HANDLE:
 * ================
 * ✗ Authentication (JWTAuthenticationFilter)
 * ✗ Authorization (JWTAuthenticationFilter)
 * ✗ Rate limiting (SecurityHardeningFilter)
 * ✗ CORS (CORSFilter)
 * ✗ Security headers (SecurityHardeningFilter)
 * 
 * APPLIES TO:
 * ===========
 * - ALL requests (logging is first in chain)
 * - JSP pages and MVC controllers
 * - Customer APIs (/api/*)
 * - Admin APIs (/api/admin/*)
 * - Static assets
 * - CORS preflight (OPTIONS)
 * 
 * PERFORMANCE NOTES:
 * ==================
 * - Minimal overhead (UUID generation + MDC put)
 * - No blocking operations
 * - Lightweight logging (info level)
 * - Slow request detection (> 1 second)
 * - MDC cleared in finally block (no memory leaks)
 */
public class RequestLoggingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("RequestLoggingFilter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Generate unique request ID
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        // Add to MDC for structured logging
        MDC.put("requestId", requestId);
        MDC.put("method", httpRequest.getMethod());
        MDC.put("uri", httpRequest.getRequestURI());
        MDC.put("remoteAddr", httpRequest.getRemoteAddr());
        
        // Add request ID to response header
        httpResponse.setHeader("X-Request-ID", requestId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();
            
            // Log request details
            logger.info("Request: {} {} | Status: {} | Duration: {}ms | Remote: {}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    status,
                    duration,
                    httpRequest.getRemoteAddr());
            
            // Log slow requests (> 1 second)
            if (duration > 1000) {
                logger.warn("SLOW REQUEST: {} {} | Duration: {}ms",
                        httpRequest.getMethod(),
                        httpRequest.getRequestURI(),
                        duration);
            }
            
            // Clear MDC
            MDC.clear();
        }
    }
    
    @Override
    public void destroy() {
        logger.info("RequestLoggingFilter destroyed");
    }
}
