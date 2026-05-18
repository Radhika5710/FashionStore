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
 * DISABLED ROUTES (Non-Essential for Demo):
 * - /metrics* - Metrics collection (TODO not implemented)
 * - /search-analytics* - Search analytics (TODO not implemented)
 * - /wishlist* - Wishlist features
 * - /recently-viewed* - Recently viewed products
 * - /recommendations* - Product recommendations
 * - /notification* - Notifications
 * - /newsletter* - Newsletter subscription
 * - /password-reset* - Password reset
 * - /csp-violation* - CSP violation reporting
 * - /invoice* - Invoice generation
 * - /order-tracking* - Order tracking
 * - /location* - Location services
 * - /address* - Address management
 * - /saved-items* - Saved items
 * - /review* - Product reviews
 * - /product-filter* - Advanced filtering
 * 
 * ALLOWED ROUTES (Demo Critical):
 * - /home, / - Home page
 * - /products - Product list
 * - /product/* - Product details
 * - /login, /register - Authentication
 * - /cart - Cart
 * - /checkout - Checkout
 * - /api/admin/login - Admin login
 * - /api/admin/dashboard - Admin dashboard
 * - /api/admin/products - Admin products
 * - /api/admin/orders - Admin orders
 * - /api/admin/logout - Admin logout
 * - /healthz, /health - Health checks
 */
public class DemoHardeningFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DemoHardeningFilter.class);
    
    private static volatile boolean demoHardeningMode = false;
    
    // Non-essential routes to disable in demo mode
    private static final Set<String> DISABLED_ROUTES = new HashSet<>(Arrays.asList(
        "/metrics",
        "/search-analytics",
        "/wishlist",
        "/recently-viewed",
        "/recommendations",
        "/notification",
        "/newsletter",
        "/password-reset",
        "/csp-violation",
        "/invoice",
        "/order-tracking",
        "/location",
        "/address",
        "/saved-items",
        "/review",
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
        
        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        
        // Get path without context
        String relativePath = path.substring(contextPath.length());
        
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
