package com.fashionstore.controller.api;

import com.fashionstore.controller.ApiResponse;
import com.fashionstore.model.User;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.security.AuthContext;
import com.fashionstore.security.JWTUtil;
import com.fashionstore.service.UserService;
import com.fashionstore.util.AuditLogger;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Customer Authentication API Controller
 * Handles token refresh and user info for customer authentication
 */
@WebServlet("/api/auth/*")
public class AuthApiController extends CustomerApiBaseController {

    private static final long serialVersionUID = 1L;
    private UserService userService;

    @Override
    public void init() throws ServletException {
        super.init();
        userService = ServiceRegistry.getInstance().getUserService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        applyCors(request, response);
        String pathInfo = request.getPathInfo();
        
        try {
            if ("/refresh".equals(pathInfo)) {
                handleTokenRefresh(request, response);
            } else {
                writeApiResponse(response, 404, ApiResponse.error("Endpoint not found"));
            }
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        applyCors(request, response);
        String pathInfo = request.getPathInfo();
        
        try {
            if ("/me".equals(pathInfo)) {
                handleMe(request, response);
            } else {
                writeApiResponse(response, 404, ApiResponse.error("Endpoint not found"));
            }
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Handle token refresh
     */
    private void handleTokenRefresh(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String refreshToken = getCookieValue(request, "refresh_token");
        
        if (refreshToken == null || refreshToken.isBlank()) {
            writeApiResponse(response, 401, ApiResponse.error("No refresh token provided"));
            return;
        }
        
        // Validate refresh token
        JWTUtil.TokenValidationResult validation = JWTUtil.validateToken(refreshToken);
        
        if (!validation.isValid()) {
            writeApiResponse(response, 401, ApiResponse.error("Invalid or expired refresh token"));
            return;
        }
        
        // Check if token is a refresh token
        if (!"refresh".equals(validation.getTokenType())) {
            writeApiResponse(response, 401, ApiResponse.error("Invalid token type"));
            return;
        }
        
        try {
            String userId = validation.getUserId();
            User user = userService.getUserById(Integer.parseInt(userId));
            
            if (user == null) {
                writeApiResponse(response, 401, ApiResponse.error("User not found"));
                return;
            }
            
            // Generate new access token
            String newAccessToken = JWTUtil.generateToken(
                String.valueOf(user.getUserId()),
                user.getEmail(),
                user.getRole()
            );
            
            // Generate new refresh token
            String newRefreshToken = JWTUtil.generateRefreshToken(String.valueOf(user.getUserId()));
            
            AuditLogger.log("TOKEN_REFRESH", "Token refreshed for user: " + user.getEmail(), String.valueOf(user.getUserId()), request);
            
            // Set new access token cookie
            Cookie accessCookie = new Cookie("access_token", newAccessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(request.isSecure());
            accessCookie.setPath("/");
            accessCookie.setMaxAge(15 * 60); // 15 minutes
            response.addCookie(accessCookie);
            
            // Set new refresh token cookie
            Cookie refreshCookie = new Cookie("refresh_token", newRefreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(request.isSecure());
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            response.addCookie(refreshCookie);
            
            // Return new tokens in response
            java.util.Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("success", true);
            responseData.put("accessToken", newAccessToken);
            responseData.put("refreshToken", newRefreshToken);
            responseData.put("tokenType", "Bearer");
            responseData.put("expiresIn", 15 * 60);
            responseData.put("user", getUserData(user));
            
            writeApiResponse(response, 200, ApiResponse.success("Token refreshed successfully", responseData));
            
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Handle /me endpoint - get current user info
     */
    private void handleMe(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuthContext authContext = AuthContext.fromRequest(request);
        
        if (!authContext.isAuthenticated()) {
            writeApiResponse(response, 401, ApiResponse.error("Not authenticated"));
            return;
        }
        
        try {
            User user = userService.getUserById(Integer.parseInt(authContext.getUserId()));
            
            if (user == null) {
                writeApiResponse(response, 404, ApiResponse.error("User not found"));
                return;
            }
            
            java.util.Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("success", true);
            responseData.put("user", getUserData(user));
            
            writeApiResponse(response, 200, ApiResponse.success("User info retrieved successfully", responseData));
            
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private java.util.Map<String, Object> getUserData(User user) {
        java.util.Map<String, Object> userData = new java.util.HashMap<>();
        userData.put("userId", user.getUserId());
        userData.put("email", user.getEmail());
        userData.put("fullName", user.getFullName());
        userData.put("role", user.getRole());
        userData.put("isAdmin", user.isAdmin());
        return userData;
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        applyCors(request, response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
