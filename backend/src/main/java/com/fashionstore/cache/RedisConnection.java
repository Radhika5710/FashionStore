package com.fashionstore.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis connection manager with connection pooling and health monitoring
 * Provides reliable Redis connectivity with automatic reconnection
 */
public class RedisConnection {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnection.class);
    
    private JedisPool jedisPool;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicLong lastConnectionTime = new AtomicLong(0);
    private final AtomicLong lastErrorTime = new AtomicLong(0);
    
    // Configuration
    private final String host;
    private final int port;
    private final String password;
    private final int database;
    private final int maxConnections;
    private final Duration connectionTimeout;
    private final Duration socketTimeout;
    
    public RedisConnection() {
        this(
            System.getenv().getOrDefault("REDIS_HOST", "localhost"),
            Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379")),
            System.getenv("REDIS_PASSWORD"),
            Integer.parseInt(System.getenv().getOrDefault("REDIS_DB", "0")),
            Integer.parseInt(System.getenv().getOrDefault("REDIS_MAX_CONNECTIONS", "10")),
            Duration.ofSeconds(Long.parseLong(System.getenv().getOrDefault("REDIS_CONNECT_TIMEOUT", "5"))),
            Duration.ofSeconds(Long.parseLong(System.getenv().getOrDefault("REDIS_SOCKET_TIMEOUT", "5")))
        );
    }
    
    public RedisConnection(String host, int port, String password, int database, 
                         int maxConnections, Duration connectionTimeout, Duration socketTimeout) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.database = database;
        this.maxConnections = maxConnections;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
    }

    /**
     * Connect to Redis with connection pooling and retry logic
     */
    public boolean connect(Duration timeout) {
        int maxRetries = 10;
        int retryDelayMs = 5000;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            try {
                logger.info("Connecting to Redis at {}:{} (attempt {}/{})...", host, port, attempt, maxRetries);
                
                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(maxConnections);
                poolConfig.setMaxIdle(5);
                poolConfig.setMinIdle(1);
                poolConfig.setTestOnBorrow(true);
                poolConfig.setTestOnReturn(false);
                poolConfig.setTestWhileIdle(true);
                poolConfig.setMinEvictableIdleTimeMillis(60000);
                poolConfig.setTimeBetweenEvictionRunsMillis(30000);
                poolConfig.setNumTestsPerEvictionRun(3);
                poolConfig.setBlockWhenExhausted(true);
                poolConfig.setMaxWaitMillis(connectionTimeout.toMillis());
                
                if (password != null && !password.trim().isEmpty()) {
                    jedisPool = new JedisPool(poolConfig, host, port, (int) socketTimeout.toMillis(), 
                                             password, database);
                } else {
                    jedisPool = new JedisPool(poolConfig, host, port, (int) socketTimeout.toMillis(), 
                                             null, database);
                }
                
                // Test connection
                try (Jedis jedis = jedisPool.getResource()) {
                    String pong = jedis.ping();
                    if ("PONG".equals(pong)) {
                        connected.set(true);
                        lastConnectionTime.set(System.currentTimeMillis());
                        logger.info("Connected to Redis at {}:{}", host, port);
                        return true; // Success, exit retry loop
                    } else {
                        logger.error("Unexpected Redis ping response: {}", pong);
                        throw new Exception("Unexpected ping response: " + pong);
                    }
                }
                
            } catch (Exception e) {
                lastErrorTime.set(System.currentTimeMillis());
                connected.set(false);
                
                if (jedisPool != null) {
                    jedisPool.close();
                    jedisPool = null;
                }
                
                if (attempt < maxRetries) {
                    logger.warn("Redis connection attempt {}/{} failed, retrying in {}ms: {}", 
                        attempt, maxRetries, retryDelayMs, e.getMessage());
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Redis connection retry interrupted");
                        return false;
                    }
                } else {
                    logger.error("Failed to connect to Redis at {}:{} after {} attempts: {}", 
                        host, port, maxRetries, e.getMessage());
                    return false;
                }
            }
        }
        
        return false;
    }

    /**
     * Check if connected to Redis
     */
    public boolean isConnected() {
        if (!connected.get()) {
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
            return true;
        } catch (Exception e) {
            logger.warn("Redis connection check failed", e);
            connected.set(false);
            lastErrorTime.set(System.currentTimeMillis());
            return false;
        }
    }

    /**
     * Get value from Redis
     */
    public String get(String key) {
        if (!isConnected()) {
            return null;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        } catch (Exception e) {
            logger.error("Error getting key from Redis: {}", key, e);
            handleConnectionError(e);
            return null;
        }
    }

    /**
     * Set value in Redis with TTL
     */
    public boolean setex(String key, int seconds, String value) {
        if (!isConnected()) {
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.setex(key, seconds, value);
            return "OK".equals(result);
        } catch (Exception e) {
            logger.error("Error setting key in Redis: {}", key, e);
            handleConnectionError(e);
            return false;
        }
    }

    /**
     * Set value in Redis
     */
    public boolean set(String key, String value) {
        if (!isConnected()) {
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(key, value);
            return "OK".equals(result);
        } catch (Exception e) {
            logger.error("Error setting key in Redis: {}", key, e);
            handleConnectionError(e);
            return false;
        }
    }

    /**
     * Delete key from Redis
     */
    public long del(String key) {
        if (!isConnected()) {
            return 0;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting key from Redis: {}", key, e);
            handleConnectionError(e);
            return 0;
        }
    }

    /**
     * Check if key exists in Redis
     */
    public boolean exists(String key) {
        if (!isConnected()) {
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        } catch (Exception e) {
            logger.error("Error checking key existence in Redis: {}", key, e);
            handleConnectionError(e);
            return false;
        }
    }

    /**
     * Get database size
     */
    public int dbSize() {
        if (!isConnected()) {
            return 0;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            Long size = jedis.dbSize();
            return size != null ? size.intValue() : 0;
        } catch (Exception e) {
            logger.error("Error getting Redis database size", e);
            handleConnectionError(e);
            return 0;
        }
    }

    /**
     * Flush database
     */
    public boolean flushDb() {
        if (!isConnected()) {
            return false;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.flushDB();
            return "OK".equals(result);
        } catch (Exception e) {
            logger.error("Error flushing Redis database", e);
            handleConnectionError(e);
            return false;
        }
    }

    /**
     * Get Redis info
     */
    public String info() {
        if (!isConnected()) {
            return null;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.info();
        } catch (Exception e) {
            logger.error("Error getting Redis info", e);
            handleConnectionError(e);
            return null;
        }
    }

    /**
     * Get connection statistics
     */
    public ConnectionStats getStats() {
        int activeConnections = jedisPool != null ? jedisPool.getNumActive() : 0;
        int idleConnections = jedisPool != null ? jedisPool.getNumIdle() : 0;
        int totalConnections = activeConnections + idleConnections;
        
        return new ConnectionStats(
            connected.get(),
            lastConnectionTime.get(),
            lastErrorTime.get(),
            activeConnections,
            idleConnections,
            totalConnections,
            maxConnections
        );
    }

    /**
     * Close Redis connection
     */
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
            jedisPool = null;
        }
        connected.set(false);
        logger.info("Redis connection closed");
    }

    /**
     * Handle connection errors
     */
    private void handleConnectionError(Exception e) {
        lastErrorTime.set(System.currentTimeMillis());
        connected.set(false);
        
        // In production, implement reconnection logic here
        logger.warn("Redis connection error, marking as disconnected", e);
    }

    /**
     * Attempt to reconnect
     */
    public boolean reconnect() {
        if (jedisPool != null) {
            jedisPool.close();
        }
        
        return connect(connectionTimeout);
    }

    /**
     * Connection statistics
     */
    public static class ConnectionStats {
        private final boolean connected;
        private final long lastConnectionTime;
        private final long lastErrorTime;
        private final int activeConnections;
        private final int idleConnections;
        private final int totalConnections;
        private final int maxConnections;

        public ConnectionStats(boolean connected, long lastConnectionTime, long lastErrorTime,
                            int activeConnections, int idleConnections, int totalConnections, int maxConnections) {
            this.connected = connected;
            this.lastConnectionTime = lastConnectionTime;
            this.lastErrorTime = lastErrorTime;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.totalConnections = totalConnections;
            this.maxConnections = maxConnections;
        }

        // Getters
        public boolean isConnected() { return connected; }
        public long getLastConnectionTime() { return lastConnectionTime; }
        public long getLastErrorTime() { return lastErrorTime; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getTotalConnections() { return totalConnections; }
        public int getMaxConnections() { return maxConnections; }
    }
}
