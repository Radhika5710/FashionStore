package com.fashionstore.exception;

import com.fashionstore.util.JsonUtil;
import com.fashionstore.util.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for the FashionStore application
 * Provides standardized exception handling, logging, and error responses
 */
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle exceptions and provide standardized responses
     * @param request The HTTP request
     * @param response The HTTP response
     * @param exception The exception to handle
     * @param context Additional context information
     */
    public static void handleException(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      Exception exception, 
                                      ExceptionContext context) {
        
        // Set correlation ID for logging
        String correlationId = (String) request.getAttribute("correlationId");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }

        // Log the exception with structured information
        logException(exception, request, context);

        // Determine if this is an AJAX/API request
        boolean isAjax = isAjaxRequest(request);

        try {
            if (isAjax) {
                handleAjaxException(response, exception, context);
            } else {
                handleJspException(request, response, exception, context);
            }
        } catch (Exception handlingException) {
            logger.error("Error while handling exception", handlingException);
            try {
                sendGenericErrorResponse(response);
            } catch (IOException e) {
                logger.error("Error sending generic error response", e);
            }
        } finally {
            MDC.clear();
        }
    }

    /**
     * Handle exceptions for AJAX/API requests
     */
    private static void handleAjaxException(HttpServletResponse response, 
                                             Exception exception, 
                                             ExceptionContext context) throws IOException {
        
        ErrorResponse errorResponse = createErrorResponse(exception, context);
        
        response.setStatus(errorResponse.getHttpStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = JsonUtil.toJson(errorResponse);
        response.getWriter().write(jsonResponse);
    }

    /**
     * Handle exceptions for JSP requests
     */
    private static void handleJspException(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Exception exception,
                                            ExceptionContext context) throws IOException {
        
        ErrorResponse errorResponse = createErrorResponse(exception, context);
        
        // Set error attributes for JSP
        request.setAttribute("error", errorResponse);
        request.setAttribute("exception", exception);
        request.setAttribute("context", context);
        
        // Determine error page based on exception type
        String errorPage = determineErrorPage(exception);
        
        try {
            request.getRequestDispatcher("/WEB-INF/views/errors/" + errorPage + ".jsp")
                   .forward(request, response);
        } catch (Exception e) {
            logger.error("Failed to forward to error page: {}", errorPage, e);
            sendGenericErrorResponse(response);
        }
    }

    /**
     * Create standardized error response
     */
    private static ErrorResponse createErrorResponse(Exception exception, ExceptionContext context) {
        
        ExceptionType exceptionType = categorizeException(exception);
        int httpStatus = determineHttpStatus(exceptionType);
        String userMessage = getUserMessage(exception, exceptionType);
        String errorCode = generateErrorCode(exceptionType);
        
        return ErrorResponse.builder()
                .success(false)
                .error(errorCode)
                .message(userMessage)
                .type(exceptionType.name())
                .httpStatus(httpStatus)
                .timestamp(System.currentTimeMillis())
                .path(context != null ? context.getPath() : null)
                .build();
    }

    /**
     * Categorize exception type
     */
    private static ExceptionType categorizeException(Exception exception) {
        
        if (exception instanceof FashionStoreException) {
            FashionStoreException fsException = (FashionStoreException) exception;
            return fsException.getType();
        }
        
        // Database exceptions
        if (isDatabaseException(exception)) {
            return ExceptionType.DATABASE_ERROR;
        }
        
        // Validation exceptions
        if (isValidationException(exception)) {
            return ExceptionType.VALIDATION_ERROR;
        }
        
        // Authentication exceptions
        if (isAuthenticationException(exception)) {
            return ExceptionType.AUTHENTICATION_ERROR;
        }
        
        // Authorization exceptions
        if (isAuthorizationException(exception)) {
            return ExceptionType.AUTHORIZATION_ERROR;
        }
        
        // Payment exceptions
        if (isPaymentException(exception)) {
            return ExceptionType.PAYMENT_ERROR;
        }
        
        // Resource not found
        if (exception instanceof java.util.NoSuchElementException ||
            exception.getMessage() != null && exception.getMessage().contains("not found")) {
            return ExceptionType.NOT_FOUND;
        }
        
        // Default to system error
        return ExceptionType.SYSTEM_ERROR;
    }

    /**
     * Determine HTTP status code based on exception type
     */
    private static int determineHttpStatus(ExceptionType exceptionType) {
        return switch (exceptionType) {
            case VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case AUTHENTICATION_ERROR -> HttpServletResponse.SC_UNAUTHORIZED;
            case AUTHORIZATION_ERROR -> HttpServletResponse.SC_FORBIDDEN;
            case NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case PAYMENT_ERROR -> HttpServletResponse.SC_PAYMENT_REQUIRED;
            case DATABASE_ERROR -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            case SYSTEM_ERROR -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Get user-friendly error message
     */
    private static String getUserMessage(Exception exception, ExceptionType exceptionType) {
        
        // If it's a FashionStoreException with user message, use it
        if (exception instanceof FashionStoreException) {
            FashionStoreException fsException = (FashionStoreException) exception;
            if (fsException.getUserMessage() != null && !fsException.getUserMessage().trim().isEmpty()) {
                return fsException.getUserMessage();
            }
        }
        
        // Provide default user messages based on exception type
        return switch (exceptionType) {
            case VALIDATION_ERROR -> "Please check your input and try again.";
            case AUTHENTICATION_ERROR -> "Please log in to continue.";
            case AUTHORIZATION_ERROR -> "You don't have permission to perform this action.";
            case NOT_FOUND -> "The requested resource was not found.";
            case PAYMENT_ERROR -> "Payment processing failed. Please try again.";
            case DATABASE_ERROR -> "Service temporarily unavailable. Please try again later.";
            case SYSTEM_ERROR -> "An unexpected error occurred. Please try again.";
        };
    }

    /**
     * Generate error code for tracking
     */
    private static String generateErrorCode(ExceptionType exceptionType) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        timestamp = timestamp.length() >= 6 ? timestamp.substring(timestamp.length() - 6) : timestamp;
        return switch (exceptionType) {
            case VALIDATION_ERROR -> "VAL_" + timestamp;
            case AUTHENTICATION_ERROR -> "AUTH_" + timestamp;
            case AUTHORIZATION_ERROR -> "PERM_" + timestamp;
            case NOT_FOUND -> "NF_" + timestamp;
            case PAYMENT_ERROR -> "PAY_" + timestamp;
            case DATABASE_ERROR -> "DB_" + timestamp;
            case SYSTEM_ERROR -> "SYS_" + timestamp;
        };
    }

    /**
     * Determine error page for JSP rendering
     */
    private static String determineErrorPage(Exception exception) {
        ExceptionType type = categorizeException(exception);
        return switch (type) {
            case VALIDATION_ERROR -> "400";
            case AUTHENTICATION_ERROR -> "401";
            case AUTHORIZATION_ERROR -> "403";
            case NOT_FOUND -> "404";
            case PAYMENT_ERROR -> "402";
            default -> "500";
        };
    }

    /**
     * Log exception with structured information
     */
    private static void logException(Exception exception, HttpServletRequest request, ExceptionContext context) {
        
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();
        
        // Build structured log data
        Map<String, Object> logData = new HashMap<>();
        logData.put("exceptionType", exception.getClass().getSimpleName());
        logData.put("message", exception.getMessage());
        logData.put("requestUri", requestUri);
        logData.put("method", method);
        logData.put("remoteAddr", remoteAddr);
        logData.put("userAgent", userAgent);
        
        if (context != null) {
            logData.put("context", context.toMap());
        }
        
        // Log at appropriate level
        ExceptionType type = categorizeException(exception);
        switch (type) {
            case SYSTEM_ERROR, DATABASE_ERROR -> logger.error("System error occurred: {}", logData, exception);
            case PAYMENT_ERROR -> logger.warn("Payment error occurred: {}", logData, exception);
            case AUTHORIZATION_ERROR -> logger.warn("Authorization error occurred: {}", logData, exception);
            case AUTHENTICATION_ERROR -> logger.info("Authentication error occurred: {}", logData, exception);
            case VALIDATION_ERROR -> logger.info("Validation error occurred: {}", logData, exception);
            case NOT_FOUND -> logger.info("Resource not found: {}", logData, exception);
        }
        
        // Audit log for security-related exceptions
        if (type == ExceptionType.AUTHENTICATION_ERROR || 
            type == ExceptionType.AUTHORIZATION_ERROR) {
            AuditLogger.log("SECURITY_EXCEPTION", 
                           "Security exception: " + type.name() + " at " + requestUri,
                           null, request);
        }
    }

    /**
     * Check if request is AJAX
     */
    private static boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) ||
               request.getContentType() != null && request.getContentType().contains("application/json") ||
               "application/json".equals(request.getHeader("Accept"));
    }

    /**
     * Send generic error response when all else fails
     */
    private static void sendGenericErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> genericError = new HashMap<>();
        genericError.put("success", false);
        genericError.put("error", "SYS_ERROR");
        genericError.put("message", "An unexpected error occurred. Please try again.");
        genericError.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(JsonUtil.toJson(genericError));
    }

    // Helper methods to identify exception types
    private static boolean isDatabaseException(Exception exception) {
        return exception instanceof java.sql.SQLException ||
               exception.getMessage() != null && 
               (exception.getMessage().contains("database") ||
                exception.getMessage().contains("SQL") ||
                exception.getMessage().contains("connection"));
    }

    private static boolean isValidationException(Exception exception) {
        return exception instanceof IllegalArgumentException ||
               exception instanceof java.util.InputMismatchException ||
               exception.getMessage() != null && 
               (exception.getMessage().contains("validation") ||
                exception.getMessage().contains("invalid"));
    }

    private static boolean isAuthenticationException(Exception exception) {
        return exception instanceof SecurityException ||
               exception.getMessage() != null && 
               (exception.getMessage().contains("authentication") ||
                exception.getMessage().contains("login") ||
                exception.getMessage().contains("unauthorized"));
    }

    private static boolean isAuthorizationException(Exception exception) {
        return exception instanceof SecurityException ||
               exception.getMessage() != null && 
               (exception.getMessage().contains("authorization") ||
                exception.getMessage().contains("permission") ||
                exception.getMessage().contains("access denied"));
    }

    private static boolean isPaymentException(Exception exception) {
        return exception.getMessage() != null && 
               (exception.getMessage().contains("payment") ||
                exception.getMessage().contains("transaction") ||
                exception.getMessage().contains("stripe") ||
                exception.getMessage().contains("razorpay"));
    }
}
