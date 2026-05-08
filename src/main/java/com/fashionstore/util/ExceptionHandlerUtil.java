package com.fashionstore.util;

import com.fashionstore.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for centralized exception handling
 * Provides consistent error handling across the application
 */
public class ExceptionHandlerUtil {
    
    private static final Logger LOGGER = Logger.getLogger(ExceptionHandlerUtil.class.getName());
    
    /**
     * Handle exceptions in a standardized way
     * Logs error details and forwards to appropriate error page
     */
    public static void handleException(HttpServletRequest request, 
                                     HttpServletResponse response,
                                     Exception exception,
                                     String context) {
        
        // Log the error with full details
        logException(exception, context, request);
        
        // Determine error code and user message
        int statusCode = determineStatusCode(exception);
        String userMessage = determineUserMessage(exception);
        String errorCode = determineErrorCode(exception);
        
        // Set response status
        response.setStatus(statusCode);
        
        // Store error details in request for JSP rendering
        request.setAttribute("errorStatus", statusCode);
        request.setAttribute("errorMessage", userMessage);
        request.setAttribute("errorCode", errorCode);
        request.setAttribute("errorContext", context);
        
        // Only include debug info in development mode
        if (isDevelopmentMode()) {
            request.setAttribute("errorDetails", getStackTraceString(exception));
            request.setAttribute("errorClass", exception.getClass().getName());
        }
        
        // Forward to error page
        try {
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        } catch (Exception e) {
            // Fallback if error page fails
            LOGGER.log(Level.SEVERE, "Failed to forward to error page", e);
            sendBasicErrorResponse(response, statusCode, userMessage);
        }
    }
    
    /**
     * Handle DAO exceptions specifically
     */
    public static void handleDAOException(HttpServletRequest request,
                                        HttpServletResponse response,
                                        SQLException exception,
                                        String operation) {
        
        LOGGER.log(Level.SEVERE, "Database error during " + operation + ": " + exception.getMessage(), exception);
        
        int statusCode = 500;
        String userMessage = "A database error occurred. Please try again later.";
        
        // Check for specific SQL error codes
        int sqlErrorCode = exception.getErrorCode();
        String sqlState = exception.getSQLState();
        
        if (sqlErrorCode == 1062) { // MySQL duplicate entry
            statusCode = 409;
            userMessage = "This record already exists.";
        } else if (sqlErrorCode == 1452) { // MySQL foreign key constraint
            statusCode = 400;
            userMessage = "Invalid reference. The related record does not exist.";
        } else if (sqlErrorCode == 1048) { // MySQL column cannot be null
            statusCode = 400;
            userMessage = "Required information is missing.";
        } else if (sqlState != null && sqlState.startsWith("08")) { // Connection errors
            statusCode = 503;
            userMessage = "Database connection failed. Service temporarily unavailable.";
        }
        
        request.setAttribute("errorStatus", statusCode);
        request.setAttribute("errorMessage", userMessage);
        request.setAttribute("errorCode", "DB_" + sqlErrorCode);
        request.setAttribute("errorContext", operation);
        
        try {
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        } catch (Exception e) {
            sendBasicErrorResponse(response, statusCode, userMessage);
        }
    }
    
    /**
     * Handle null pointer exceptions with detailed context
     */
    public static void handleNullPointerException(HttpServletRequest request,
                                                HttpServletResponse response,
                                                NullPointerException exception,
                                                String nullVariable) {
        
        LOGGER.log(Level.SEVERE, "Null pointer accessing: " + nullVariable, exception);
        
        request.setAttribute("errorStatus", 500);
        request.setAttribute("errorMessage", "An internal error occurred. Please try again.");
        request.setAttribute("errorCode", "NPE_001");
        request.setAttribute("errorContext", "Null value: " + nullVariable);
        
        try {
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        } catch (Exception e) {
            sendBasicErrorResponse(response, 500, "An internal error occurred.");
        }
    }
    
    /**
     * Handle validation exceptions
     */
    public static void handleValidationException(HttpServletRequest request,
                                             HttpServletResponse response,
                                             String fieldName,
                                             String validationMessage) {
        
        LOGGER.log(Level.WARNING, "Validation failed for field " + fieldName + ": " + validationMessage);
        
        request.setAttribute("errorStatus", 400);
        request.setAttribute("errorMessage", validationMessage);
        request.setAttribute("errorCode", "VAL_001");
        request.setAttribute("errorField", fieldName);
        request.setAttribute("errorContext", "Validation");
        
        try {
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        } catch (Exception e) {
            sendBasicErrorResponse(response, 400, validationMessage);
        }
    }
    
    /**
     * Safe wrapper for controller operations
     */
    public static boolean safeExecute(HttpServletRequest request,
                                    HttpServletResponse response,
                                    SafeOperation operation,
                                    String context) {
        try {
            operation.execute();
            return true;
        } catch (ApplicationException e) {
            LOGGER.log(Level.WARNING, "Application exception in " + context + ": " + e.getMessage());
            request.setAttribute("errorStatus", e.getErrorCode().getHttpStatus());
            request.setAttribute("errorMessage", e.getUserMessage());
            request.setAttribute("errorCode", e.getErrorCode().name());
            
            try {
                request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
            } catch (Exception fwdException) {
                sendBasicErrorResponse(response, e.getErrorCode().getHttpStatus(), e.getUserMessage());
            }
            return false;
        } catch (SQLException e) {
            handleDAOException(request, response, e, context);
            return false;
        } catch (NullPointerException e) {
            handleNullPointerException(request, response, e, context);
            return false;
        } catch (Exception e) {
            handleException(request, response, e, context);
            return false;
        }
    }
    
    /**
     * Determine appropriate HTTP status code
     */
    private static int determineStatusCode(Exception exception) {
        if (exception instanceof ApplicationException) {
            return ((ApplicationException) exception).getErrorCode().getHttpStatus();
        }
        if (exception instanceof IllegalArgumentException) {
            return 400;
        }
        if (exception instanceof SecurityException) {
            return 403;
        }
        return 500;
    }
    
    /**
     * Determine user-friendly error message
     */
    private static String determineUserMessage(Exception exception) {
        if (exception instanceof ApplicationException) {
            return ((ApplicationException) exception).getUserMessage();
        }
        return "An unexpected error occurred. Please try again later.";
    }
    
    /**
     * Determine error code
     */
    private static String determineErrorCode(Exception exception) {
        if (exception instanceof ApplicationException) {
            return ((ApplicationException) exception).getErrorCode().name();
        }
        return "ERR_" + exception.getClass().getSimpleName().toUpperCase();
    }
    
    /**
     * Log exception with full context
     */
    private static void logException(Exception exception, String context, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        String logMessage = String.format(
            "Exception in %s | Client: %s | Method: %s | URI: %s | Error: %s",
            context, clientIp, method, requestUri, exception.getMessage()
        );
        
        LOGGER.log(Level.SEVERE, logMessage, exception);
        
        // Also log to audit logger for security events
        if (exception instanceof SecurityException) {
            AuditLogger.log("SECURITY_EXCEPTION", logMessage, "system", request);
        }
    }
    
    /**
     * Get stack trace as string
     */
    private static String getStackTraceString(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.toString()).append("\n");
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Check if running in development mode
     */
    private static boolean isDevelopmentMode() {
        String env = System.getenv("APP_ENV");
        return env == null || env.equals("development") || env.equals("dev");
    }
    
    /**
     * Send basic error response as fallback
     */
    private static void sendBasicErrorResponse(HttpServletResponse response, int status, String message) {
        try {
            response.setContentType("text/html");
            response.getWriter().write(
                "<!DOCTYPE html>" +
                "<html><head><title>Error</title></head>" +
                "<body style='font-family: Arial; padding: 40px; text-align: center;'>" +
                "<h1 style='color: #d32f2f;'>Error " + status + "</h1>" +
                "<p style='color: #666;'>" + message + "</p>" +
                "<a href='/home' style='color: #1976d2;'>Return Home</a>" +
                "</body></html>"
            );
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send basic error response", e);
        }
    }
    
    /**
     * Functional interface for safe operations
     */
    @FunctionalInterface
    public interface SafeOperation {
        void execute() throws Exception;
    }
}
