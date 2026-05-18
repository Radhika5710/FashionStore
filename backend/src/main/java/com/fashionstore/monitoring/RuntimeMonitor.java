package com.fashionstore.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runtime monitoring for memory, threads, and performance metrics
 * Provides comprehensive monitoring for production readiness
 */
public class RuntimeMonitor {
    private static final Logger logger = LoggerFactory.getLogger(RuntimeMonitor.class);
    
    // Monitoring data storage
    private static final Map<String, Object> metrics = new ConcurrentHashMap<>();
    private static final AtomicLong totalRequests = new AtomicLong(0);
    private static final AtomicLong activeRequests = new AtomicLong(0);
    private static final AtomicLong failedRequests = new AtomicLong(0);
    
    // Monitoring components
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private static final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    
    // Database connection pool monitoring
    private static final Map<String, Object> dbPoolMetrics = new ConcurrentHashMap<>();
    
    // Cache monitoring
    private static final Map<String, Object> cacheMetrics = new ConcurrentHashMap<>();
    
    // Scheduled executor for periodic monitoring
    private static final ScheduledExecutorService monitorExecutor = Executors.newScheduledThreadPool(2);
    
    // Alert thresholds
    private static final double MEMORY_USAGE_THRESHOLD = 0.8; // 80%
    private static final int THREAD_COUNT_THRESHOLD = 200;
    private static final double CPU_USAGE_THRESHOLD = 0.8; // 80%
    private static final int FAILED_REQUEST_THRESHOLD = 100; // per minute
    
    static {
        // Start monitoring tasks
        startMonitoringTasks();
    }
    
    /**
     * Start all monitoring tasks
     */
    private static void startMonitoringTasks() {
        // Memory monitoring every 30 seconds
        monitorExecutor.scheduleAtFixedRate(RuntimeMonitor::monitorMemory, 
                                           0, 30, TimeUnit.SECONDS);
        
        // Thread monitoring every 30 seconds
        monitorExecutor.scheduleAtFixedRate(RuntimeMonitor::monitorThreads, 
                                           0, 30, TimeUnit.SECONDS);
        
        // System metrics every 60 seconds
        monitorExecutor.scheduleAtFixedRate(RuntimeMonitor::monitorSystemMetrics, 
                                           0, 60, TimeUnit.SECONDS);
        
        // Request metrics every 60 seconds
        monitorExecutor.scheduleAtFixedRate(RuntimeMonitor::monitorRequestMetrics, 
                                           0, 60, TimeUnit.SECONDS);
        
        // Leak detection every 5 minutes
        monitorExecutor.scheduleAtFixedRate(RuntimeMonitor::detectLeaks, 
                                           0, 5, TimeUnit.MINUTES);
        
        // Performance analysis every 10 minutes
        monitorExecutor.scheduleAtFixedRate(RuntimeMonitor::analyzePerformance, 
                                           0, 10, TimeUnit.MINUTES);
        
        // Stock reservation sweeper every 1 minute to release abandoned stocks
        monitorExecutor.scheduleAtFixedRate(RuntimeMonitor::sweepExpiredReservations, 
                                           1, 1, TimeUnit.MINUTES);
        
        logger.info("Runtime monitoring started");
    }
    
    /**
     * Monitor memory usage
     */
    private static void monitorMemory() {
        try {
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
            
            double heapUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax();
            double nonHeapUsagePercent = nonHeapUsage.getMax() > 0 ? 
                (double) nonHeapUsage.getUsed() / nonHeapUsage.getMax() : 0.0;
            
            Map<String, Object> memoryMetrics = new HashMap<>();
            memoryMetrics.put("heapUsed", heapUsage.getUsed());
            memoryMetrics.put("heapMax", heapUsage.getMax());
            memoryMetrics.put("heapUsagePercent", heapUsagePercent);
            memoryMetrics.put("nonHeapUsed", nonHeapUsage.getUsed());
            memoryMetrics.put("nonHeapMax", nonHeapUsage.getMax());
            memoryMetrics.put("nonHeapUsagePercent", nonHeapUsagePercent);
            
            metrics.put("memory", memoryMetrics);
            
            // Check for memory warnings
            if (heapUsagePercent > MEMORY_USAGE_THRESHOLD) {
                logger.warn("High memory usage detected: {}%", String.format("%.2f", heapUsagePercent * 100));
                triggerAlert("HIGH_MEMORY", "Memory usage: " + String.format("%.2f", heapUsagePercent * 100) + "%");
            }
            
            // Get detailed memory information
            List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
            Map<String, Object> poolMetrics = new HashMap<>();
            
            for (MemoryPoolMXBean pool : memoryPools) {
                MemoryUsage usage = pool.getUsage();
                if (usage != null) {
                    Map<String, Object> poolData = new HashMap<>();
                    poolData.put("used", usage.getUsed());
                    poolData.put("max", usage.getMax());
                    poolData.put("usagePercent", usage.getMax() > 0 ? (double) usage.getUsed() / usage.getMax() : 0.0);
                    poolMetrics.put(pool.getName(), poolData);
                }
            }
            
            metrics.put("memoryPools", poolMetrics);
            
        } catch (Exception e) {
            logger.error("Error monitoring memory", e);
        }
    }
    
    /**
     * Monitor thread usage
     */
    private static void monitorThreads() {
        try {
            int threadCount = threadMXBean.getThreadCount();
            int peakThreadCount = threadMXBean.getPeakThreadCount();
            long totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();
            long daemonThreadCount = threadMXBean.getDaemonThreadCount();
            
            Map<String, Object> threadMetrics = new HashMap<>();
            threadMetrics.put("threadCount", threadCount);
            threadMetrics.put("peakThreadCount", peakThreadCount);
            threadMetrics.put("totalStartedThreadCount", totalStartedThreadCount);
            threadMetrics.put("daemonThreadCount", daemonThreadCount);
            
            // Get thread info
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds());
            Map<String, Object> threadStates = new HashMap<>();
            
            for (ThreadInfo threadInfo : threadInfos) {
                if (threadInfo != null) {
                    Thread.State state = threadInfo.getThreadState();
                    String stateStr = state.toString();
                    Object count = threadStates.get(stateStr);
                    int currentCount = (count instanceof Integer) ? (Integer) count : 0;
                    threadStates.put(stateStr, currentCount + 1);
                }
            }
            
            threadMetrics.put("threadStates", threadStates);
            
            // Check for thread warnings
            if (threadCount > THREAD_COUNT_THRESHOLD) {
                logger.warn("High thread count detected: {}", threadCount);
                triggerAlert("HIGH_THREADS", "Thread count: " + threadCount);
            }
            
            // Check for deadlocked threads
            long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
            if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                logger.error("Deadlocked threads detected: {}", deadlockedThreads.length);
                triggerAlert("DEADLOCK", "Deadlocked threads: " + deadlockedThreads.length);
            }
            
            metrics.put("threads", threadMetrics);
            
        } catch (Exception e) {
            logger.error("Error monitoring threads", e);
        }
    }
    
    /**
     * Monitor system metrics
     */
    private static void monitorSystemMetrics() {
        try {
            Map<String, Object> systemMetrics = new HashMap<>();
            
            // CPU usage
            double cpuUsage = -1;
            try {
                if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                    cpuUsage = ((com.sun.management.OperatingSystemMXBean) osMXBean).getProcessCpuLoad();
                }
            } catch (Exception e) {
                // Method not available on this platform
            }
            if (cpuUsage >= 0) {
                systemMetrics.put("cpuUsage", cpuUsage);
                
                if (cpuUsage > CPU_USAGE_THRESHOLD) {
                    logger.warn("High CPU usage detected: {}%", String.format("%.2f", cpuUsage * 100));
                    triggerAlert("HIGH_CPU", "CPU usage: " + String.format("%.2f", cpuUsage * 100) + "%");
                }
            }
            
            // System load average
            double systemLoadAverage = osMXBean.getSystemLoadAverage();
            if (systemLoadAverage >= 0) {
                systemMetrics.put("systemLoadAverage", systemLoadAverage);
            }
            
            // Memory
            long totalPhysicalMemory = -1;
            long freePhysicalMemory = -1;
            try {
                if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                    totalPhysicalMemory = ((com.sun.management.OperatingSystemMXBean) osMXBean).getTotalPhysicalMemorySize();
                    freePhysicalMemory = ((com.sun.management.OperatingSystemMXBean) osMXBean).getFreePhysicalMemorySize();
                }
            } catch (Exception e) {
                // Method not available on this platform
            }
            if (totalPhysicalMemory >= 0 && freePhysicalMemory >= 0) {
                long usedPhysicalMemory = totalPhysicalMemory - freePhysicalMemory;
                systemMetrics.put("totalPhysicalMemory", totalPhysicalMemory);
                systemMetrics.put("freePhysicalMemory", freePhysicalMemory);
                systemMetrics.put("usedPhysicalMemory", usedPhysicalMemory);
                systemMetrics.put("physicalMemoryUsagePercent", (double) usedPhysicalMemory / totalPhysicalMemory);
            }
            
            // Runtime information
            long uptime = runtimeMXBean.getUptime();
            systemMetrics.put("uptime", uptime);
            systemMetrics.put("uptimeHours", uptime / (1000 * 60 * 60));
            
            metrics.put("system", systemMetrics);
            
        } catch (Exception e) {
            logger.error("Error monitoring system metrics", e);
        }
    }
    
    /**
     * Monitor request metrics
     */
    private static void monitorRequestMetrics() {
        try {
            Map<String, Object> requestMetrics = new HashMap<>();
            requestMetrics.put("totalRequests", totalRequests.get());
            requestMetrics.put("activeRequests", activeRequests.get());
            requestMetrics.put("failedRequests", failedRequests.get());
            
            // Calculate requests per minute
            long failedPerMinute = failedRequests.get();
            if (failedPerMinute > FAILED_REQUEST_THRESHOLD) {
                logger.warn("High failure rate detected: {} failures per minute", failedPerMinute);
                triggerAlert("HIGH_FAILURE_RATE", "Failure rate: " + failedPerMinute + " per minute");
            }
            
            // Reset counters for next minute
            failedRequests.set(0);
            
            metrics.put("requests", requestMetrics);
            
        } catch (Exception e) {
            logger.error("Error monitoring request metrics", e);
        }
    }
    
    /**
     * Detect potential leaks
     */
    private static void detectLeaks() {
        try {
            // Memory leak detection
            detectMemoryLeaks();
            
            // Thread leak detection
            detectThreadLeaks();
            
            // Connection leak detection
            detectConnectionLeaks();
            
            // Cache leak detection
            detectCacheLeaks();
            
        } catch (Exception e) {
            logger.error("Error during leak detection", e);
        }
    }
    
    /**
     * Detect memory leaks
     */
    private static void detectMemoryLeaks() {
        try {
            // Check for increasing memory usage over time
            Object memObj = metrics.get("memory");
            if (memObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> memoryMetrics = (Map<String, Object>) memObj;
                Object heapObj = memoryMetrics.get("heapUsagePercent");
                if (heapObj instanceof Double) {
                    Double heapUsagePercent = (Double) heapObj;
                    if (heapUsagePercent > 0.9) {
                        logger.warn("Potential memory leak detected - heap usage: {}%", 
                                   String.format("%.2f", heapUsagePercent * 100));
                        triggerAlert("MEMORY_LEAK", "Potential memory leak detected");
                    }
                }
            }
            
            // Check for increasing non-heap memory
            Object poolObj = metrics.get("memoryPools");
            if (poolObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> poolMetrics = (Map<String, Object>) poolObj;
                for (Object poolDataObj : poolMetrics.values()) {
                    if (poolDataObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> poolData = (Map<String, Object>) poolDataObj;
                        Object usageObj = poolData.get("usagePercent");
                        if (usageObj instanceof Double) {
                            Double usagePercent = (Double) usageObj;
                            if (usagePercent > 0.8) {
                                logger.warn("High memory pool usage detected: {}%", 
                                           String.format("%.2f", usagePercent * 100));
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error detecting memory leaks", e);
        }
    }
    
    /**
     * Detect thread leaks
     */
    private static void detectThreadLeaks() {
        try {
            Object threadObj = metrics.get("threads");
            if (threadObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> threadMetrics = (Map<String, Object>) threadObj;
                Object countObj = threadMetrics.get("threadCount");
                Object peakObj = threadMetrics.get("peakThreadCount");
                
                if (countObj instanceof Integer && peakObj instanceof Integer) {
                    Integer threadCount = (Integer) countObj;
                    Integer peakThreadCount = (Integer) peakObj;
                    
                    if (threadCount > peakThreadCount * 0.9) {
                        logger.warn("Potential thread leak detected - current: {}, peak: {}", 
                                   threadCount, peakThreadCount);
                        triggerAlert("THREAD_LEAK", "Potential thread leak detected");
                    }
                }
                
                // Check for threads stuck in certain states
                Object statesObj = threadMetrics.get("threadStates");
                if (statesObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> threadStates = (Map<String, Object>) statesObj;
                    for (Object entryObj : threadStates.entrySet()) {
                        if (entryObj instanceof Map.Entry) {
                            @SuppressWarnings("unchecked")
                            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) entryObj;
                            String state = entry.getKey();
                            Object valueObj = entry.getValue();
                            if (valueObj instanceof Integer) {
                                Integer count = (Integer) valueObj;
                                
                                if ("BLOCKED".equals(state) && count > 10) {
                                    logger.warn("High number of blocked threads: {}", count);
                                    triggerAlert("BLOCKED_THREADS", "High number of blocked threads: " + count);
                                }
                                
                                if ("WAITING".equals(state) && count > 50) {
                                    logger.warn("High number of waiting threads: {}", count);
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error detecting thread leaks", e);
        }
    }
    
    /**
     * Detect connection leaks
     */
    private static void detectConnectionLeaks() {
        try {
            // This would integrate with your connection pool monitoring
            // For now, just log the current DB pool metrics
            if (!dbPoolMetrics.isEmpty()) {
                logger.debug("DB Pool metrics: {}", dbPoolMetrics);
                
                // Check for potential connection leaks
                Object activeConnections = dbPoolMetrics.get("activeConnections");
                Object maxConnections = dbPoolMetrics.get("maxConnections");
                
                if (activeConnections != null && maxConnections != null) {
                    int active = ((Number) activeConnections).intValue();
                    int max = ((Number) maxConnections).intValue();
                    
                    if (active > max * 0.9) {
                        logger.warn("High DB connection usage: {}/{}", active, max);
                        triggerAlert("DB_CONNECTION_LEAK", "High DB connection usage");
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error detecting connection leaks", e);
        }
    }
    
    /**
     * Detect cache leaks
     */
    private static void detectCacheLeaks() {
        try {
            if (!cacheMetrics.isEmpty()) {
                logger.debug("Cache metrics: {}", cacheMetrics);
                
                // Check for cache size issues
                for (Object cacheObj : cacheMetrics.entrySet()) {
                    Map.Entry<String, Object> entry = (Map.Entry<String, Object>) cacheObj;
                    String cacheName = entry.getKey();
                    Object metricsData = entry.getValue();
                    
                    if (metricsData instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> cacheData = (Map<String, Object>) metricsData;
                        
                        Object size = cacheData.get("size");
                        Object maxSize = cacheData.get("maxSize");
                        
                        if (size != null && maxSize != null) {
                            int currentSize = ((Number) size).intValue();
                            int maxCacheSize = ((Number) maxSize).intValue();
                            
                            if (currentSize > maxCacheSize * 0.9) {
                                logger.warn("Cache {} is near capacity: {}/{}", cacheName, currentSize, maxCacheSize);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error detecting cache leaks", e);
        }
    }
    
    /**
     * Analyze overall performance
     */
    private static void analyzePerformance() {
        try {
            Map<String, Object> performanceReport = new HashMap<>();
            
            // Memory analysis
            Object memObj = metrics.get("memory");
            if (memObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> memoryMetrics = (Map<String, Object>) memObj;
                Object heapObj = memoryMetrics.get("heapUsagePercent");
                
                if (heapObj instanceof Double) {
                    Double heapUsage = (Double) heapObj;
                    performanceReport.put("memoryHealth", heapUsage < 0.7 ? "GOOD" : "WARNING");
                }
            }
            
            // Thread analysis
            Object threadObj = metrics.get("threads");
            if (threadObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> threadMetrics = (Map<String, Object>) threadObj;
                Object countObj = threadMetrics.get("threadCount");
                
                if (countObj instanceof Integer) {
                    Integer threadCount = (Integer) countObj;
                    performanceReport.put("threadHealth", threadCount < THREAD_COUNT_THRESHOLD * 0.8 ? "GOOD" : "WARNING");
                }
            }
            
            // System analysis
            Object sysObj = metrics.get("system");
            if (sysObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> systemMetrics = (Map<String, Object>) sysObj;
                Object cpuObj = systemMetrics.get("cpuUsage");
                
                if (cpuObj instanceof Double) {
                    Double cpuUsage = (Double) cpuObj;
                    performanceReport.put("systemHealth", cpuUsage < CPU_USAGE_THRESHOLD * 0.8 ? "GOOD" : "WARNING");
                }
            }
            
            // Request analysis
            Object reqObj = metrics.get("requests");
            if (reqObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> requestMetrics = (Map<String, Object>) reqObj;
                Object failedObj = requestMetrics.get("failedRequests");
                Object totalObj = requestMetrics.get("totalRequests");
                
                if (failedObj instanceof Long && totalObj instanceof Long) {
                    Long failedRequests = (Long) failedObj;
                    Long totalRequests = (Long) totalObj;
                    double failureRate = totalRequests > 0 ? (double) failedRequests / totalRequests : 0.0;
                    performanceReport.put("requestHealth", failureRate < 0.05 ? "GOOD" : "WARNING");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error analyzing performance", e);
        }
    }
    
    /**
     * Record request start
     */
    public static void recordRequestStart() {
        totalRequests.incrementAndGet();
        activeRequests.incrementAndGet();
    }
    
    /**
     * Record request end
     */
    public static void recordRequestEnd() {
        activeRequests.decrementAndGet();
    }
    
    /**
     * Record failed request
     */
    public static void recordFailedRequest() {
        failedRequests.incrementAndGet();
    }
    
    /**
     * Update database pool metrics
     */
    public static void updateDBPoolMetrics(String poolName, Map<String, Object> poolMetrics) {
        dbPoolMetrics.put(poolName, new HashMap<>(poolMetrics));
    }
    
    /**
     * Update cache metrics
     */
    public static void updateCacheMetrics(String cacheName, Map<String, Object> cacheMetrics) {
        cacheMetrics.put(cacheName, new HashMap<>(cacheMetrics));
    }
    
    /**
     * Get all metrics
     */
    public static Map<String, Object> getAllMetrics() {
        return new HashMap<>(metrics);
    }
    
    /**
     * Get specific metric
     */
    public static Object getMetric(String metricName) {
        return metrics.get(metricName);
    }
    
    /**
     * Trigger alert
     */
    private static void triggerAlert(String alertType, String message) {
        logger.error("ALERT [{}]: {}", alertType, message);
        
        // In a production system, this would send alerts to monitoring systems
        // For now, just log the alert
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", alertType);
        alert.put("message", message);
        alert.put("timestamp", System.currentTimeMillis());
        
        Object alertsObj = metrics.computeIfAbsent("alerts", k -> new ArrayList<>());
        if (alertsObj instanceof java.util.ArrayList) {
            @SuppressWarnings("unchecked")
            java.util.ArrayList<Map<String, Object>> alerts = (java.util.ArrayList<Map<String, Object>>) alertsObj;
            alerts.add(alert);
        }
    }
    
    /**
     * Get health status
     */
    public static Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Memory health
            Object memObj = metrics.get("memory");
            if (memObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> memoryMetrics = (Map<String, Object>) memObj;
                Object heapObj = memoryMetrics.get("heapUsagePercent");
                if (heapObj instanceof Double) {
                    Double heapUsage = (Double) heapObj;
                    health.put("memory", heapUsage < MEMORY_USAGE_THRESHOLD ? "UP" : "DOWN");
                }
            }
            
            // Thread health
            Object threadObj = metrics.get("threads");
            if (threadObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> threadMetrics = (Map<String, Object>) threadObj;
                Object countObj = threadMetrics.get("threadCount");
                if (countObj instanceof Integer) {
                    Integer threadCount = (Integer) countObj;
                    health.put("threads", threadCount < THREAD_COUNT_THRESHOLD ? "UP" : "DOWN");
                }
            }
            
            // Overall health
            boolean overallUp = "UP".equals(health.get("memory")) && 
                               "UP".equals(health.get("threads"));
            health.put("overall", overallUp ? "UP" : "DOWN");
            
        } catch (Exception e) {
            logger.error("Error getting health status", e);
            health.put("overall", "DOWN");
        }
        
        return health;
    }
    
    /**
     * Shutdown monitoring
     */
    public static void shutdown() {
        if (monitorExecutor != null && !monitorExecutor.isShutdown()) {
            monitorExecutor.shutdown();
            try {
                if (!monitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitorExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Runtime monitoring shutdown");
    }

    /**
     * Stock reservation sweeper lifecycle
     * Scans for orders that have been 'Pending' for more than 15 minutes,
     * transitions them to 'Cancelled', and atomically releases/restores their reserved stock.
     */
    private static void sweepExpiredReservations() {
        String selectSql = "SELECT order_id FROM orders " +
                           "WHERE status = 'Pending' " +
                           "AND payment_method IN ('STRIPE', 'RAZORPAY') " +
                           "AND created_at < NOW() - INTERVAL 15 MINUTE";
        
        String updateOrderSql = "UPDATE orders SET status = 'Cancelled', updated_at = NOW() " +
                                "WHERE order_id = ? AND status = 'Pending'";
        
        String selectItemsSql = "SELECT product_id, size_label, quantity FROM order_items WHERE order_id = ?";
        
        try (java.sql.Connection conn = com.fashionstore.util.DBConnection.getConnection()) {
            List<Integer> expiredOrderIds = new ArrayList<>();
            try (java.sql.PreparedStatement ps = conn.prepareStatement(selectSql);
                 java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expiredOrderIds.add(rs.getInt("order_id"));
                }
            }
            
            if (expiredOrderIds.isEmpty()) {
                return;
            }
            
            logger.info("Stock Sweeper: Found {} potentially expired pending orders to sweep: {}", expiredOrderIds.size(), expiredOrderIds);
            
            com.fashionstore.dao.ProductSizeDAO productSizeDAO = new com.fashionstore.daoimpl.ProductSizeDAOImpl();
            
            for (int orderId : expiredOrderIds) {
                conn.setAutoCommit(false);
                try {
                    // Try to atomically update the order to Cancelled (only if it is still Pending!)
                    boolean updated = false;
                    try (java.sql.PreparedStatement updatePs = conn.prepareStatement(updateOrderSql)) {
                        updatePs.setInt(1, orderId);
                        updated = updatePs.executeUpdate() > 0;
                    }
                    
                    if (updated) {
                        logger.info("Stock Sweeper: Atomically transitioned Order #{} to Cancelled. Releasing locked stock...", orderId);
                        
                        // Retrieve the items for this order
                        try (java.sql.PreparedStatement itemsPs = conn.prepareStatement(selectItemsSql)) {
                            itemsPs.setInt(1, orderId);
                            try (java.sql.ResultSet rs = itemsPs.executeQuery()) {
                                while (rs.next()) {
                                    int productId = rs.getInt("product_id");
                                    String sizeLabel = rs.getString("size_label");
                                    int quantity = rs.getInt("quantity");
                                    
                                    boolean stockRestored = productSizeDAO.increaseStock(productId, sizeLabel, quantity);
                                    logger.info("Stock Sweeper: Restored {} units of product ID {} size {} for Order #{}: {}", 
                                                quantity, productId, sizeLabel, orderId, stockRestored);
                                }
                            }
                        }
                        
                        conn.commit();
                        logger.info("Stock Sweeper: Successfully released all stocks for Order #{}", orderId);
                    } else {
                        // The order was already updated by another thread (e.g. payment completed/failed)
                        conn.rollback();
                        logger.debug("Stock Sweeper: Order #{} was already transitioned away from Pending. Skipping.", orderId);
                    }
                } catch (Exception e) {
                    conn.rollback();
                    logger.error("Stock Sweeper: Error sweeping Order #" + orderId, e);
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (Exception e) {
            logger.error("Stock Sweeper error: " + e.getMessage(), e);
        }
    }
}
