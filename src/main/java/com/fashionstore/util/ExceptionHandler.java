package com.fashionstore.util;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Centralized Exception Handler for FashionStore
 * Provides consistent error handling across all controllers
 */
public class ExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    /**
     * Handles exceptions by logging and redirecting to error page
     * 
     * @param request The HTTP request
     * @param response The HTTP response
     * @param e The exception that occurred
     * @param contextPath The application context path
     * @param userMessage Optional user-friendly message (null for default)
     */
    public static void handleException(HttpServletRequest request, HttpServletResponse response, 
                                       Exception e, String contextPath, String userMessage) {
        
        // Log the exception
        logger.error("=== EXCEPTION CAUGHT ===");
        logger.error("URI: {}", request.getRequestURI());
        logger.error("Method: {}", request.getMethod());
        logger.error("Error: {}", e.getMessage(), e);
        logger.error("========================");
        
        // Set error attributes for error page
        request.setAttribute("errorType", e.getClass().getSimpleName());
        request.setAttribute("errorMessage", userMessage != null ? userMessage : "An unexpected error occurred. Please try again.");
        request.setAttribute("requestUri", request.getRequestURI());
        
        // Redirect to error page
        try {
            RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/views/error.jsp");
            if (dispatcher != null) {
                dispatcher.forward(request, response);
            } else {
                // Fallback: send 500 error
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                  "An unexpected error occurred. Please try again.");
            }
        } catch (ServletException | IOException ex) {
            // Last resort fallback
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                  "An unexpected error occurred. Please try again.");
            } catch (IOException ioEx) {
                logger.error("Failed to send error response: {}", ioEx.getMessage());
            }
        }
    }

    /**
     * Handles exceptions with default user message
     */
    public static void handleException(HttpServletRequest request, HttpServletResponse response, 
                                       Exception e, String contextPath) {
        handleException(request, response, e, contextPath, null);
    }

    /**
     * Handles exceptions and redirects to a specific page instead of error page
     * Useful for form submissions that should redirect back to form with error
     */
    public static void handleExceptionWithRedirect(HttpServletRequest request, HttpServletResponse response,
                                                   Exception e, String contextPath, String redirectPath,
                                                   String errorMessage) {
        
        // Log the exception
        logger.error("Exception caught (Redirect) | URI: {} | Redirect: {} | Error: {}",
            request.getRequestURI(), redirectPath, e.getMessage(), e);
        
        // Set error message in session for display on redirect page
        request.getSession(true).setAttribute("error", errorMessage != null ? errorMessage : 
            "An unexpected error occurred. Please try again.");
        
        try {
            response.sendRedirect(contextPath + redirectPath);
        } catch (IOException ioEx) {
            // Fallback to standard exception handling
            handleException(request, response, ioEx, contextPath);
        }
    }
}
