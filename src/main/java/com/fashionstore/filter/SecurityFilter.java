package com.fashionstore.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@WebFilter("/*")
public class SecurityFilter implements Filter {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Map<String, Integer> loginAttempts = new HashMap<>();
    private static final Map<String, Long> loginAttemptTimes = new HashMap<>();
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    @Override
    public void init(FilterConfig filterConfig) {
        // Filter initialization
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Set security headers
        setSecurityHeaders(httpResponse);

        // CSRF token generation and validation
        handleCSRF(httpRequest, httpResponse);

        // Rate limiting for login
        if (!checkRateLimit(httpRequest, httpResponse)) {
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Cleanup
    }

    private void setSecurityHeaders(HttpServletResponse response) {
        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Enable XSS protection
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Strict Transport Security (HTTPS only)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        
        // Content Security Policy
        // Kept in sync with SecurityHeadersFilter so filter order does not matter.
        response.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; " +
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
            "style-src-elem 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data: https://fonts.gstatic.com https://fonts.googleapis.com; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'; " +
            "form-action 'self'; " +
            "base-uri 'self'"
        );
        
        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=(), payment=()"
        );
    }

    private void handleCSRF(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        HttpSession session = request.getSession(false);
        
        // Generate CSRF token for GET requests
        if ("GET".equals(request.getMethod()) && session != null) {
            String csrfToken = (String) session.getAttribute("csrf_token");
            if (csrfToken == null) {
                csrfToken = generateCSRFToken();
                session.setAttribute("csrf_token", csrfToken);
            }
            request.setAttribute("csrfToken", csrfToken);
        }
        
        // Validate CSRF token for POST requests (excluding login/register)
        if ("POST".equals(request.getMethod()) && session != null) {
            String path = request.getRequestURI();
            if (!path.contains("/login") && !path.contains("/register")) {
                String sessionToken = (String) session.getAttribute("csrf_token");
                String requestToken = request.getParameter("csrf_token");
                if (requestToken == null) {
                    requestToken = request.getHeader("X-CSRF-Token");
                }
                
                if (sessionToken == null || !sessionToken.equals(requestToken)) {
                    throw new ServletException("CSRF token validation failed");
                }
            }
        }
    }

    private String generateCSRFToken() {
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private boolean checkRateLimit(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        String path = request.getRequestURI();
        
        // Only apply rate limiting to login endpoint
        if (!path.contains("/login")) {
            return true;
        }
        
        if ("POST".equals(request.getMethod())) {
            String clientIp = getClientIp(request);
            Long lastAttemptTime = loginAttemptTimes.get(clientIp);
            Integer attempts = loginAttempts.getOrDefault(clientIp, 0);
            
            long currentTime = System.currentTimeMillis();
            
            // Reset if lockout period has passed
            if (lastAttemptTime != null && (currentTime - lastAttemptTime) > LOCKOUT_DURATION_MS) {
                loginAttempts.put(clientIp, 0);
                loginAttemptTimes.remove(clientIp);
                return true;
            }
            
            // Check if locked out
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                if (lastAttemptTime != null && (currentTime - lastAttemptTime) < LOCKOUT_DURATION_MS) {
                    response.sendError(429, "Too many login attempts. Please try again later.");
                    return false;
                }
            }
            
            // Increment attempt counter
            loginAttempts.put(clientIp, attempts + 1);
            loginAttemptTimes.put(clientIp, currentTime);
        }
        
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
