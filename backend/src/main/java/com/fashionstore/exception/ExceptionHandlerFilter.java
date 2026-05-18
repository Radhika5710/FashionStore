package com.fashionstore.exception;

import com.fashionstore.util.JsonUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler filter for consistent error responses
 * Catches all exceptions and returns standardized JSON error responses
 */
@WebFilter(urlPatterns = {"/api/*", "/checkout/*", "/cart/*"})
public class ExceptionHandlerFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlerFilter.class);
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException ex) {
            throw ex;
        } catch (Exception ex) {
            if (ex instanceof ApiException) {
                handleApiException((HttpServletResponse) response, (ApiException) ex);
            } else if (ex instanceof IllegalArgumentException) {
                handleIllegalArgumentException((HttpServletResponse) response, (IllegalArgumentException) ex);
            } else if (ex instanceof IllegalStateException) {
                handleIllegalStateException((HttpServletResponse) response, (IllegalStateException) ex);
            } else {
                handleGenericException((HttpServletResponse) response, ex);
            }
        }
    }
    
    private void handleApiException(HttpServletResponse response, ApiException ex) throws IOException {
        logger.error("API Exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        response.setStatus(ex.getStatusCode());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("message", ex.getMessage());
        
        response.getWriter().write(JsonUtil.toJson(errorResponse));
    }
    
    private void handleIllegalArgumentException(HttpServletResponse response, IllegalArgumentException ex) throws IOException {
        logger.warn("Validation Error: {}", ex.getMessage());
        response.setStatus(400);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", "VALIDATION_ERROR");
        errorResponse.put("message", ex.getMessage());
        
        response.getWriter().write(JsonUtil.toJson(errorResponse));
    }
    
    private void handleIllegalStateException(HttpServletResponse response, IllegalStateException ex) throws IOException {
        logger.error("State Error: {}", ex.getMessage());
        response.setStatus(500);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", "STATE_ERROR");
        errorResponse.put("message", ex.getMessage());
        
        response.getWriter().write(JsonUtil.toJson(errorResponse));
    }
    
    private void handleGenericException(HttpServletResponse response, Exception ex) throws IOException {
        logger.error("Unexpected Exception: {}", ex.getMessage(), ex);
        response.setStatus(500);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", "INTERNAL_ERROR");
        errorResponse.put("message", "An unexpected error occurred. Please try again later.");
        
        response.getWriter().write(JsonUtil.toJson(errorResponse));
    }
}
