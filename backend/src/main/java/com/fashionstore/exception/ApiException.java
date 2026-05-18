package com.fashionstore.exception;

/**
 * Unified API Exception for consistent error handling across backend
 * Provides standardized error responses with HTTP status codes
 */
public class ApiException extends Exception {
    
    private final int statusCode;
    private final String errorCode;
    private final String message;
    
    public ApiException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.message = message;
    }
    
    public ApiException(int statusCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.message = message;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
    
    // Common API exceptions
    public static ApiException badRequest(String message) {
        return new ApiException(400, "BAD_REQUEST", message);
    }
    
    public static ApiException unauthorized(String message) {
        return new ApiException(401, "UNAUTHORIZED", message);
    }
    
    public static ApiException forbidden(String message) {
        return new ApiException(403, "FORBIDDEN", message);
    }
    
    public static ApiException notFound(String message) {
        return new ApiException(404, "NOT_FOUND", message);
    }
    
    public static ApiException conflict(String message) {
        return new ApiException(409, "CONFLICT", message);
    }
    
    public static ApiException internalError(String message) {
        return new ApiException(500, "INTERNAL_ERROR", message);
    }
    
    public static ApiException internalError(String message, Throwable cause) {
        return new ApiException(500, "INTERNAL_ERROR", message, cause);
    }
    
    public static ApiException serviceUnavailable(String message) {
        return new ApiException(503, "SERVICE_UNAVAILABLE", message);
    }
}
