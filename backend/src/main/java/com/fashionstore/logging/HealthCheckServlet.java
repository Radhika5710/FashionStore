package com.fashionstore.logging;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint for monitoring and load balancer health checks
 * Provides application health status with detailed diagnostics
 */
@WebServlet("/health")
public class HealthCheckServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckServlet.class);
    
    @Override
    public void init() throws ServletException {
        logger.info("Health check servlet initialized");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String detailed = request.getParameter("detailed");
        boolean isDetailed = "true".equalsIgnoreCase(detailed);
        
        try {
            HealthStatus healthStatus = performHealthCheck();
            
            // Set response status based on health
            response.setStatus(healthStatus.isHealthy() ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            // Write health status
            PrintWriter writer = response.getWriter();
            if (isDetailed) {
                writer.write(healthStatus.toJsonDetailed());
            } else {
                writer.write(healthStatus.toJson());
            }
            writer.flush();
            
            // Log health check
            if (!healthStatus.isHealthy()) {
                logger.warn("Health check failed - status: {}, issues: {}", 
                           healthStatus.getStatus(), healthStatus.getIssues());
            }
            
        } catch (Exception e) {
            logger.error("Error performing health check", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"UNHEALTHY\",\"timestamp\":\"" + Instant.now() + "\"}");
        }
    }
    
    /**
     * Perform comprehensive health check
     */
    private HealthStatus performHealthCheck() {
        HealthStatus status = new HealthStatus();
        
        // Check system resources
        checkSystemResources(status);
        
        // Determine overall health
        status.calculateOverallHealth();
        
        return status;
    }
    
    /**
     * Check system resources
     */
    private void checkSystemResources(HealthStatus status) {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            // Memory usage
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            status.addMetric("memory_used", usedMemory);
            status.addMetric("memory_max", maxMemory);
            status.addMetric("memory_usage_percent", memoryUsagePercent);
            
            if (memoryUsagePercent > 90) {
                status.addIssue("High memory usage: " + String.format("%.1f%%", memoryUsagePercent));
            }
            
            // CPU usage (approximate)
            double cpuUsage = -1;
            try {
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    cpuUsage = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad();
                }
            } catch (Exception e) {
                // Method not available on this platform
            }
            if (cpuUsage >= 0) {
                status.addMetric("cpu_usage_percent", cpuUsage * 100);
                
                if (cpuUsage > 0.8) {
                    status.addIssue("High CPU usage: " + String.format("%.1f%%", cpuUsage * 100));
                }
            }
            
            // System load average
            double loadAverage = osBean.getSystemLoadAverage();
            if (loadAverage >= 0) {
                status.addMetric("system_load_average", loadAverage);
                
                int processors = osBean.getAvailableProcessors();
                if (loadAverage > processors * 2) {
                    status.addIssue("High system load: " + String.format("%.2f", loadAverage));
                }
            }
            
        } catch (Exception e) {
            status.addIssue("Failed to check system resources: " + e.getMessage());
        }
    }
    
    /**
     * Health status data structure
     */
    private static class HealthStatus {
        private String status = "UNKNOWN";
        private boolean healthy = false;
        private final Map<String, Object> metrics = new HashMap<>();
        private final Map<String, String> details = new HashMap<>();
        private final java.util.List<String> issues = new java.util.ArrayList<>();
        private final long timestamp = System.currentTimeMillis();
        
        public void addMetric(String name, Object value) {
            metrics.put(name, value);
        }
        
        public void addDetail(String name, String value) {
            details.put(name, value);
        }
        
        public void addIssue(String issue) {
            issues.add(issue);
        }
        
        public void calculateOverallHealth() {
            if (issues.isEmpty()) {
                status = "HEALTHY";
                healthy = true;
            } else if (issues.size() <= 2) {
                status = "DEGRADED";
                healthy = true;
            } else {
                status = "UNHEALTHY";
                healthy = false;
            }
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        public String getStatus() {
            return status;
        }
        
        public Map<String, Object> getMetrics() {
            return metrics;
        }
        
        public java.util.List<String> getIssues() {
            return issues;
        }
        
        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"status\":\"").append(status).append("\",");
            sb.append("\"timestamp\":").append(timestamp).append(",");
            sb.append("\"healthy\":").append(healthy);
            if (!issues.isEmpty()) {
                sb.append(",\"issues\":[");
                for (int i = 0; i < issues.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append("\"").append(issues.get(i).replace("\"", "\\\"")).append("\"");
                }
                sb.append("]");
            }
            sb.append("}");
            return sb.toString();
        }
        
        public String toJsonDetailed() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"status\":\"").append(status).append("\",");
            sb.append("\"timestamp\":").append(timestamp).append(",");
            sb.append("\"healthy\":").append(healthy).append(",");
            sb.append("\"application\":\"FashionStore\",");
            sb.append("\"version\":\"1.0.0\",");
            
            // Metrics
            sb.append("\"metrics\":{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : metrics.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
                first = false;
            }
            sb.append("},");
            
            // Issues
            sb.append("\"issues\":[");
            for (int i = 0; i < issues.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(issues.get(i).replace("\"", "\\\"")).append("\"");
            }
            sb.append("],");
            
            // Details
            sb.append("\"details\":{");
            first = true;
            for (Map.Entry<String, String> entry : details.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue().replace("\"", "\\\"")).append("\"");
                first = false;
            }
            sb.append("}");
            
            sb.append("}");
            return sb.toString();
        }
    }
}
