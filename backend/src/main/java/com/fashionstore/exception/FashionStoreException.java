package com.fashionstore.exception;

/**
 * Base exception class for FashionStore application
 * Provides structured error handling with user-friendly messages
 */
public class FashionStoreException extends RuntimeException {

    private final ExceptionType type;
    private final String userMessage;
    private final String errorCode;
    private final Object data;

    public FashionStoreException(ExceptionType type, String message) {
        this(type, message, null, null, null);
    }

    public FashionStoreException(ExceptionType type, String message, String userMessage) {
        this(type, message, userMessage, null, null);
    }

    public FashionStoreException(ExceptionType type, String message, String userMessage, Throwable cause) {
        this(type, message, userMessage, null, cause);
    }

    public FashionStoreException(ExceptionType type, String message, String userMessage, Object data) {
        this(type, message, userMessage, data, null);
    }

    public FashionStoreException(ExceptionType type, String message, String userMessage, Object data, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.userMessage = userMessage;
        this.data = data;
        this.errorCode = generateErrorCode(type);
    }

    public ExceptionType getType() {
        return type;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }

    private String generateErrorCode(ExceptionType type) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        timestamp = timestamp.length() >= 6 ? timestamp.substring(timestamp.length() - 6) : timestamp;
        return switch (type) {
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
     * Create validation exception
     */
    public static FashionStoreException validation(String message, String userMessage) {
        return new FashionStoreException(ExceptionType.VALIDATION_ERROR, message, userMessage);
    }

    /**
     * Create validation exception with data
     */
    public static FashionStoreException validation(String message, String userMessage, Object data) {
        return new FashionStoreException(ExceptionType.VALIDATION_ERROR, message, userMessage, data);
    }

    /**
     * Create authentication exception
     */
    public static FashionStoreException authentication(String message, String userMessage) {
        return new FashionStoreException(ExceptionType.AUTHENTICATION_ERROR, message, userMessage);
    }

    /**
     * Create authorization exception
     */
    public static FashionStoreException authorization(String message, String userMessage) {
        return new FashionStoreException(ExceptionType.AUTHORIZATION_ERROR, message, userMessage);
    }

    /**
     * Create not found exception
     */
    public static FashionStoreException notFound(String message, String userMessage) {
        return new FashionStoreException(ExceptionType.NOT_FOUND, message, userMessage);
    }

    /**
     * Create payment exception
     */
    public static FashionStoreException payment(String message, String userMessage) {
        return new FashionStoreException(ExceptionType.PAYMENT_ERROR, message, userMessage);
    }

    /**
     * Create payment exception with data
     */
    public static FashionStoreException payment(String message, String userMessage, Object data) {
        return new FashionStoreException(ExceptionType.PAYMENT_ERROR, message, userMessage, data);
    }

    /**
     * Create database exception
     */
    public static FashionStoreException database(String message, String userMessage, Throwable cause) {
        return new FashionStoreException(ExceptionType.DATABASE_ERROR, message, userMessage, cause);
    }

    /**
     * Create system exception
     */
    public static FashionStoreException system(String message, String userMessage, Throwable cause) {
        return new FashionStoreException(ExceptionType.SYSTEM_ERROR, message, userMessage, cause);
    }
}
