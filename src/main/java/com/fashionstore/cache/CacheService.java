package com.fashionstore.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    private static volatile CacheService instance;
    private final Map<String, CacheEntry> cache;
    private final boolean redisEnabled;
    
    private CacheService() {
        this.cache = new ConcurrentHashMap<>();
        this.redisEnabled = false;
        logger.info("Cache service initialized with in-memory fallback");
    }
    
    public static CacheService getInstance() {
        if (instance == null) {
            synchronized (CacheService.class) {
                if (instance == null) {
                    instance = new CacheService();
                }
            }
        }
        return instance;
    }
    
    public void put(String key, Object value, long ttl, TimeUnit timeUnit) {
        if (key == null || value == null) {
            return;
        }
        
        long expiryTime = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        cache.put(key, new CacheEntry(value, expiryTime));
        logger.debug("Cached key: {} with TTL: {} {}", key, ttl, timeUnit);
    }
    
    public void put(String key, Object value) {
        put(key, value, 1, TimeUnit.HOURS);
    }
    
    public Object get(String key) {
        if (key == null) {
            return null;
        }
        
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (System.currentTimeMillis() > entry.expiryTime) {
            cache.remove(key);
            logger.debug("Cache expired for key: {}", key);
            return null;
        }
        
        return entry.value;
    }
    
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        
        try {
            return type.cast(value);
        } catch (ClassCastException e) {
            logger.warn("Type mismatch for cached key: {}", key);
            return null;
        }
    }
    
    public void remove(String key) {
        if (key != null) {
            cache.remove(key);
            logger.debug("Removed from cache: {}", key);
        }
    }
    
    public void clear() {
        cache.clear();
        logger.info("Cache cleared");
    }
    
    public void invalidatePattern(String pattern) {
        cache.keySet().removeIf(key -> key.matches(pattern));
        logger.debug("Invalidated cache pattern: {}", pattern);
    }
    
    public int size() {
        cleanupExpired();
        return cache.size();
    }
    
    public void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> currentTime > entry.getValue().expiryTime);
    }
    
    public boolean isRedisEnabled() {
        return redisEnabled;
    }
    
    private static class CacheEntry {
        final Object value;
        final long expiryTime;
        
        CacheEntry(Object value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
    }
}
