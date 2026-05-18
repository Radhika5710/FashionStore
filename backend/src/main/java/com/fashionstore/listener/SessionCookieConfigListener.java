package com.fashionstore.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.annotation.WebListener;

/**
 * Makes the session cookie {@code Secure} flag environment-aware so plain HTTP on localhost works,
 * while production behind HTTPS can enforce secure cookies via env or profile.
 */
@WebListener
public class SessionCookieConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        boolean secure = resolveSecureCookie();
        SessionCookieConfig cfg = sce.getServletContext().getSessionCookieConfig();
        cfg.setHttpOnly(true);
        cfg.setSecure(secure);
    }

    /**
     * Order: explicit {@code FASHIONSTORE_SESSION_COOKIE_SECURE}, then {@code FASHIONSTORE_PROFILE=prod},
     * otherwise false (typical local HTTP).
     */
    private static boolean resolveSecureCookie() {
        String explicit = System.getenv("FASHIONSTORE_SESSION_COOKIE_SECURE");
        if (explicit != null && !explicit.isBlank()) {
            return Boolean.parseBoolean(explicit.trim());
        }
        String profile = System.getenv("FASHIONSTORE_PROFILE");
        return profile != null && "prod".equalsIgnoreCase(profile.trim());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup on shutdown
    }
}
