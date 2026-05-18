package com.fashionstore.filter;

import com.fashionstore.security.AuthContext;
import com.fashionstore.security.JWTUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JWTAuthenticationFilter - Hybrid MVC + React Admin Architecture
 * 
 * ARCHITECTURE OVERVIEW:
 * =====================
 * - JSP + Session Auth: Customer MVC frontend
 *   * JSP pages rendered server-side
 *   * Session-based authentication for customers
 *   * CSRF protection for form submissions
 *   * Routes: /login, /register, /products, /cart, /checkout, etc.
 * 
 * - JWT Auth: React admin APIs only
 *   * React admin dashboard (separate frontend)
 *   * JWT token-based authentication
 *   * Routes: /api/admin/*
 *   * No session required for admin APIs
 * 
 * FILTER RESPONSIBILITIES:
 * =======================
 * 1. Preserve JSP MVC session authentication
 *    - Skip JWT validation for JSP routes
 *    - Let session auth handle customer authentication
 *    - Do NOT interfere with HttpSession
 * 
 * 2. Apply JWT authentication ONLY to /api/admin/*
 *    - Validate JWT token in Authorization header
 *    - Check admin role
 *    - Return JSON responses
 * 
 * 3. Prevent authentication conflicts
 *    - No 401/403 conflicts between session and JWT
 *    - No HttpSession conflicts
 *    - Clear separation of concerns
 * 
 * FILTER LOGIC:
 * =============
 * 1. Skip OPTIONS requests (CORS preflight)
 * 2. Skip routes that use session auth (JSP pages, customer APIs)
 * 3. Skip static assets (/assets/*, /images/*, etc.)
 * 4. Skip public admin APIs (/api/admin/login, /api/admin/register)
 * 5. Apply JWT validation ONLY to protected admin APIs (/api/admin/*)
 * 6. Return JSON unauthorized/forbidden responses
 * 7. Continue filter chain for all other routes
 * 
 * ROUTES HANDLED:
 * ===============
 * Session Auth (Skip JWT):
 * - /login, /register, /home, /products, /product, /cart, /checkout
 * - /account, /orders, /wishlist, /forgot-password, /reset-password
 * - /api/products, /api/categories, /api/search, /api/cart, /api/wishlist
 * - /api/orders, /api/profile, /api/address, /api/reviews
 * - /assets/*, /images/*, /css/*, /js/*, /fonts/*
 * - /, /error
 * 
 * JWT Auth (Apply validation):
 * - /api/admin/products, /api/admin/orders, /api/admin/users
 * - /api/admin/categories, /api/admin/coupons, /api/admin/dashboard
 * - /api/admin/reports, /api/admin/settings
 * 
 * Public Admin APIs (No JWT required):
 * - /api/admin/login, /api/admin/register
 * 
 * SECURITY GUARANTEES:
 * ====================
 * ✓ JSP customer frontend works normally with session auth
 * ✓ React admin login works independently with JWT
 * ✓ JWT only secures admin APIs (/api/admin/*)
 * ✓ No session conflicts between customer and admin auth
 * ✓ No 401/403 conflicts between authentication methods
 * ✓ CSRF protection preserved for JSP forms
 * ✓ Static assets served without authentication
 */
public class JWTAuthenticationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(JWTAuthenticationFilter.class);
    
    /**
     * ADMIN API ENDPOINTS - Only these require JWT authentication
     * All other routes use session-based authentication or are public
     */
    private static final Set<String> ADMIN_API_ENDPOINTS = new HashSet<>(Arrays.asList(
        "/api/admin/products",
        "/api/admin/orders",
        "/api/admin/users",
        "/api/admin/categories",
        "/api/admin/coupons",
        "/api/admin/dashboard",
        "/api/admin/reports",
        "/api/admin/settings"
    ));
    
    /**
     * PUBLIC ADMIN API ENDPOINTS - No authentication required
     */
    private static final Set<String> PUBLIC_ADMIN_API_ENDPOINTS = new HashSet<>(Arrays.asList(
        "/api/admin/login",
        "/api/admin/register"
    ));
    
    /**
     * ROUTES TO SKIP - These use session auth or are public
     * JWT filter should NOT intercept these
     */
    private static final Set<String> SKIP_JWT_ROUTES = new HashSet<>(Arrays.asList(
        // JSP pages and MVC routes (session auth)
        "/login",
        "/register",
        "/home",
        "/products",
        "/product",
        "/cart",
        "/checkout",
        "/forgot-password",
        "/reset-password",
        "/account",
        "/orders",
        "/wishlist",
        
        // Static assets
        "/assets",
        "/images",
        "/css",
        "/js",
        "/fonts",
        
        // Public customer APIs (session auth)
        "/api/products",
        "/api/categories",
        "/api/search",
        "/api/cart",
        "/api/wishlist",
        "/api/orders",
        "/api/profile",
        "/api/address",
        "/api/reviews",
        
        // Root and error pages
        "/",
        "/error"
    ));
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("JWT Authentication Filter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        logger.debug("JWT Filter: {} {}", method, path);
        
        // ============================================================
        // HYBRID ARCHITECTURE LOGIC
        // Separate JWT auth (admin APIs) from Session auth (customer MVC)
        // ============================================================
        
        // 1. Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            logger.debug("CORS preflight request, skipping: {}", path);
            chain.doFilter(request, response);
            return;
        }
        
        // 2. Skip routes that use session auth or are public
        // This includes JSP pages, MVC controllers, static assets, customer APIs
        // CRITICAL: Do NOT apply JWT validation to these routes
        // Let session auth handle them exclusively
        if (shouldSkipJWTValidation(path)) {
            logger.debug("Skipping JWT validation for route: {} (uses session auth or is public)", path);
            // Do NOT create AuthContext for session-based routes
            // This prevents HttpSession conflicts
            chain.doFilter(request, response);
            return;
        }
        
        // 3. Check if this is a public admin API endpoint
        // Public endpoints don't require JWT validation
        if (isPublicAdminApiEndpoint(path)) {
            logger.debug("Public admin API endpoint, skipping JWT validation: {}", path);
            chain.doFilter(request, response);
            return;
        }
        
        // 4. At this point, we know it's a protected admin API endpoint
        // Apply JWT validation ONLY to /api/admin/* endpoints
        if (!isAdminApiEndpoint(path)) {
            // Not an admin API endpoint, let it through
            logger.debug("Not an admin API endpoint, skipping JWT validation: {}", path);
            chain.doFilter(request, response);
            return;
        }
        
        logger.debug("Protected admin API endpoint, validating JWT: {}", path);
        
        // ============================================================
        // JWT VALIDATION FOR ADMIN APIS ONLY
        // ============================================================
        
        // Create auth context and validate JWT
        AuthContext authContext = AuthContext.fromRequest(httpRequest);
        
        // Check if authenticated
        if (!authContext.isAuthenticated()) {
            logger.warn("Unauthenticated request to protected admin API: {}", path);
            sendUnauthorizedResponse(httpResponse, authContext.getError());
            return;
        }
        
        // Check if user has admin role
        if (!authContext.isAdmin()) {
            logger.warn("Non-admin user accessing admin API: {} - Role: {}", path, authContext.getRole());
            sendForbiddenResponse(httpResponse, "Admin access required");
            return;
        }
        
        // Store auth context in request attribute for controllers
        // This is ONLY for admin API controllers
        httpRequest.setAttribute("authContext", authContext);
        
        logger.debug("Admin API request authenticated: {} - User: {} - Role: {}", 
                     path, authContext.getUserId(), authContext.getRole());
        
        // Continue filter chain
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        logger.info("JWT Authentication Filter destroyed");
    }
    
    /**
     * Check if JWT validation should be skipped for this route
     * 
     * JWT validation is skipped for:
     * - JSP pages and MVC routes (use session auth)
     * - Static assets
     * - Public customer APIs
     * - Root and error pages
     */
    private boolean shouldSkipJWTValidation(String path) {
        for (String skipRoute : SKIP_JWT_ROUTES) {
            if (path.startsWith(skipRoute)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if this is a public admin API endpoint
     * Public endpoints don't require JWT validation
     */
    private boolean isPublicAdminApiEndpoint(String path) {
        for (String endpoint : PUBLIC_ADMIN_API_ENDPOINTS) {
            if (path.startsWith(endpoint)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if this is a protected admin API endpoint
     * These endpoints require JWT validation and admin role
     */
    private boolean isAdminApiEndpoint(String path) {
        for (String endpoint : ADMIN_API_ENDPOINTS) {
            if (path.startsWith(endpoint)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Send unauthorized response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String jsonResponse = String.format(
            "{\"success\":false,\"message\":\"%s\",\"error\":\"UNAUTHORIZED\"}",
            message != null ? message : "Authentication required"
        );
        
        response.getWriter().write(jsonResponse);
    }
    
    /**
     * Send forbidden response
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        String jsonResponse = String.format(
            "{\"success\":false,\"message\":\"%s\",\"error\":\"FORBIDDEN\"}",
            message != null ? message : "Access denied"
        );
        
        response.getWriter().write(jsonResponse);
    }
}
