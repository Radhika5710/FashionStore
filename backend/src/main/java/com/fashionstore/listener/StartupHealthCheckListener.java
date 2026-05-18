package com.fashionstore.listener;

import com.fashionstore.cache.RedisConnection;
import com.fashionstore.util.DBConnection;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Startup health check listener - verifies critical dependencies during application startup
 * Provides early detection of connection issues before the application accepts traffic
 */
@WebListener
public class StartupHealthCheckListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(StartupHealthCheckListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("========================================");
        logger.info("Starting FashionStore Application");
        logger.info("========================================");
        
        // Check demo mode
        String demoMode = System.getenv("DEMO_MODE");
        if (demoMode != null && demoMode.equalsIgnoreCase("true")) {
            logger.info("DEMO MODE ENABLED - Security features relaxed for demo");
        }
        
        // Check demo hardening mode
        String demoHardening = System.getenv("DEMO_HARDENING_MODE");
        if (demoHardening != null && demoHardening.equalsIgnoreCase("true")) {
            logger.warn("EMERGENCY DEMO STABILIZATION MODE ACTIVE");
            logger.warn("Non-essential features disabled for maximum reliability");
        }
        
        // Check CSRF protection
        String csrfEnabled = System.getenv("CSRF_ENABLED");
        if (csrfEnabled != null && csrfEnabled.equalsIgnoreCase("false")) {
            logger.info("CSRF PROTECTION DISABLED via environment variable");
        }
        
        // Check rate limiting
        String rateLimitEnabled = System.getenv("RATE_LIMIT_ENABLED");
        if (rateLimitEnabled != null && rateLimitEnabled.equalsIgnoreCase("false")) {
            logger.info("RATE LIMITING DISABLED via environment variable");
        }
        
        // Verify database connectivity
        checkDatabaseConnectivity();
        
        // Verify Redis connectivity
        checkRedisConnectivity();
        
        logger.info("========================================");
        logger.info("FashionStore Application Started Successfully");
        logger.info("========================================");
    }

    private void checkDatabaseConnectivity() {
        logger.info("Checking database connectivity...");
        boolean dbHealthy = DBConnection.isHealthy();
        
        if (dbHealthy) {
            logger.info("✓ Database connection: HEALTHY");
        } else {
            logger.warn("✗ Database connection: UNHEALTHY - Application may not function correctly");
        }
    }

    private void checkRedisConnectivity() {
        logger.info("Checking Redis connectivity...");
        try {
            RedisConnection redisConnection = new RedisConnection();
            boolean connected = redisConnection.connect(Duration.ofSeconds(5));
            
            if (connected) {
                logger.info("✓ Redis connection: HEALTHY");
                redisConnection.close();
            } else {
                logger.warn("✗ Redis connection: UNHEALTHY - Caching will be disabled");
            }
        } catch (Exception e) {
            logger.warn("✗ Redis connection: FAILED - {}", e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("FashionStore Application shutting down...");
    }
}
