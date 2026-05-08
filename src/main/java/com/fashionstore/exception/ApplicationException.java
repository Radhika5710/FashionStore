package com.fashionstore.exception;

/**
 * Base application exception for FashionStore
 * Provides structured error handling with error codes and user-friendly messages
 */
public class ApplicationException extends Exception {
    
    private final ErrorCode errorCode;
    private final String userMessage;
    private final String debugMessage;
    
    public ApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = errorCode.getUserMessage();
        this.debugMessage = message;
    }
    
    public ApplicationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = errorCode.getUserMessage();
        this.debugMessage = message;
    }
    
    public ApplicationException(ErrorCode errorCode, String userMessage, String debugMessage, Throwable cause) {
        super(debugMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.debugMessage = debugMessage;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public String getDebugMessage() {
        return debugMessage;
    }
    
    /**
     * Error codes with associated HTTP status codes and user messages
     */
    public enum ErrorCode {
        // Database errors (5xx)
        DATABASE_CONNECTION_ERROR(500, "Unable to connect to database. Please try again later."),
        DATABASE_QUERY_ERROR(500, "An error occurred while processing your request."),
        DATABASE_TRANSACTION_ERROR(500, "Transaction failed. Please try again."),
        
        // Authentication errors (401/403)
        AUTHENTICATION_FAILED(401, "Invalid credentials. Please check your email and password."),
        SESSION_EXPIRED(401, "Your session has expired. Please log in again."),
        UNAUTHORIZED_ACCESS(403, "You don't have permission to access this resource."),
        INVALID_TOKEN(401, "Invalid or expired token."),
        
        // Validation errors (400)
        INVALID_INPUT(400, "Invalid input provided. Please check your data."),
        MISSING_REQUIRED_FIELD(400, "Required field is missing."),
        INVALID_EMAIL_FORMAT(400, "Invalid email format."),
        INVALID_PASSWORD_FORMAT(400, "Password does not meet requirements."),
        
        // Business logic errors (400/409)
        PRODUCT_NOT_FOUND(404, "Product not found."),
        PRODUCT_OUT_OF_STOCK(400, "Product is out of stock."),
        INSUFFICIENT_STOCK(400, "Not enough stock available."),
        CART_EMPTY(400, "Your cart is empty."),
        INVALID_ORDER_STATE(409, "Order cannot be processed in current state."),
        DUPLICATE_ENTRY(409, "This item already exists."),
        
        // Payment errors (402/400)
        PAYMENT_FAILED(402, "Payment processing failed. Please try again."),
        PAYMENT_DECLINED(402, "Payment was declined by your bank."),
        INVALID_PAYMENT_METHOD(400, "Invalid payment method selected."),
        
        // Generic errors
        INTERNAL_SERVER_ERROR(500, "An unexpected error occurred. Please try again later."),
        SERVICE_UNAVAILABLE(503, "Service temporarily unavailable. Please try again later."),
        RESOURCE_NOT_FOUND(404, "The requested resource was not found.");
        
        private final int httpStatus;
        private final String userMessage;
        
        ErrorCode(int httpStatus, String userMessage) {
            this.httpStatus = httpStatus;
            this.userMessage = userMessage;
        }
        
        public int getHttpStatus() {
            return httpStatus;
        }
        
        public String getUserMessage() {
            return userMessage;
        }
    }
}
