package com.fashionstore.security;

import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * File Upload Security Filter
 * Validates and secures all file uploads
 */
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024, // 1 MB
    maxFileSize = 10 * 1024 * 1024, // 10 MB
    maxRequestSize = 20 * 1024 * 1024 // 20 MB
)
public class FileUploadSecurityFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadSecurityFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("FileUploadSecurityFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Apply file upload security measures
            if (!validateFileUpload(httpRequest, httpResponse)) {
                return; // Security measure blocked the upload
            }

            // Continue with the request
            chain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("File upload security filter error: {}", e.getMessage(), e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validate file upload
     */
    private boolean validateFileUpload(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only validate POST requests with multipart content
        if (!"POST".equalsIgnoreCase(method)) {
            return true;
        }

        String contentType = request.getContentType();
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            return true;
        }

        try {
            // Check admin authorization for admin uploads (skip session validation for admin endpoints)
            if (path.startsWith("/admin") || path.startsWith("/api/admin")) {
                AdminAuthorizationUtil.AuthorizationResult authResult = 
                    AdminAuthorizationUtil.validateAdminAccess(request);
                
                if (!authResult.isAuthorized()) {
                    logger.warn("File upload attempted without admin authorization: {}", path);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin authorization required");
                    return false;
                }
            } else {
                // Check authentication for non-admin uploads
                if (!com.fashionstore.util.SecurityUtil.isAuthenticatedCustomer(request)) {
                    logger.warn("File upload attempted without valid customer session: {}", path);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                    return false;
                }
            }

            // Validate each uploaded file
            for (Part part : request.getParts()) {
                if (part.getName().equals("file") || part.getSubmittedFileName() != null) {
                    FileUploadValidator.ValidationResult validationResult = 
                        FileUploadValidator.validateFile(part, FileUploadValidator.UploadType.IMAGE);
                    
                    if (!validationResult.isValid()) {
                        logger.warn("File upload validation failed: {}", validationResult.getErrorMessage());
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, validationResult.getErrorMessage());
                        return false;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error validating file upload: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "File upload validation failed");
            return false;
        }

        return true;
    }

    @Override
    public void destroy() {
        logger.info("FileUploadSecurityFilter destroyed");
    }
}
