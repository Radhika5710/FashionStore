package com.fashionstore.util;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);
    private static final String LOG_FILE = "logs/audit.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log an audit event
     */
    public static void log(String userId, String action, String details, HttpServletRequest request) {
        String logEntry = String.format("[%s] User: %s | Action: %s | Details: %s | IP: %s | URI: %s",
            LocalDateTime.now().format(FORMATTER),
            userId != null ? userId : "anonymous",
            action,
            details != null ? details : "N/A",
            getClientIp(request),
            request != null ? request.getRequestURI() : "N/A"
        );

        writeLog(logEntry);
    }

    /**
     * Log a security event
     */
    public static void logSecurityEvent(String eventType, String details, HttpServletRequest request) {
        String logEntry = String.format("[%s] SECURITY EVENT | Type: %s | Details: %s | IP: %s | URI: %s",
            LocalDateTime.now().format(FORMATTER),
            eventType,
            details != null ? details : "N/A",
            getClientIp(request),
            request != null ? request.getRequestURI() : "N/A"
        );

        writeLog(logEntry);
    }

    /**
     * Log an error event
     */
    public static void logError(String errorType, String details, HttpServletRequest request) {
        String logEntry = String.format("[%s] ERROR | Type: %s | Details: %s | IP: %s | URI: %s",
            LocalDateTime.now().format(FORMATTER),
            errorType,
            details != null ? details : "N/A",
            getClientIp(request),
            request != null ? request.getRequestURI() : "N/A"
        );

        writeLog(logEntry);
    }

    private static void writeLog(String logEntry) {
        try {
            Path logPath = Paths.get(LOG_FILE);
            
            // Create logs directory if it doesn't exist
            if (!Files.exists(logPath.getParent())) {
                Files.createDirectories(logPath.getParent());
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                writer.write(logEntry);
                writer.newLine();
            }
        } catch (IOException e) {
            // Fallback to console if file logging fails
            logger.warn("Audit log write failed: {}", logEntry);
        }
    }

    private static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
