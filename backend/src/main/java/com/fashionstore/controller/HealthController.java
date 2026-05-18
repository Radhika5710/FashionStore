package com.fashionstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashionstore.cache.RedisConnection;
import com.fashionstore.util.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for monitoring and observability
 * Provides health status for Kubernetes probes, load balancers, and monitoring systems
 */
@WebServlet("/healthz")
public class HealthController extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static RedisConnection redisConnection;

    static {
        // Initialize Redis connection for health checks
        redisConnection = new RedisConnection();
        redisConnection.connect(Duration.ofSeconds(5));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            
            // Check database health
            Map<String, Object> components = new HashMap<>();
            Map<String, Object> dbCheck = new HashMap<>();
            
            boolean dbHealthy = checkDatabase();
            dbCheck.put("status", dbHealthy ? "UP" : "DOWN");
            components.put("database", dbCheck);
            
            // Check Redis health
            Map<String, Object> redisCheck = new HashMap<>();
            boolean redisHealthy = checkRedis();
            redisCheck.put("status", redisHealthy ? "UP" : "DOWN");
            components.put("redis", redisCheck);
            
            health.put("components", components);
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            if (dbHealthy && redisHealthy) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                health.put("status", "DOWN");
            }
            
            response.getWriter().write(objectMapper.writeValueAsString(health));
            
        } catch (Exception e) {
            logger.error("Health check error: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) {
        // Lightweight health check for Kubernetes liveness probes
        try {
            boolean dbHealthy = checkDatabase();
            boolean redisHealthy = checkRedis();
            if (dbHealthy && redisHealthy) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    private boolean checkDatabase() {
        try (Connection conn = DBConnection.getConnection()) {
            return conn != null && conn.isValid(5);
        } catch (Exception e) {
            logger.error("Database health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean checkRedis() {
        try {
            return redisConnection.isConnected();
        } catch (Exception e) {
            logger.error("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}
