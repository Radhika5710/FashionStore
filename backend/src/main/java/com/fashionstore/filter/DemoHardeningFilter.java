package com.fashionstore.filter;

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
 * Demo Hardening Filter - Emergency Demo Stabilization Mode
 *
 * Only active when the environment variable DEMO_HARDENING_MODE=true is set.
 * When inactive, all requests pass through immediately.
 *
 * DISABLED ROUTES (no backing implementation):
 * - /metrics* - Metrics collection
 * - /search-analytics* - Search analytics
 * - /recently-viewed* - Recently viewed products
 * - /recommendations* - Product recommendations
 * - /notification* - Notifications
 * - /newsletter* - Newsletter subscription
 * - /password-reset* - Password reset flow
 * - /csp-violation* - CSP violation reporting endpoint
 * - /invoice* - Invoice generation
 * - /location* - Location services
 * - /saved-items* - Saved items
 * - /product-filter* - Advanced filtering
 *
 * ALWAYS ALLOWED (storefront critical — must NEVER be disabled):
 * - /home, / - Home page
 * - /products - Product catalogue
 * - /product/* - Product details
 * - /login, /register - Authentication
 * - /logout - Logout
 * - /cart - Shopping cart
 * - /checkout - Checkout
 * - /wishlist - Wishlist page
 * - /orders - Order history
 * - /review - Product reviews
 * - /address, /account/addresses - Address management
 * - /order-tracking - Order tracking
 * - /api/admin/* - Admin APIs
 * - /healthz, /health - Health checks
 */
public class DemoHardeningFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DemoHardeningFilter.class);
    
    private static volatile boolean demoHardeningMode = false;
    
    // Non-essential routes to disable in demo mode.
    // NOTE: Only routes that have NO backing servlet/controller should be here.
    // Critical storefront routes (/wishlist, /orders, /cart, /review, /address,
    // /order-tracking) are INTENTIONALLY omitted — they must work in all modes.
    private static final Set<String> DISABLED_ROUTES = new HashSet<>(Arrays.asList(
        "/metrics",
        "/search-analytics",
        "/recently-viewed",
        "/recommendations",
        "/notification",
        "/newsletter",
        "/password-reset",
        "/csp-violation",
        "/invoice",
        "/location",
        "/saved-items",
        "/product-filter"
    ));

    @Override
    public void init(FilterConfig filterConfig) {
        // Check if demo hardening mode is enabled
        String demoHardening = System.getenv("DEMO_HARDENING_MODE");
        if (demoHardening != null && demoHardening.equalsIgnoreCase("true")) {
            demoHardeningMode = true;
            logger.warn("========================================");
            logger.warn("EMERGENCY DEMO STABILIZATION MODE ACTIVE");
            logger.warn("Non-essential routes disabled");
            logger.warn("========================================");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!demoHardeningMode) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip internal dispatcher forwards/includes/errors — never block JSP forwards
        DispatcherType dt = httpRequest.getDispatcherType();
        if (dt == DispatcherType.FORWARD || dt == DispatcherType.INCLUDE || dt == DispatcherType.ERROR) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();

        // Get path without context
        String relativePath = path.length() > contextPath.length()
                ? path.substring(contextPath.length())
                : "/";

        // Check if route is disabled
        for (String disabledRoute : DISABLED_ROUTES) {
            if (relativePath.startsWith(disabledRoute)) {
                logger.warn("Demo Hardening: Disabled route accessed - {}", relativePath);
                httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Feature disabled in demo mode");
                return;
            }
        }

        // Continue with request
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("DemoHardeningFilter destroyed");
    }
    
    /**
     * Check if demo hardening mode is active
     */
    public static boolean isDemoHardeningMode() {
        return demoHardeningMode;
    }
}
