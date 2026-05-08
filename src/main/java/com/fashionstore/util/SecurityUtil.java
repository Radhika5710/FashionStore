package com.fashionstore.util;

import com.fashionstore.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class SecurityUtil {

    /**
     * Check if the current user is authenticated
     */
    public static boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("user") instanceof User;
    }

    /**
     * Check if the current user is an admin
     */
    public static boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        User user = (User) session.getAttribute("user");
        return user != null && "admin".equals(user.getRole());
    }

    /**
     * Check if the current user is the owner of a resource
     */
    public static boolean isOwner(HttpServletRequest request, int resourceUserId) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        User user = (User) session.getAttribute("user");
        return user != null && user.getUserId() == resourceUserId;
    }

    /**
     * Get the current authenticated user
     */
    public static User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        return (User) session.getAttribute("user");
    }

    /**
     * Require authentication - redirect to login if not authenticated
     */
    public static boolean requireAuthentication(HttpServletRequest request, HttpServletResponse response, String contextPath) {
        if (!isAuthenticated(request)) {
            try {
                response.sendRedirect(contextPath + "/login");
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Require admin role - send 403 if not admin
     */
    public static boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) {
        if (!isAuthenticated(request)) {
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        
        if (!isAdmin(request)) {
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validate CSRF token
     */
    public static boolean validateCSRF(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String sessionToken = (String) session.getAttribute("csrf_token");
        String requestToken = request.getParameter("csrf_token");
        
        return sessionToken != null && sessionToken.equals(requestToken);
    }
}
