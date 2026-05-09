package com.fashionstore.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    
    private static final Map<String, RateLimitEntry> rateLimits = new ConcurrentHashMap<>();
    private static final long CLEANUP_INTERVAL = 60000; // 1 minute
    private static volatile long lastCleanup = System.currentTimeMillis();
    
    // Rate limits per endpoint
    private static final int LOGIN_ATTEMPTS_PER_MINUTE = 5;
    private static final int REGISTRATION_ATTEMPTS_PER_MINUTE = 3;
    private static final int GENERAL_REQUESTS_PER_MINUTE = 100;
    private static final int PASSWORD_RESET_ATTEMPTS_PER_MINUTE = 3;
    
    private static class RateLimitEntry {
        final AtomicInteger count;
        final long windowStart;
        
        RateLimitEntry() {
            this.count = new AtomicInteger(1);
            this.windowStart = System.currentTimeMillis();
        }
        
        boolean isExpired(long windowMs) {
            return System.currentTimeMillis() - windowStart > windowMs;
        }
    }
    
    public static boolean checkRateLimit(HttpServletRequest request, String endpoint) {
        String key = getClientKey(request, endpoint);
        int maxRequests = getMaxRequests(endpoint);
        long windowMs = 60000; // 1 minute window
        
        cleanupExpiredEntries();
        
        RateLimitEntry entry = rateLimits.get(key);
        if (entry == null || entry.isExpired(windowMs)) {
            rateLimits.put(key, new RateLimitEntry());
            return true;
        }
        
        int currentCount = entry.count.incrementAndGet();
        if (currentCount > maxRequests) {
            logger.warn("Rate limit exceeded for key: {}, endpoint: {}, count: {}", key, endpoint, currentCount);
            return false;
        }
        
        return true;
    }
    
    private static String getClientKey(HttpServletRequest request, String endpoint) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        return endpoint + ":" + ip + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");
    }
    
    private static int getMaxRequests(String endpoint) {
        if (endpoint.contains("/login")) return LOGIN_ATTEMPTS_PER_MINUTE;
        if (endpoint.contains("/register")) return REGISTRATION_ATTEMPTS_PER_MINUTE;
        if (endpoint.contains("/forgot-password")) return PASSWORD_RESET_ATTEMPTS_PER_MINUTE;
        return GENERAL_REQUESTS_PER_MINUTE;
    }
    
    private static void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < CLEANUP_INTERVAL) {
            return;
        }
        
        lastCleanup = now;
        rateLimits.entrySet().removeIf(entry -> entry.getValue().isExpired(60000));
        logger.debug("Rate limiter cleanup completed, active entries: {}", rateLimits.size());
    }
    
    public static void resetRateLimit(HttpServletRequest request, String endpoint) {
        String key = getClientKey(request, endpoint);
        rateLimits.remove(key);
        logger.debug("Rate limit reset for key: {}", key);
    }
}
