package com.fashionstore.filter;

import com.fashionstore.security.AuthContext;
import com.fashionstore.security.JWTUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JWTAuthenticationFilter - Protects ONLY /api/admin/* endpoints with JWT.
 *
 * ARCHITECTURE:
 * =============
 * - ALL storefront routes (JSP pages, customer APIs) use session-based auth.
 *   This filter is completely transparent to them.
 * - ONLY /api/admin/* routes are subject to JWT validation.
 * - Public admin endpoints (/api/admin/login, /api/admin/register) are exempt.
 *
 * IMPORTANT: This filter is mapped to /api/admin/* in web.xml, so it only
 * runs for requests that match that pattern. The shouldSkipJWTValidation check
 * is a belt-and-suspenders safety guard.
 *
 * SECURITY GUARANTEES:
 * ✓ JSP customer frontend works normally with session auth
 * ✓ React admin login works independently with JWT
 * ✓ JWT only secures admin APIs (/api/admin/*)
 * ✓ No session conflicts between customer and admin auth
 * ✓ No 401/403 conflicts on storefront routes
 * ✓ CSRF protection preserved for JSP forms
 * ✓ Static assets served without authentication
 * ✓ JSP RequestDispatcher.forward() calls are never intercepted
 */
public class JWTAuthenticationFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    /**
     * The single prefix that requires JWT — everything else is SKIPPED.
     */
    private static final String ADMIN_API_PREFIX = "/api/admin/";

    /**
     * Public admin endpoints that do NOT require a JWT (login/register).
     */
    private static final Set<String> PUBLIC_ADMIN_PATHS = new HashSet<>(Arrays.asList(
        "/api/admin/login",
        "/api/admin/register"
    ));

    /**
     * Storefront and customer routes — JWT filter must NEVER touch these.
     * These are checked as a safety guard even though the filter is now mapped
     * only to /api/admin/* in web.xml.
     */
    private static final Set<String> STOREFRONT_PREFIXES = new HashSet<>(Arrays.asList(
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
        "/assets",
        "/images",
        "/css",
        "/js",
        "/fonts",
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
        "/healthz",
        "/health",
        "/api/metrics",
        "/csp-violation-report",
        "/"
    ));

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("JWTAuthenticationFilter initialized — protecting /api/admin/* only");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip internal dispatcher forwards/includes/errors — never re-inspect.
        DispatcherType dt = httpRequest.getDispatcherType();
        if (dt == DispatcherType.FORWARD || dt == DispatcherType.INCLUDE || dt == DispatcherType.ERROR) {
            chain.doFilter(request, response);
            return;
        }

        // Skip CORS preflight
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String contextPath = httpRequest.getContextPath();
        String fullPath = httpRequest.getRequestURI();
        // Relative path (strip context prefix)
        String path = fullPath.length() > contextPath.length()
                ? fullPath.substring(contextPath.length())
                : "/";

        // 1. Known storefront route → skip JWT entirely
        if (isStorefrontRoute(path)) {
            logger.debug("JWT Filter: storefront route, skipping — {}", path);
            chain.doFilter(request, response);
            return;
        }

        // 2. Not an admin API route → skip JWT (belt-and-suspenders)
        if (!path.startsWith(ADMIN_API_PREFIX) && !path.equals("/api/admin")) {
            logger.debug("JWT Filter: non-admin path, skipping — {}", path);
            chain.doFilter(request, response);
            return;
        }

        // 3. Public admin endpoints (login / register) → no token required
        if (isPublicAdminPath(path)) {
            logger.debug("JWT Filter: public admin endpoint, skipping — {}", path);
            chain.doFilter(request, response);
            return;
        }

        // 4. Protected admin API → validate JWT
        logger.debug("JWT Filter: validating JWT for protected admin endpoint — {}", path);

        AuthContext authContext = AuthContext.fromRequest(httpRequest);

        if (!authContext.isAuthenticated()) {
            logger.warn("JWT Filter: unauthenticated request to protected admin API — {}", path);
            sendJsonError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED,
                    authContext.getError() != null ? authContext.getError() : "Authentication required",
                    "UNAUTHORIZED");
            return;
        }

        if (!authContext.isAdmin()) {
            logger.warn("JWT Filter: non-admin access attempt on {} (role={})", path, authContext.getRole());
            sendJsonError(httpResponse, HttpServletResponse.SC_FORBIDDEN,
                    "Admin access required", "FORBIDDEN");
            return;
        }

        // Expose auth context to downstream controllers
        httpRequest.setAttribute("authContext", authContext);
        logger.debug("JWT Filter: admin authenticated — user={}, role={}, path={}",
                authContext.getUserId(), authContext.getRole(), path);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("JWTAuthenticationFilter destroyed");
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private boolean isStorefrontRoute(String path) {
        for (String prefix : STOREFRONT_PREFIXES) {
            if (path.equals(prefix)
                    || path.startsWith(prefix + "/")
                    || path.startsWith(prefix + "?")) {
                return true;
            }
        }
        // Bare root
        return path.isEmpty() || path.equals("/");
    }

    private boolean isPublicAdminPath(String path) {
        for (String pub : PUBLIC_ADMIN_PATHS) {
            if (path.equals(pub) || path.startsWith(pub + "/")) {
                return true;
            }
        }
        return false;
    }

    private void sendJsonError(HttpServletResponse response, int status,
                               String message, String errorCode) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        response.getWriter().write(String.format(
            "{\"success\":false,\"message\":\"%s\",\"error\":\"%s\"}",
            message.replace("\"", "\\\""), errorCode));
    }
}
